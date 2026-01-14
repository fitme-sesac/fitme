// RegisterPage.tsx (전체) - SOLAPI 연동 전제(프론트는 devCode/로컬OTP 제거 + 쿠키기반 검증만)
import HtmlPage from "../components/HtmlPage";
import { usePageCss } from "../hooks/usePageCss";

const html = `<main class="main">
  <!-- Page Title -->
  <div class="page-title" style="background-color: #003300; margin-bottom: 30px;">
    <div class="container text-center">
      <h1 style="color: white;">회원가입</h1>
      <nav class="breadcrumbs">
        <div>
          <span class="responsive-span" style="color: white;">
            회원으로 가입 하시면 다양한 서비스를 조회 및 편리하게 이용하실 수 있습니다.
          </span>
        </div>
      </nav>
    </div>
  </div>

  <!-- 회원가입 폼 (한 페이지) -->
  <section id="services-2" class="services-2 section">
    <div class="container mb-3">
      <div class="row justify-content-center" data-aos="fade-up">
        <div class="col-md-8 col-lg-8">
          <h2 style="font-weight: bold; color: #222; line-height: 4rem;">회원 기본정보</h2>
          <span style="font-weight: bold; color: #222; line-height: 1rem;">
            <span style="color: red;">*</span>는 필수입력 사항입니다.
          </span>
          <hr style="border: 1px solid #111;">

          <div class="alert alert-danger">
            <span></span>
          </div>

          <form id="registerForm" action="/User/Register" method="post">
            <input type="hidden" />

            <!-- 아이디 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="userid">아이디 <span style="color: red;">*</span></label>
              <input
                type="text"
                id="userid"
                name="userid"
                class="form-control"
                required
                pattern="^[a-z][a-z0-9]{7,19}$"
                maxlength="20"
                title="아이디는 영문 소문자로 시작해야 하며, 8~20자여야 합니다."
                placeholder="아이디를 입력하세요"
              />
              <span class="help-text">영문 소문자로 시작, 영문/숫자 조합 8~20자</span>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 비밀번호 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="password">비밀번호 <span style="color: red;">*</span></label>
              <input
                type="password"
                id="password"
                name="password"
                class="form-control"
                required
                pattern="(?=.*[0-9])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,}"
                maxlength="64"
                title="비밀번호는 8자 이상이며, 숫자 및 특수문자를 하나 이상 포함해야 합니다."
                placeholder="비밀번호를 입력하세요"
              />
              <span class="help-text">8자 이상 + 숫자 1개 이상 + 특수문자 1개 이상</span>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 비밀번호 재확인 -->
            <div class="form-group" style="font-weight: bold;">
              <label for="passwordConfirm">비밀번호 재확인 <span style="color: red;">*</span></label>
              <input
                type="password"
                id="passwordConfirm"
                name="passwordConfirm"
                class="form-control"
                required
                maxlength="64"
                placeholder="비밀번호를 다시 입력하세요"
              />
            </div>
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
                <option value="" selected disabled>선택</option>
                <option value="MALE">남성</option>
                <option value="FEMALE">여성</option>
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

            <!-- 이메일 (표시는 1칸, 전송은 emailId/emailDomain/emailTLD) -->
            <div class="form-group" style="font-weight: bold;">
              <label for="emailFull">이메일 <span style="color: red;">*</span></label>
              <input type="email" id="emailFull" class="form-control" required placeholder="example@gmail.com" />
            </div>
            <input type="hidden" id="emailId" name="emailId" />
            <input type="hidden" id="emailDomain" name="emailDomain" />
            <input type="hidden" id="emailTLD" name="emailTLD" />
            <input type="hidden" id="email" name="email" />
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
                      <p>서비스 이용 조건, 계정 관리, 금지행위, 책임 제한 등을 포함합니다.</p>
                    </div>
                    <div class="policy-agree">
                      <label>
                        <input type="checkbox" id="agreeTerms" name="agreeTerms" value="true" />
                        (필수) 이용약관 동의
                      </label>
                    </div>
                  </div>

                  <div class="policy-section">
                    <div class="policy-title">[개인정보 처리방침]</div>
                    <div class="policy-box">
                      <p>수집 항목, 이용 목적, 보관 기간, 파기 절차, 제3자 제공 및 위탁 등을 포함합니다.</p>
                    </div>
                    <div class="policy-agree">
                      <label>
                        <input type="checkbox" id="agreePrivacy" name="agreePrivacy" value="true" />
                        (필수) 개인정보 처리방침 동의
                      </label>
                    </div>
                  </div>

                  <div class="policy-section">
                    <div class="policy-title">[운영정책]</div>
                    <div class="policy-box">
                      <p>커뮤니티 운영 원칙, 콘텐츠 규정, 제재 기준 등을 포함합니다.</p>
                    </div>
                    <div class="policy-agree">
                      <label>
                        <input type="checkbox" id="agreePolicy" name="agreePolicy" value="true" />
                        (필수) 운영정책 동의
                      </label>
                    </div>
                  </div>
                </div>

                <!-- 선택 동의(마케팅) -->
                <div class="policy-marketing">
                  <label>
                    <input type="checkbox" id="marketingOptIn" name="marketingOptIn" value="true" />
                    (선택) 마케팅 수신 동의
                  </label>
                </div>
              </div>
            </div>
            <hr style="height:0.6px;background:#888;border:none;width:100%;margin:0;">

            <!-- 버튼 -->
            <div class="d-flex mt-5 button-group" style="justify-content:flex-end;">
              <button type="submit" class="btn btn-success rounded-pill">회원가입</button>
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
    "  const form = document.getElementById('registerForm');\n" +
    "  if (!form) return;\n" +
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
    "  const params = new URLSearchParams(window.location.search);\n" +
    "  const em = params.get('errorMessage') || params.get('error') || '';\n" +
    "  if (em) setAlert(em);\n" +
    "\n" +
    "  const qs = (sel) => document.querySelector(sel);\n" +
    "\n" +
    "  // 이메일 분해(서버 전송용 hidden)\n" +
    "  const emailFull = qs('#emailFull');\n" +
    "  const emailId = qs('#emailId');\n" +
    "  const emailDomain = qs('#emailDomain');\n" +
    "  const emailTLD = qs('#emailTLD');\n" +
    "  const emailHidden = qs('#email');\n" +
    "\n" +
    "  // ✅ naver.co.kr 같은 케이스도 지원(domain=naver, tld=co.kr)\n" +
    "  const splitEmail = (v) => {\n" +
    "    const s = (v || '').trim();\n" +
    "    const at = s.lastIndexOf('@');\n" +
    "    if (at <= 0) return null;\n" +
    "    const local = s.slice(0, at);\n" +
    "    const host = s.slice(at + 1);\n" +
    "    const parts = host.split('.').filter(Boolean);\n" +
    "    if (!local || parts.length < 2) return null;\n" +
    "    const domain = parts.shift();\n" +
    "    const tld = parts.join('.');\n" +
    "    return { id: local, domain, tld };\n" +
    "  };\n" +
    "\n" +
    "  const fillEmailParts = () => {\n" +
    "    const v = (emailFull?.value || '').trim();\n" +
    "    if (!v) { setAlert('이메일을 입력해주세요.'); return false; }\n" +
    "    const parts = splitEmail(v);\n" +
    "    if (!parts) { setAlert('이메일 형식이 올바르지 않습니다.'); return false; }\n" +
    "    if (emailId) emailId.value = parts.id;\n" +
    "    if (emailDomain) emailDomain.value = parts.domain;\n" +
    "    if (emailTLD) emailTLD.value = parts.tld;\n" +
    "    if (emailHidden) emailHidden.value = v;\n" +
    "    return true;\n" +
    "  };\n" +
    "\n" +
    "  // 휴대폰 OTP\n" +
    "  let otpVerified = false;\n" +
    "  const phoneInput = qs('#phone');\n" +
    "  const otpInput = qs('#phoneOtpCode');\n" +
    "  const otpStatus = qs('#otpStatus');\n" +
    "  const sendBtn = qs('#sendOtpBtn');\n" +
    "  const verifyBtn = qs('#verifyOtpBtn');\n" +
    "\n" +
    "  const normalizePhone = (p) => String(p || '').replace(/[^0-9]/g, '');\n" +
    "  const isPhoneValid = (p) => /^[0-9]{10,11}$/.test(p);\n" +
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
    "  // ✅ 새로고침/재접속 대비: 서버 쿠키 기준으로 인증 상태 동기화\n" +
    "  const syncOtpStatus = async () => {\n" +
    "    try {\n" +
    "      const res = await fetch('/api/phone/otp/status', { credentials: 'include' });\n" +
    "      const json = await res.json().catch(() => null);\n" +
    "      if (json && json.verified === true) {\n" +
    "        otpVerified = true;\n" +
    "        setOtpStatus('휴대폰 인증이 완료되었습니다.', true);\n" +
    "        lockOtpInputs();\n" +
    "      }\n" +
    "    } catch (e) {\n" +
    "      // status 실패는 치명적이지 않음(그냥 다시 인증 유도)\n" +
    "    }\n" +
    "  };\n" +
    "  syncOtpStatus();\n" +
    "\n" +
    "  const sendOtp = async () => {\n" +
    "    setAlert('');\n" +
    "    const p = normalizePhone(phoneInput?.value);\n" +
    "    if (!isPhoneValid(p)) { setOtpStatus('휴대폰 번호를 확인해주세요. (숫자만 10~11자리)', false); return; }\n" +
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
    "      if (sendBtn) sendBtn.disabled = false;\n" +
    "    }\n" +
    "  };\n" +
    "\n" +
    "  const verifyOtp = async () => {\n" +
    "    setAlert('');\n" +
    "    const p = normalizePhone(phoneInput?.value);\n" +
    "    const input = String(otpInput?.value || '').trim();\n" +
    "    if (!isPhoneValid(p)) { setOtpStatus('휴대폰 번호를 확인해주세요.', false); return; }\n" +
    "    if (!/^[0-9]{6}$/.test(input)) { setOtpStatus('인증번호는 6자리 숫자입니다.', false); return; }\n" +
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
    "\n" +
    "      otpVerified = true;\n" +
    "      setOtpStatus('휴대폰 인증이 완료되었습니다.', true);\n" +
    "      lockOtpInputs();\n" +
    "    } catch (e) {\n" +
    "      setOtpStatus('네트워크 오류로 인증에 실패했습니다.', false);\n" +
    "    } finally {\n" +
    "      if (verifyBtn) verifyBtn.disabled = false;\n" +
    "    }\n" +
    "  };\n" +
    "\n" +
    "  if (sendBtn) sendBtn.addEventListener('click', (e) => { e.preventDefault(); sendOtp(); });\n" +
    "  if (verifyBtn) verifyBtn.addEventListener('click', (e) => { e.preventDefault(); verifyOtp(); });\n" +
    "\n" +
    "  form.addEventListener('submit', (e) => {\n" +
    "    setAlert('');\n" +
    "\n" +
    "    const pw = String(qs('#password')?.value || '');\n" +
    "    const pw2 = String(qs('#passwordConfirm')?.value || '');\n" +
    "    if (pw !== pw2) { e.preventDefault(); setAlert('비밀번호와 비밀번호 재확인이 일치하지 않습니다.'); return; }\n" +
    "\n" +
    "    if (!fillEmailParts()) { e.preventDefault(); return; }\n" +
    "\n" +
    "    const agreeTerms = qs('#agreeTerms');\n" +
    "    const agreePrivacy = qs('#agreePrivacy');\n" +
    "    const agreePolicy = qs('#agreePolicy');\n" +
    "    if (!agreeTerms?.checked || !agreePrivacy?.checked || !agreePolicy?.checked) {\n" +
    "      e.preventDefault();\n" +
    "      setAlert('필수 약관에 동의해야 회원가입이 가능합니다.');\n" +
    "      return;\n" +
    "    }\n" +
    "\n" +
    "    if (!otpVerified) {\n" +
    "      e.preventDefault();\n" +
    "      setAlert('휴대폰 인증을 완료해주세요.');\n" +
    "      return;\n" +
    "    }\n" +
    "  });\n" +
    "})();",
    "AOS.init();",
];

export default function RegisterPage() {
    usePageCss("/assets/css/pages/register.css");
    return <HtmlPage html={html} scripts={scripts} />;
}
