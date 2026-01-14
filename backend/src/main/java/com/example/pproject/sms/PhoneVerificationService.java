package com.example.pproject.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationRepository repo;
    private final SolapiSmsService solapiSmsService;

    @Value("${otp.pepper}")
    private String otpPepper;

    // 정책값(원하면 yml로 뺄 것)
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 5;

    public record SendResult(UUID verificationId, OffsetDateTime expiresAt) {}

    public SendResult sendOtp(String phoneDigits, String purpose, String ip, String ua) {
        String phone = normalize(phoneDigits);
        validatePhone(phone);

        OffsetDateTime now = OffsetDateTime.now();

        PhoneVerificationEntity pv = repo
                .findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, purpose)
                .orElse(null);

        if (pv != null) {
            // 만료된 활성 레코드면 소비 처리 후 새로 발급
            if (!pv.getExpiresAt().isAfter(now)) {
                pv.setConsumedAt(now);
                repo.save(pv);
                pv = null;
            } else {
                // 재발송 제한
                if (Duration.between(pv.getLastSentAt(), now).compareTo(RESEND_COOLDOWN) < 0) {
                    throw new IllegalStateException("잠시 후 다시 시도해주세요. (재발송 대기)");
                }
                if (pv.getResendCount() >= MAX_RESENDS) {
                    throw new IllegalStateException("재발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
                }
            }
        }

        String code = generate6Digits();
        // 먼저 발송(실패하면 DB 상태 안 바뀌게)
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
            pv.setAttemptCount(0); // 재발송 시 시도횟수 리셋(운영정책에 따라 유지해도 됨)
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
        pv.setConsumedAt(now); // 성공 즉시 소비 처리(재사용 방지)
        repo.save(pv);
    }

    private String normalize(String p) {
        return String.valueOf(p == null ? "" : p).replaceAll("[^0-9]", "");
    }

    private void validatePhone(String phone) {
        // DDL: 10~15 자리
        if (phone.isBlank() || !phone.matches("^[0-9]{10,15}$")) {
            throw new IllegalStateException("휴대폰 번호 형식이 올바르지 않습니다.");
        }
    }

    private String generate6Digits() {
        int n = 100000 + new java.security.SecureRandom().nextInt(900000);
        return String.valueOf(n);
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(otpPepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out); // 64 hex chars
        } catch (Exception e) {
            throw new IllegalStateException("OTP hash error", e);
        }
    }

    private boolean constantTimeHexEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        byte[] ba = HexFormat.of().parseHex(a);
        byte[] bb = HexFormat.of().parseHex(b);
        return java.security.MessageDigest.isEqual(ba, bb);
    }
}
