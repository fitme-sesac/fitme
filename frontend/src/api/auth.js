import { http } from "./http";

/**
 * 공통: axios가 (성공/실패 모두) 백엔드가 프론트로 redirect한 최종 URL을 따라간 뒤,
 * response.request.responseURL 등에 최종 주소가 남는 점을 이용해 에러 메시지를 추출한다.
 */
function parseQueryFromUrl(urlString) {
  if (!urlString) return new URLSearchParams();
  try {
    const u = new URL(urlString, window.location.origin);
    return u.searchParams;
  } catch {
    return new URLSearchParams();
  }
}

function extractRedirectErrorMessage(response) {
  const finalUrl = response?.request?.responseURL || response?.config?.url || "";
  const params = parseQueryFromUrl(finalUrl);

  // backend에서 Login/Register 등으로 redirect하면서 사용하는 키들
  const msg = params.get("errorMessage") || params.get("error") || params.get("message");
  return msg ? decodeURIComponent(msg) : null;
}

function throwIfRedirectError(response) {
  const msg = extractRedirectErrorMessage(response);
  // message는 성공에서도 사용할 수 있으므로, errorMessage/error만 우선 처리
  const finalUrl = response?.request?.responseURL || "";
  const params = parseQueryFromUrl(finalUrl);
  const err = params.get("errorMessage") || params.get("error");
  if (err) {
    throw new Error(decodeURIComponent(err));
  }
  return msg ? decodeURIComponent(msg) : null;
}

// ========================
// Auth (form login 기반)
// ========================

export async function login(userid, password) {
  const body = new URLSearchParams();
  body.set("userid", userid ?? "");
  body.set("password", password ?? "");

  const res = await http.post("/Login", body, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    // Spring Security 성공/실패 모두 redirect를 사용하므로,
    // 최종 redirect URL의 query를 파싱해 에러를 판단한다.
    maxRedirects: 5,
  });

  throwIfRedirectError(res);
  return { ok: true };
}

export async function logout() {
  // Spring Security logoutUrl
  const res = await http.post("/Logout", null, { maxRedirects: 5 });
  // logout은 보통 성공 redirect만 있으므로 에러가 없다면 ok
  return { ok: true, message: throwIfRedirectError(res) };
}

export async function checkAuthStatus() {
  return http.get("/api/auth/status").then((r) => r.data);
}

// ========================
// 회원가입
// ========================

function splitEmailForBackend(email) {
  const s = String(email || "").trim();
  const at = s.indexOf("@");
  if (at <= 0) return { emailId: "", emailDomain: "", emailTLD: "" };
  const emailId = s.slice(0, at);
  const rest = s.slice(at + 1);
  const parts = rest.split(".").filter(Boolean);
  if (parts.length < 2) return { emailId, emailDomain: rest, emailTLD: "" };

  const emailTLD = parts[parts.length - 1];
  const emailDomain = parts.slice(0, -1).join(".");
  return { emailId, emailDomain, emailTLD };
}

export async function register(userData) {
  // backend(UserController.registerProc)는 form body + emailId/emailDomain/emailTLD 를 @RequestParam으로 받는다.
  const body = new URLSearchParams();

  const emailParts =
    userData?.emailId && userData?.emailDomain && userData?.emailTLD
      ? {
        emailId: String(userData.emailId),
        emailDomain: String(userData.emailDomain),
        emailTLD: String(userData.emailTLD),
      }
      : splitEmailForBackend(userData?.email);

  // 필드명은 backend UserRequestDTO/컨트롤러 바인딩에 최대한 맞춤
  body.set("userid", userData?.userid ?? "");
  body.set("password", userData?.password ?? "");
  body.set("passwordConfirm", userData?.passwordConfirm ?? userData?.passwordConfirm ?? userData?.confirmPassword ?? "");
  body.set("username", userData?.username ?? "");
  body.set("phone", userData?.phone ?? "");
  body.set("gender", userData?.gender ?? "");
  body.set("birthday", userData?.birthday ?? "");

  body.set("agreeTerms", String(Boolean(userData?.agreeTerms)));
  body.set("agreePrivacy", String(Boolean(userData?.agreePrivacy)));
  body.set("agreePolicy", String(Boolean(userData?.agreePolicy)));
  body.set("marketingOptIn", String(Boolean(userData?.marketingOptIn)));

  body.set("emailId", emailParts.emailId ?? "");
  body.set("emailDomain", emailParts.emailDomain ?? "");
  body.set("emailTLD", emailParts.emailTLD ?? "");

  const res = await http.post("/User/Register", body, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    maxRedirects: 5,
  });

  // 실패 시 /Register?errorMessage=... 로 redirect됨
  throwIfRedirectError(res);

  // 성공 시 /Login 으로 redirect됨
  return { ok: true, message: extractRedirectErrorMessage(res) };
}

