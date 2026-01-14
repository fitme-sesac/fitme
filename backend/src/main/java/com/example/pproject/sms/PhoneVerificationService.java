package com.example.pproject.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationRepository repo;
    private final SolapiSmsService solapiSmsService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${otp.pepper}")
    private String otpPepper;

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 5;

    public record SendResult(UUID verificationId, OffsetDateTime expiresAt) {}

    /**
     * Acquires a PostgreSQL transactional advisory lock derived from the given key.
     *
     * This method blocks until pg_advisory_xact_lock(hashtext(key)) obtains the lock;
     * the lock is held for the duration of the current database transaction and is
     * released when the transaction completes.
     *
     * @param key the string used to derive the advisory lock (passed to hashtext)
     */
    private void advisoryXactLock(String key) {
        jdbcTemplate.execute(con -> {
            PreparedStatement ps = con.prepareStatement("select pg_advisory_xact_lock(hashtext(?))");
            ps.setString(1, key);
            return ps;
        }, (PreparedStatement ps) -> {
            ps.execute();
            return null;
        });
    }

    /**
     * Generates and sends a one-time password (OTP) for the given phone and purpose, and records a verification entry.
     *
     * The method normalizes and validates the phone, acquires a per-phone-and-purpose advisory transaction lock to
     * serialize concurrent sends, enforces resend cooldown and max-resend limits, sends the SMS, stores an HMAC-hashed
     * code and metadata, and returns the verification identifier and expiry time.
     *
     * @param phoneDigits raw phone input; digits will be extracted and validated
     * @param purpose     logical purpose for the OTP (used in hashing and lookup)
     * @param ip          request origin IP to record with the verification
     * @param ua          request user agent to record with the verification
     * @return            a SendResult containing the verificationId and the OTP expiry timestamp
     * @throws IllegalStateException if the phone format is invalid, if resend cooldown or max-resends limits are exceeded,
     *                               or if an internal OTP hashing error occurs
     */
    @Transactional
    public SendResult sendOtp(String phoneDigits, String purpose, String ip, String ua) {
        String phone = normalize(phoneDigits);
        validatePhone(phone);

        // ✅ 동일 phone+purpose 동시 발송 레이스 방지
        String lockKey = phone + ":" + purpose;
        advisoryXactLock(lockKey);

        OffsetDateTime now = OffsetDateTime.now();

        PhoneVerificationEntity pv = repo
                .findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, purpose)
                .orElse(null);

        if (pv != null) {
            if (!pv.getExpiresAt().isAfter(now)) {
                pv.setConsumedAt(now);
                repo.save(pv);
                pv = null;
            } else {
                if (Duration.between(pv.getLastSentAt(), now).compareTo(RESEND_COOLDOWN) < 0) {
                    throw new IllegalStateException("잠시 후 다시 시도해주세요. (재발송 대기)");
                }
                if (pv.getResendCount() >= MAX_RESENDS) {
                    throw new IllegalStateException("재발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
                }
            }
        }

        String code = generate6Digits();

        // ✅ 먼저 발송(실패 시 DB 변경 없음)
        solapiSmsService.sendOtp(phone, code);

        String codeHash = hmacSha256Hex(phone + ":" + purpose + ":" + code);

        if (pv == null) {
            pv = new PhoneVerificationEntity();
            pv.setVerificationId(UUID.randomUUID());
            pv.setPhone(phone);
            pv.setPurpose(purpose);
            pv.setAttemptCount(0);
            pv.setResendCount(0);
            pv.setCreatedAt(now);
        } else {
            pv.setResendCount(pv.getResendCount() + 1);
            pv.setAttemptCount(0);
        }

        pv.setCodeHash(codeHash);
        pv.setLastSentAt(now);
        pv.setExpiresAt(now.plus(OTP_TTL));
        pv.setVerifiedAt(null);
        pv.setConsumedAt(null);
        pv.setRequestIp(ip);
        pv.setRequestUserAgent(ua);

        repo.save(pv);
        return new SendResult(pv.getVerificationId(), pv.getExpiresAt());
    }

    /**
     * Validates the provided 6-digit OTP for a verification record and, on success,
     * marks the verification as verified and consumed.
     *
     * @param verificationId the UUID of the verification record to check
     * @param phoneDigits raw phone digits provided by the caller (will be normalized)
     * @param purpose the verification purpose associated with the OTP
     * @param code the 6-digit OTP code submitted for verification
     * @throws IllegalStateException if the code is not a 6-digit numeric string; if the
     *         verification record does not exist or has already been consumed; if the
     *         phone or purpose does not match the record; if the OTP has expired; if
     *         the maximum allowed attempts has been reached (the record will be consumed);
     *         or if the provided code does not match the stored OTP (attempt count is incremented). 
     */
    @Transactional
    public void verifyOtp(UUID verificationId, String phoneDigits, String purpose, String code) {
        String phone = normalize(phoneDigits);
        validatePhone(phone);
        if (code == null || !code.matches("^[0-9]{6}$")) {
            throw new IllegalStateException("인증번호는 6자리 숫자입니다.");
        }

        PhoneVerificationEntity pv = repo.findById(verificationId)
                .orElseThrow(() -> new IllegalStateException("인증 절차가 만료되었습니다. 다시 발송해주세요."));

        OffsetDateTime now = OffsetDateTime.now();

        if (pv.getConsumedAt() != null) {
            throw new IllegalStateException("인증 절차가 만료되었습니다. 다시 발송해주세요.");
        }
        if (!pv.getPhone().equals(phone) || !pv.getPurpose().equals(purpose)) {
            throw new IllegalStateException("인증 정보가 일치하지 않습니다. 다시 발송해주세요.");
        }
        if (!pv.getExpiresAt().isAfter(now)) {
            pv.setConsumedAt(now);
            repo.save(pv);
            throw new IllegalStateException("인증번호가 만료되었습니다. 다시 발송해주세요.");
        }
        if (pv.getAttemptCount() >= MAX_ATTEMPTS) {
            pv.setConsumedAt(now);
            repo.save(pv);
            throw new IllegalStateException("인증 실패 횟수를 초과했습니다. 다시 발송해주세요.");
        }

        String expected = pv.getCodeHash();
        String given = hmacSha256Hex(phone + ":" + purpose + ":" + code);

        if (!constantTimeHexEquals(expected, given)) {
            pv.setAttemptCount(pv.getAttemptCount() + 1);
            if (pv.getAttemptCount() >= MAX_ATTEMPTS) {
                pv.setConsumedAt(now);
            }
            repo.save(pv);
            throw new IllegalStateException("인증번호가 일치하지 않습니다.");
        }

        pv.setVerifiedAt(now);
        pv.setConsumedAt(now);
        repo.save(pv);
    }

    /**
     * Normalize a phone-like string to its digits-only form.
     *
     * @param p the input string which may contain non-digit characters or be null
     * @return a string containing only the digits extracted from {@code p}, or an empty string if {@code p} is null or contains no digits
     */
    private String normalize(String p) {
        return String.valueOf(p == null ? "" : p).replaceAll("[^0-9]", "");
    }

    /**
     * Ensures the provided phone string consists of 10 to 15 digits.
     *
     * @param phone the phone string to validate (digits only)
     * @throws IllegalStateException if {@code phone} is blank or not composed of 10–15 digits
     */
    private void validatePhone(String phone) {
        if (phone.isBlank() || !phone.matches("^[0-9]{10,15}$")) {
            throw new IllegalStateException("휴대폰 번호 형식이 올바르지 않습니다.");
        }
    }

    /**
     * Generate a random 6-digit numeric string.
     *
     * @return a 6-digit numeric string in the range 100000 to 999999 inclusive
     */
    private String generate6Digits() {
        int n = 100000 + new java.security.SecureRandom().nextInt(900000);
        return String.valueOf(n);
    }

    /**
     * Compute the HMAC-SHA256 hex digest of the given data using the configured OTP pepper as the key.
     *
     * @param data the input string to hash
     * @return a lowercase hex-encoded HMAC-SHA256 of the input data
     * @throws IllegalStateException if the MAC cannot be computed
     */
    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(otpPepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("OTP hash error", e);
        }
    }

    /**
     * Compares two hexadecimal-encoded byte sequences in constant time to avoid timing attacks.
     *
     * @param a a hex-encoded string (lower- or upper-case) representing the first byte sequence; may be null
     * @param b a hex-encoded string (lower- or upper-case) representing the second byte sequence; may be null
     * @return `true` if both inputs are non-null, have the same length, and decode to identical bytes; `false` otherwise
     */
    private boolean constantTimeHexEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        byte[] ba = HexFormat.of().parseHex(a);
        byte[] bb = HexFormat.of().parseHex(b);
        return java.security.MessageDigest.isEqual(ba, bb);
    }
}