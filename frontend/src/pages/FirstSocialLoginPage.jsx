// FirstSocialLoginPage.jsx
// ✅ SOLAPI(백엔드 쿠키 기반) OTP 연동 버전: devCode/로컬 OTP 제거
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

            <!-- ✅ 이메일 입력 UI 제거 (소셜 이메일 자동 세팅) -->
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
              </select>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 생년월일 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="birthday">생년월일 <span style="color: red;">*</span></label>
              <div style="display:flex; flex-direction:column;">
                <input type="date" id="birthday" name="birthday" class="form-control" required />
                <span class="help-text">YYYY-MM-DD</span>
              </div>
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

            <!-- 약관/정책 -->
            <div class="form-group" style="font-weight: bold; align-items: flex-start;">
              <label>정책/약관 <span style="color: red;">*</span></label>

              <div class="policy-container">
                <div class="policy-sections">
                  <div class="policy-section">
                    <div class="policy-title">[이용약관]</div>
                    <div class="policy-box">
                      서비스 제공을 위해 필요한 최소한의 규칙과 이용 조건을 안내합니다.
                    </div>
                    <div class="policy-agree">
                      <label><input type="checkbox" id="agreeTerms" name="agreeTerms" value="true" required /> (필수) 이용약관 동의</label>
                    </div>
                  </div>

                  <div class="policy-section">
                    <div class="policy-title">[개인정보 처리방침]</div>
                    <div class="policy-box">
                      회원가입/서비스 제공을 위해 수집하는 정보, 보관 기간, 파기 절차를 안내합니다.
                    </div>
                    <div class="policy-agree">
                      <label><input type="checkbox" id="agreePrivacy" name="agreePrivacy" value="true" required /> (필수) 개인정보 처리방침 동의</label>
                    </div>
                  </div>

                  <div class="policy-section">
                    <div class="policy-title">[운영정책]</div>
                    <div class="policy-box">
                      커뮤니티 이용 시 금지행위 및 제재 기준, 신고/처리 절차를 안내합니다.
                    </div>
                    <div class="policy-agree">
                      <label><input type="checkbox" id="agreePolicy" name="agreePolicy" value="true" required /> (필수) 운영정책 동의</label>
                    </div>
                  </div>
                </div>

                <div class="policy-marketing">
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
    "\n" +
    "  // ✅ 중복 초기화 방지\n" +
    "  if (form.dataset.fitmeFirstSocialInit === '1') return;\n" +
    "  form.dataset.fitmeFirstSocialInit = '1';\n" +
    "\n" +
    "  const alertEl = document.querySelector('.alert.alert-danger');\n" +
    "  const setAlert = (msg) => {\n" +
    "    if (!alertEl) { if (msg) alert(msg); return; }\n" +
    "    const span = alertEl.querySelector('span');\n" +
    "    if (span) span.textContent = msg; else alertEl.textContent = msg;\n" +
    "    alertEl.style.display = msg ? 'block' : 'none';\n" +
    "  };\n" +
    "  setAlert('');\n" +
    "\n" +
    "  const qs = (sel) => document.querySelector(sel);\n" +
    "  const urlParams = new URLSearchParams(window.location.search);\n" +
    "  const em = urlParams.get('errorMessage') || urlParams.get('error') || '';\n" +
    "  if (em) setAlert(em);\n" +
    "\n" +
    "  // ✅ 소셜에서 넘어온 email/username 자동 세팅\n" +
    "  const email = urlParams.get('email') || '';\n" +
    "  const username = urlParams.get('username') || '';\n" +
    "  const emailHidden = qs('#email');\n" +
    "  const usernameInput = qs('#username');\n" +
    "  if (emailHidden) emailHidden.value = email;\n" +
    "  if (usernameInput && !usernameInput.value) usernameInput.value = username;\n" +
    "\n" +
    "  // ===== 휴대폰 OTP (SOLAPI + 쿠키 기반) =====\n" +
    "  let otpVerified = false;\n" +
    "  let sending = false;\n" +
    "  let verifying = false;\n" +
    "\n" +
    "  const phoneInput = qs('#phone');\n" +
    "  const otpInput = qs('#phoneOtpCode');\n" +
    "  const otpStatus = qs('#otpStatus');\n" +
    "  const sendBtn = qs('#sendOtpBtn');\n" +
    "  const verifyBtn = qs('#verifyOtpBtn');\n" +
    "\n" +
    "  const normalizePhone = (p) => String(p || '').replace(/[^0-9]/g, '');\n" +
    "  const isPhoneValid = (p) => /^[0-9]{10,11}$/.test(p);\n" +
    "\n" +
    "  const setOtpStatus = (msg, ok) => {\n" +
    "    if (!otpStatus) return;\n" +
    "    otpStatus.textContent = msg;\n" +
    "    otpStatus.style.color = ok ? '#198754' : '#dc3545';\n" +
    "  };\n" +
    "\n" +
    "  const lockOtpInputs = () => {\n" +
    "    if (phoneInput) phoneInput.setAttribute('readonly', 'readonly');\n" +
    "    if (otpInput) otpInput.setAttribute('readonly', 'readonly');\n" +
    "    if (sendBtn) sendBtn.disabled = true;\n" +
    "    if (verifyBtn) verifyBtn.disabled = true;\n" +
    "  };\n" +
    "\n" +
    "  // ✅ 새로고침 대비: 서버 쿠키(PHONE_VERIFIED_TMP)로 인증 상태 동기화\n" +
    "  const syncOtpStatus = async () => {\n" +
    "    try {\n" +
    "      const res = await fetch('/api/phone/otp/status', { credentials: 'include' });\n" +
    "      if (!res.ok) return;\n" +
    "      const json = await res.json().catch(() => null);\n" +
    "      if (json && json.verified === true) {\n" +
    "        otpVerified = true;\n" +
    "        setOtpStatus('휴대폰 인증이 완료되었습니다.', true);\n" +
    "        lockOtpInputs();\n" +
    "      }\n" +
    "    } catch (e) {}\n" +
    "  };\n" +
    "  syncOtpStatus();\n" +
    "\n" +
    "  const sendOtp = async () => {\n" +
    "    if (otpVerified) return;\n" +
    "    if (sending) return;\n" +
    "    sending = true;\n" +
    "\n" +
    "    setAlert('');\n" +
    "    const p = normalizePhone(phoneInput?.value);\n" +
    "    if (!isPhoneValid(p)) { sending = false; setOtpStatus('휴대폰 번호를 확인해주세요. (숫자만 10~11자리)', false); return; }\n" +
    "    if (phoneInput) phoneInput.value = p;\n" +
    "    otpVerified = false;\n" +
    "    if (otpInput) otpInput.value = '';\n" +
    "    setOtpStatus('인증번호 발송 중...', true);\n" +
    "\n" +
    "    try {\n" +
    "      if (sendBtn) sendBtn.disabled = true;\n" +
    "      const res = await fetch('/api/phone/otp/send', {\n" +
    "        method: 'POST',\n" +
    "        headers: { 'Content-Type': 'application/json' },\n" +
    "        credentials: 'include',\n" +
    "        body: JSON.stringify({ phone: p })\n" +
    "      });\n" +
    "      const json = await res.json().catch(() => null);\n" +
    "      if (!res.ok || !json || json.ok !== true) {\n" +
    "        const msg = (json && (json.message || json.errorMessage)) ? (json.message || json.errorMessage) : '인증번호 발송에 실패했습니다.';\n" +
    "        setOtpStatus(msg, false);\n" +
    "        return;\n" +
    "      }\n" +
    "      setOtpStatus('인증번호를 발송했습니다. 문자로 받은 6자리를 입력해주세요.', true);\n" +
    "    } catch (e) {\n" +
    "      setOtpStatus('네트워크 오류로 발송에 실패했습니다.', false);\n" +
    "    } finally {\n" +
    "      sending = false;\n" +
    "      if (sendBtn && !otpVerified) sendBtn.disabled = false;\n" +
    "    }\n" +
    "  };\n" +
    "\n" +
    "  const verifyOtp = async () => {\n" +
    "    if (otpVerified) return;\n" +
    "    if (verifying) return;\n" +
    "    verifying = true;\n" +
    "\n" +
    "    setAlert('');\n" +
    "    const p = normalizePhone(phoneInput?.value);\n" +
    "    const input = String(otpInput?.value || '').trim();\n" +
    "    if (!isPhoneValid(p)) { verifying = false; setOtpStatus('휴대폰 번호를 확인해주세요.', false); return; }\n" +
    "    if (!/^[0-9]{6}$/.test(input)) { verifying = false; setOtpStatus('인증번호는 6자리 숫자입니다.', false); return; }\n" +
    "\n" +
    "    try {\n" +
    "      if (verifyBtn) verifyBtn.disabled = true;\n" +
    "      const res = await fetch('/api/phone/otp/verify', {\n" +
    "        method: 'POST',\n" +
    "        headers: { 'Content-Type': 'application/json' },\n" +
    "        credentials: 'include',\n" +
    "        body: JSON.stringify({ phone: p, code: input })\n" +
    "      });\n" +
    "      const json = await res.json().catch(() => null);\n" +
    "      if (!json || json.verified !== true) {\n" +
    "        const msg = (json && (json.message || json.errorMessage)) ? (json.message || json.errorMessage) : '인증번호가 일치하지 않습니다.';\n" +
    "        otpVerified = false;\n" +
    "        setOtpStatus(msg, false);\n" +
    "        return;\n" +
    "      }\n" +
    "      otpVerified = true;\n" +
    "      setOtpStatus('휴대폰 인증이 완료되었습니다.', true);\n" +
    "      lockOtpInputs();\n" +
    "    } catch (e) {\n" +
    "      setOtpStatus('네트워크 오류로 인증에 실패했습니다.', false);\n" +
    "    } finally {\n" +
    "      verifying = false;\n" +
    "      if (verifyBtn && !otpVerified) verifyBtn.disabled = false;\n" +
    "    }\n" +
    "  };\n" +
    "\n" +
    "  const onSendClick = (e) => { e.preventDefault(); sendOtp(); };\n" +
    "  const onVerifyClick = (e) => { e.preventDefault(); verifyOtp(); };\n" +
    "\n" +
    "  if (sendBtn) sendBtn.addEventListener('click', onSendClick);\n" +
    "  if (verifyBtn) verifyBtn.addEventListener('click', onVerifyClick);\n" +
    "\n" +
    "  form.addEventListener('submit', (e) => {\n" +
    "    setAlert('');\n" +
    "    if (!email) { e.preventDefault(); setAlert('이메일 정보가 없습니다. 다시 로그인 해주세요.'); return; }\n" +
    "    if (!otpVerified) { e.preventDefault(); setAlert('휴대폰 인증을 완료해주세요.'); return; }\n" +
    "  });\n" +
    "})();",
    "(() => { if (window.AOS && typeof window.AOS.init === 'function') window.AOS.init(); })();",
];

export default function FirstSocialLoginPage() {
    usePageCss("/assets/css/pages/register.css");
    return <HtmlPage html={html} scripts={scripts} />;
}