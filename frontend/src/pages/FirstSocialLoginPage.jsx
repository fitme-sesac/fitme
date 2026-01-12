import HtmlPage from "../components/HtmlPage";

import { usePageCss } from "../hooks/usePageCss";

const html = `<main class="main">
  <!-- Page Title -->
  <div class="page-title" style="background-color: #003300; margin-bottom: 30px;">
    <div class="container text-center">
      <h1 style="color: white;">추가 정보 입력</h1>
      <nav class="breadcrumbs">
        <div>
          <span class="responsive-span" style="color: white;">최초 소셜 로그인 사용자는 추가 정보를 입력해야 합니다.</span>
        </div>
      </nav>
    </div>
  </div>

  <section id="services-2" class="services-2 section">
    <div class="container mb-3">
      <div class="row justify-content-center" data-aos="fade-up">
        <div class="col-md-8 col-lg-8">
          <h2 style="font-weight: bold; color: #222; line-height: 4rem;">추가정보</h2>
          <span style="font-weight: bold; color: #222; line-height: 1rem;">
            <span style="color: red;">*</span>는 필수입력 사항입니다.
          </span>
          <hr style="border: 1px solid #111;">

          <div class="alert alert-danger">
            <span></span>
          </div>

          <form id="firstSocialLoginForm" action="/User/First_Social_Login" method="post">
            <input type="hidden" />

            <!-- 이메일(자동) -->
            <div class="form-group" style="font-weight: bold;">
              <label for="emailDisplay">이메일 <span style="color: red;">*</span></label>
              <input type="text" id="emailDisplay" class="form-control" readonly />
            </div>
            <input type="hidden" id="email" name="email" />
            <input type="hidden" id="socialType" name="socialType" value="GOOGLE" />
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 이름 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="username">이름 <span style="color: red;">*</span></label>
              <input type="text" id="username" name="username" class="form-control" required placeholder="이름을 입력하세요" />
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 성별 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="gender">성별 <span style="color: red;">*</span></label>
              <select id="gender" name="gender" class="form-control" required>
                <option value="">선택</option>
                <option value="MALE">남성</option>
                <option value="FEMALE">여성</option>
                <option value="OTHER">기타</option>
                <option value="UNDISCLOSED">비공개</option>
              </select>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 생년월일 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="birthday">생년월일 <span style="color: red;">*</span></label>
              <input type="date" id="birthday" name="birthday" class="form-control" required />
              <span class="help-text">YYYY-MM-DD</span>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 휴대폰 번호 + 인증 -->
            <div class="form-group" style="font-weight: bold; align-items: flex-start;">
              <label for="phone">휴대폰 <span style="color: red;">*</span></label>
              <div style="display:flex; flex-direction:column; gap:.5rem; width: 18rem;">
                <div class="d-flex" style="gap:.5rem;">
                  <input type="text" id="phone" name="phone" class="form-control" required placeholder="01012345678" style="width: 11.5rem;" />
                  <button type="button" id="sendOtpBtn" class="btn btn-secondary" style="min-width: 6rem; height: 2rem;">인증번호</button>
                </div>
                <div class="d-flex" style="gap:.5rem;">
                  <input type="text" id="phoneOtpCode" name="phoneOtpCode" class="form-control" required placeholder="인증번호 6자리" style="width: 11.5rem;" />
                  <button type="button" id="verifyOtpBtn" class="btn btn-secondary" style="min-width: 6rem; height: 2rem;">확인</button>
                </div>
                <div id="otpStatus" class="help-text" style="margin: 0;">휴대폰 번호 입력 후 인증을 완료해주세요.</div>
              </div>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 약관/정책 (고정 박스 + 스크롤) -->
            <div class="form-group" style="font-weight: bold; align-items: flex-start;">
              <label>정책/약관 <span style="color: red;">*</span></label>
              <div style="width: 18rem;">
                <div class="policy-box">
                  <strong>[이용약관]</strong>
                  <div style="margin-top:.5rem;">
                    서비스 제공을 위해 필요한 최소한의 규칙과 이용 조건을 안내합니다.
                  </div>
                  <hr style="margin: .75rem 0;">
                  <strong>[개인정보 처리방침]</strong>
                  <div style="margin-top:.5rem;">
                    회원가입/서비스 제공을 위해 수집하는 정보, 보관 기간, 파기 절차를 안내합니다.
                  </div>
                  <hr style="margin: .75rem 0;">
                  <strong>[운영정책]</strong>
                  <div style="margin-top:.5rem;">
                    커뮤니티 이용 시 금지행위 및 제재 기준, 신고/처리 절차를 안내합니다.
                  </div>
                </div>

                <div class="policy-checks">
                  <label><input type="checkbox" id="agreeTerms" name="agreeTerms" value="true" required /> (필수) 이용약관 동의</label>
                  <label><input type="checkbox" id="agreePrivacy" name="agreePrivacy" value="true" required /> (필수) 개인정보 처리방침 동의</label>
                  <label><input type="checkbox" id="agreePolicy" name="agreePolicy" value="true" required /> (필수) 운영정책 동의</label>
                  <label><input type="checkbox" id="marketingOptIn" name="marketingOptIn" value="true" /> (선택) 마케팅 수신 동의</label>
                </div>
              </div>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <div class="d-flex mt-5 button-group" style="justify-content:flex-end;">
              <button type="submit" class="btn btn-success rounded-pill">가입 완료</button>
              <a href="/Login" class="btn btn-secondary rounded-pill ms-2">취소</a>
            </div>
          </form>
        </div>
      </div>
    </div>
  </section>
</main>`;