// ========================
// 휴대폰 OTP (회원가입)
// ========================

export async function requestPhoneVerification(phone) {
  return http.post("/api/phone/otp/send", { phone }).then((r) => r.data);
}

export async function verifyPhone(phone, code) {
  return http.post("/api/phone/otp/verify", { phone, code }).then((r) => r.data);
}

// ========================
// 아이디 찾기 (이름 + 휴대폰)
// ========================

export async function findUserId(name, phone) {
  try {
    const res = await http.post("/api/user/find-id/send", { name, phone }).then((r) => r.data);
    if (!res?.ok) {
      throw new Error(res?.message || "이름과 휴대폰 번호가 일치하는 회원이 없습니다.");
    }
    return res; // { ok:true, expiresIn:number }
  } catch (e) {
    const msg =
      e?.response?.data?.message ||
      e?.response?.data?.errorMessage ||
      e?.message ||
      "이름과 휴대폰 번호가 일치하는 회원이 없습니다.";
    throw new Error(msg);
  }
}

export async function verifyUserIdCode(phone, code) {
  const res = await http.post("/api/user/find-id/verify", { phone, code }).then((r) => r.data);
  if (!res?.ok) {
    throw new Error(res?.message || "인증번호가 일치하지 않습니다.");
  }
  return res; // { ok:true, userId }
}

export async function getUserIdResult() {
  const res = await http.get("/api/user/find-id/result").then((r) => r.data);
  if (!res?.ok) {
    throw new Error(res?.message || "조회할 아이디 정보가 없습니다.");
  }
  return res; // { ok:true, userId }
}

// ========================
// 비밀번호 찾기 (아이디 + 휴대폰 OTP + 토큰으로 재설정)
// - 프론트 FindPasswordPage.jsx 의 기대 형태에 맞춤
// ========================

export async function findPassword(loginId, phone) {
  try {
    const res = await http.post("/api/user/find-password/send", { loginId, phone }).then((r) => r.data);
    if (!res?.ok) {
      throw new Error(res?.message || "일치하는 회원 정보가 없습니다.");
    }
    return res;
  } catch (e) {
    const msg =
      e?.response?.data?.message ||
      e?.response?.data?.errorMessage ||
      e?.message ||
      "일치하는 회원 정보가 없습니다.";
    throw new Error(msg);
  }
}

export async function verifyPasswordCode(phone, code) {
  const res = await http.post("/api/user/find-password/verify", { phone, code }).then((r) => r.data);
  if (!res?.ok) {
    throw new Error(res?.message || "인증번호가 일치하지 않습니다.");
  }
  return res; // { ok:true, token }
}

export async function setNewPassword(token, newPassword) {
  const res = await http.post("/api/user/find-password/reset", { token, newPassword }).then((r) => r.data);
  if (!res?.ok) {
    throw new Error(res?.message || "비밀번호 변경에 실패했습니다.");
  }
  return res;
}

// ========================
// 비밀번호 변경(로그인 상태)
// ========================

export async function changePassword(currentPassword, newPassword, confirmPassword) {
  const body = new URLSearchParams();
  body.set("currentPassword", currentPassword ?? "");
  body.set("newPassword", newPassword ?? "");
  body.set("confirmPassword", confirmPassword ?? "");

  const res = await http.post("/User/Change_Password", body, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    maxRedirects: 5,
  });

  throwIfRedirectError(res);
  return { ok: true, message: extractRedirectErrorMessage(res) };
}

// ========================
// OAuth2
// ========================

// ========================
// 프로필
// ========================

export async function updateProfile(formData) {
  // formData contains name, handle, profileImage (file)
  // assuming backend endpoint /api/user/profile handles multipart/form-data
  const res = await http.post("/api/user/profile", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  }).then((r) => r.data);

  if (!res?.ok) {
    // If the backend returns a structured error
    throw new Error(res?.message || "프로필 업데이트에 실패했습니다.");
  }
  return res;
}

export function getOAuth2AuthorizationUrl(provider) {
  // backend security config: /oauth2/authorization/{registrationId}
  // Vite proxy를 쓰는 경우 같은 origin에서 접근 가능
  return `/oauth2/authorization/${provider}`;
}

// Backward-compatible aliases (코드 일부가 구버전 함수명을 쓰는 경우 대비)
export const getAuthStatus = checkAuthStatus;
export const sendPhoneOtp = requestPhoneVerification;
export const verifyPhoneOtp = verifyPhone;
