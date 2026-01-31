import http from "./http";

/**
 * 로그인 API
 */
export async function login({ userid, password }) {
    const payload = { userid, password };
    // 백엔드는 JSON으로 받는 로그인 API(`/User/Login` 등)를 사용할 수도 있고,
    // REST `/api/auth/login`을 쓸 수도 있습니다.
    // 현재 프로젝트에서는 백엔드에 맞춰 /api/auth/login을 사용합니다.
    return http.post("/api/auth/login", payload);
}

/**
 * 로그아웃 API
 */
export async function logout() {
    return http.post("/api/auth/logout");
}

/**
 * 로그인 상태 확인
 */
export async function getAuthStatus() {
    return http.get("/api/auth/status");
}

/**
 * 일반 회원가입 (백엔드 UserController.registerProc 호환)
 *
 * ✅ 포인트:
 * - 백엔드는 /User/Register (Spring MVC)에서
 *   - @ModelAttribute(UserRequestDTO)
 *   - + @RequestParam("emailId/emailDomain/emailTLD")
 *   를 같이 받습니다.
 * - 실패 시 redirect: {frontBaseUrl}/Register?errorMessage=... 로 보내는 구조라
 *   SPA에서는 responseURL을 파싱해서 에러 메시지를 표시합니다.
 */
export async function register(userData) {
    // ---------- helpers ----------
    const splitEmailForLegacy = (email) => {
        if (!email || typeof email !== "string") return { emailId: "", emailDomain: "", emailTLD: "" };
        const [idPart, domainPart] = email.split("@");
        if (!idPart || !domainPart) return { emailId: "", emailDomain: "", emailTLD: "" };

        const parts = domainPart.split(".");
        if (parts.length < 2) return { emailId: idPart, emailDomain: domainPart, emailTLD: "" };

        return {
            emailId: idPart,
            emailDomain: parts[0],
            emailTLD: parts.slice(1).join("."), // co.kr 등 처리
        };
    };

    const normalizeBirthday = (birthday) => {
        if (!birthday) return "";
        // 이미 YYYY-MM-DD면 그대로
        if (/^\d{4}-\d{2}-\d{2}$/.test(birthday)) return birthday;
        // YYYYMMDD → YYYY-MM-DD
        if (/^\d{8}$/.test(birthday)) {
            const yyyy = birthday.slice(0, 4);
            const mm = birthday.slice(4, 6);
            const dd = birthday.slice(6, 8);
            return `${yyyy}-${mm}-${dd}`;
        }
        return birthday;
    };

    const decodeErrorMessage = (raw) => {
        if (!raw) return "";
        try {
            return decodeURIComponent(raw.replace(/\+/g, "%20"));
        } catch {
            return raw;
        }
    };

    // ---------- build payload ----------
    const formData = new FormData();

    // DTO 필드 (UserRequestDTO 기준)
    formData.append("userid", userData.userid ?? "");
    formData.append("password", userData.password ?? "");
    formData.append("passwordConfirm", userData.passwordConfirm ?? userData.confirmPassword ?? "");
    formData.append("username", userData.username ?? userData.name ?? "");
    formData.append("phone", userData.phone ?? "");
    formData.append("gender", userData.gender ?? "");
    formData.append("birthday", normalizeBirthday(userData.birthday ?? ""));
    formData.append("agreeTerms", String(Boolean(userData.agreeTerms)));
    formData.append("agreePrivacy", String(Boolean(userData.agreePrivacy)));
    formData.append("agreePolicy", String(Boolean(userData.agreePolicy)));
    formData.append("marketingOptIn", String(Boolean(userData.marketingOptIn)));

    // Controller에서 별도로 받는 email 파트들(@RequestParam)
    const email = userData.email ?? "";
    const { emailId, emailDomain, emailTLD } = splitEmailForLegacy(email);
    formData.append("emailId", emailId);
    formData.append("emailDomain", emailDomain);
    formData.append("emailTLD", emailTLD);

    // ---------- request ----------
    // ⚠️ FormData 보낼 때 Content-Type을 직접 지정하지 마세요(바운더리 깨질 수 있음).
    const res = await http.post("/User/Register", formData, {
        validateStatus: () => true, // redirect/에러 상태를 우리가 해석
    });

    // axios(XHR)가 redirect를 따라간 최종 URL(브라우저 환경에서 제공)
    const finalUrl = res?.request?.responseURL || "";

    // 1) 백엔드가 실패 시: /Register?errorMessage=... 로 redirect
    if (finalUrl.includes("/Register") && finalUrl.includes("errorMessage=")) {
        const u = new URL(finalUrl, window.location.origin);
        const raw = u.searchParams.get("errorMessage");
        const msg = decodeErrorMessage(raw) || "회원가입에 실패했습니다.";
        throw new Error(msg);
    }

    // 2) 혹시 /Register로 갔는데 errorMessage가 없는 케이스
    if (finalUrl.includes("/Register") && !finalUrl.includes("errorMessage=")) {
        throw new Error("회원가입에 실패했습니다. 입력값을 확인해주세요.");
    }

    // 3) 백엔드가 성공 시: /Login 으로 redirect
    if (finalUrl.includes("/Login")) {
        return { ok: true };
    }

    // 4) 서버 예외(500 등)
    if (res.status >= 500) {
        throw new Error("서버 오류로 회원가입에 실패했습니다. (백엔드 로그 확인 필요)");
    }

    // 5) 기타 4xx
    if (res.status >= 400) {
        throw new Error("회원가입에 실패했습니다. 입력값을 확인해주세요.");
    }

    // 예상 밖이지만 성공으로 간주
    return { ok: true };
}

/**
 * 휴대폰 인증번호 발송
 */
export async function sendPhoneOtp(phone, purpose = "SIGNUP") {
    return http.post("/api/phone/otp/send", { phone, purpose });
}

/**
 * 휴대폰 인증번호 검증
 */
export async function verifyPhoneOtp(phone, code, purpose = "SIGNUP") {
    return http.post("/api/phone/otp/verify", { phone, code, purpose });
}

/**
 * 아이디 찾기 - 인증번호 발송
 */
export async function sendFindIdOtp(name, phone) {
    return http.post("/User/Find_Userid", { name, phone });
}

/**
 * 아이디 찾기 - 인증번호 확인
 */
export async function verifyFindIdOtp(name, phone, code) {
    return http.post("/User/Verify_Userid_Code", { name, phone, code });
}

/**
 * 비밀번호 찾기 - 인증번호 발송
 */
export async function sendFindPwOtp(userid, phone) {
    return http.post("/User/Find_Password", { userid, phone });
}

/**
 * 비밀번호 찾기 - 인증번호 검증 및 링크 발급
 */
export async function verifyFindPwOtp(userid, phone, code) {
    return http.post("/User/Verify_Code", { userid, phone, code });
}
