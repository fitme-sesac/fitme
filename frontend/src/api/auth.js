import { http } from "./http";

/**
 * 인증 API 서비스
 * - 로그인/로그아웃
 * - 회원가입
 * - 비밀번호 찾기/변경
 * - 소셜 로그인
 */

/**
 * 인증 상태 확인
 * @returns {Promise<{authenticated: boolean, email?: string, name?: string, avatarUrl?: string}>}
 */
export async function checkAuthStatus() {
  const response = await http.get("/api/auth/status");
  return response.data;
}

/**
 * 로그인
 * @param {string} loginId - 로그인 ID
 * @param {string} password - 비밀번호
 * @returns {Promise<LoginResponse>}
 */
export async function login(loginId, password) {
  const formData = new URLSearchParams();
  formData.append("userid", loginId);
  formData.append("password", password);

  const response = await http.post("/Login", formData, {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  });
  return response.data;
}

/**
 * 로그아웃
 * @returns {Promise<void>}
 */
export async function logout() {
  const response = await http.post("/Logout");
  return response.data;
}

/**
 * 회원가입
 * @param {Object} userData - 회원가입 정보
 * @param {string} userData.email - 이메일
 * @param {string} userData.loginId - 로그인 ID
 * @param {string} userData.password - 비밀번호
 * @param {string} userData.name - 이름
 * @param {string} userData.phone - 휴대폰 번호
 * @param {string} userData.role - 역할 (CANDIDATE/EMPLOYER)
 * @returns {Promise<RegisterResponse>}
 */
export async function register(userData) {
  const response = await http.post("/Register", userData);
  return response.data;
}

/**
 * 아이디 찾기 - 휴대폰 인증 요청
 * @param {string} name - 이름
 * @param {string} phone - 휴대폰 번호
 * @returns {Promise<{message: string}>}
 */
export async function findUserId(name, phone) {
  const response = await http.post("/Find_Userid", { name, phone });
  return response.data;
}

/**
 * 아이디 찾기 - 인증 코드 확인
 * @param {string} phone - 휴대폰 번호
 * @param {string} code - 인증 코드
 * @returns {Promise<{userId: string}>}
 */
export async function verifyUserIdCode(phone, code) {
  const response = await http.post("/Verify_Userid_Code", { phone, code });
  return response.data;
}

/**
 * 비밀번호 찾기 - 휴대폰 인증 요청
 * @param {string} loginId - 로그인 ID
 * @param {string} phone - 휴대폰 번호
 * @returns {Promise<{message: string}>}
 */
export async function findPassword(loginId, phone) {
  const response = await http.post("/Find_password", { loginId, phone });
  return response.data;
}

/**
 * 비밀번호 찾기 - 인증 코드 확인
 * @param {string} phone - 휴대폰 번호
 * @param {string} code - 인증 코드
 * @returns {Promise<{token: string}>}
 */
export async function verifyPasswordCode(phone, code) {
  const response = await http.post("/Verify_Code", { phone, code });
  return response.data;
}

/**
 * 새 비밀번호 설정 (비밀번호 찾기 후)
 * @param {string} token - 인증 토큰
 * @param {string} newPassword - 새 비밀번호
 * @returns {Promise<{message: string}>}
 */
export async function setNewPassword(token, newPassword) {
  const response = await http.post("/New_Password", { token, newPassword });
  return response.data;
}

/**
 * 비밀번호 변경 (로그인 상태)
 * @param {string} currentPassword - 현재 비밀번호
 * @param {string} newPassword - 새 비밀번호
 * @returns {Promise<{message: string}>}
 */
export async function changePassword(currentPassword, newPassword) {
  const response = await http.post("/Change_Password", {
    currentPassword,
    newPassword,
  });
  return response.data;
}

/**
 * 소셜 로그인 URL 가져오기
 * @param {string} provider - 소셜 로그인 제공자 (google, kakao, naver)
 * @returns {string} - 리다이렉트 URL
 */
export function getSocialLoginUrl(provider) {
  return `/oauth2/authorization/${provider}`;
}

/**
 * 휴대폰 인증 요청 (회원가입용)
 * @param {string} phone - 휴대폰 번호
 * @returns {Promise<{message: string}>}
 */
export async function requestPhoneVerification(phone) {
  const response = await http.post("/api/phone/send-code", { phone });
  return response.data;
}

/**
 * 휴대폰 인증 확인 (회원가입용)
 * @param {string} phone - 휴대폰 번호
 * @param {string} code - 인증 코드
 * @returns {Promise<{verified: boolean}>}
 */
export async function verifyPhone(phone, code) {
  const response = await http.post("/api/phone/verify", { phone, code });
  return response.data;
}