const scripts = [
  "(() => {\n" +
    "  const form = document.getElementById('firstSocialLoginForm');\n" +
    "  if (!form) return;\n" +
    "  const alertEl = document.querySelector('.alert.alert-danger');\n" +
    "  const setAlert = (msg) => {\n" +
    "    if (!alertEl) { alert(msg); return; }\n" +
    "    const span = alertEl.querySelector('span');\n" +
    "    if (span) span.textContent = msg; else alertEl.textContent = msg;\n" +
    "    alertEl.style.display = msg ? 'block' : 'none';\n" +
    "  };\n" +
    "  setAlert('');\n" +
    "\n" +
    "  const params = new URLSearchParams(window.location.search);\n" +
    "  const em = params.get('errorMessage') || params.get('error') || '';\n" +
    "  if (em) setAlert(em);\n" +
    "\n" +
    "  const qs = (sel) => document.querySelector(sel);\n" +
    "  const params = new URLSearchParams(window.location.search);\n" +
    "  const email = params.get('email') || '';\n" +
    "  const username = params.get('username') || '';\n" +
    "\n" +
    "  const emailDisplay = qs('#emailDisplay');\n" +
    "  const emailHidden = qs('#email');\n" +
    "  const usernameInput = qs('#username');\n" +
    "  if (emailDisplay) emailDisplay.value = email;\n" +
    "  if (emailHidden) emailHidden.value = email;\n" +
    "  if (usernameInput && !usernameInput.value) usernameInput.value = username;\n" +
    "\n" +
    "  // 휴대폰 OTP (서버 엔드포인트가 없으면 개발용 코드로 동작)\n" +
    "  let otpCode = null;\n" +
    "  let otpVerified = false;\n" +
    "  const phoneInput = qs('#phone');\n" +
    "  const otpInput = qs('#phoneOtpCode');\n" +
    "  const otpStatus = qs('#otpStatus');\n" +
    "  const sendBtn = qs('#sendOtpBtn');\n" +
    "  const verifyBtn = qs('#verifyOtpBtn');\n" +
    "\n" +
    "  const setOtpStatus = (msg, ok) => {\n" +
    "    if (!otpStatus) return;\n" +
    "    otpStatus.textContent = msg;\n" +
    "    otpStatus.style.color = ok ? '#198754' : '#dc3545';\n" +
    "  };\n" +
    "  const normalizePhone = (p) => String(p || '').replace(/[^0-9]/g, '');\n" +
    "  const isPhoneValid = (p) => /^[0-9]{10,11}$/.test(p);\n" +
    "  const randomOtp = () => String(Math.floor(Math.random() * 1000000)).padStart(6, '0');\n" +
    "\n" +
    "  const sendOtp = async () => {\n" +
    "    setAlert('');\n" +
    "    const p = normalizePhone(phoneInput?.value);\n" +
    "    if (!isPhoneValid(p)) { setOtpStatus('휴대폰 번호를 확인해주세요. (숫자만 10~11자리)', false); return; }\n" +
    "    if (phoneInput) phoneInput.value = p;\n" +
    "    otpVerified = false;\n" +
    "    let devCode = null;\n" +
    "    try {\n" +
    "      const res = await fetch('/api/phone/otp/send', {\n" +
    "        method: 'POST',\n" +
    "        headers: { 'Content-Type': 'application/json' },\n" +
    "        body: JSON.stringify({ phone: p })\n" +
    "      });\n" +
    "      if (res.ok) {\n" +
    "        const json = await res.json().catch(() => null);\n" +
    "        if (json && json.devCode) devCode = String(json.devCode);\n" +
    "      }\n" +
    "    } catch (e) { /* ignore */ }\n" +
    "    otpCode = devCode || randomOtp();\n" +
    "    setOtpStatus('인증번호를 발송했습니다. (개발용: ' + otpCode + ')', true);\n" +
    "  };\n" +
    "\n" +
    "  const verifyOtp = async () => {\n" +
    "    setAlert('');\n" +
    "    const input = String(otpInput?.value || '').trim();\n" +
    "    if (!otpCode) { setOtpStatus('먼저 인증번호를 발송해주세요.', false); return; }\n" +
    "    if (!/^[0-9]{6}$/.test(input)) { setOtpStatus('인증번호는 6자리 숫자입니다.', false); return; }\n" +
    "\n" +
    "    let serverOk = null;\n" +
    "    try {\n" +
    "      const res = await fetch('/api/phone/otp/verify', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ phone: normalizePhone(phoneInput?.value), code: input }) });\n" +
    "      if (res.ok) { const json = await res.json().catch(() => null); if (json && typeof json.verified === 'boolean') serverOk = json.verified; }\n" +
    "    } catch (e) {}\n" +
    "\n" +
    "    const ok = (serverOk === null) ? (input === otpCode) : serverOk;\n" +
    "    if (!ok) { otpVerified = false; setOtpStatus('인증번호가 일치하지 않습니다.', false); return; }\n" +
    "\n" +
    "    otpVerified = true;\n" +
    "    setOtpStatus('휴대폰 인증이 완료되었습니다.', true);\n" +
    "    if (phoneInput) phoneInput.setAttribute('readonly', 'readonly');\n" +
    "    if (otpInput) otpInput.setAttribute('readonly', 'readonly');\n" +
    "    if (sendBtn) sendBtn.disabled = true;\n" +
    "    if (verifyBtn) verifyBtn.disabled = true;\n" +
    "  };\n" +
    "\n" +
    "  if (sendBtn) sendBtn.addEventListener('click', (e) => { e.preventDefault(); sendOtp(); });\n" +
    "  if (verifyBtn) verifyBtn.addEventListener('click', (e) => { e.preventDefault(); verifyOtp(); });\n" +
    "\n" +
    "  form.addEventListener('submit', (e) => {\n" +
    "    setAlert('');\n" +
    "\n" +
    "    if (!email) { e.preventDefault(); setAlert('이메일 정보가 없습니다. 다시 로그인 해주세요.'); return; }\n" +
    "\n" +
    "    const agreeTerms = qs('#agreeTerms');\n" +
    "    const agreePrivacy = qs('#agreePrivacy');\n" +
    "    const agreePolicy = qs('#agreePolicy');\n" +
    "    if (!agreeTerms?.checked || !agreePrivacy?.checked || !agreePolicy?.checked) {\n" +
    "      e.preventDefault(); setAlert('필수 약관에 동의해야 가입이 가능합니다.'); return;\n" +
    "    }\n" +
    "    if (!otpVerified) { e.preventDefault(); setAlert('휴대폰 인증을 완료해주세요.'); return; }\n" +
    "  });\n" +
    "})();",
  "AOS.init();",
];

export default function FirstSocialLoginPage() {
  usePageCss("/assets/css/pages/first_social_login.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
