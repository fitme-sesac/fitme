import HtmlPage from "../components/HtmlPage";

import { usePageCss } from "../hooks/usePageCss";

const html = `<main class="main">

  <!-- Page Title -->
  <div class="page-title" style="background-color: #003300;">
    <div class="container text-center">
      <h1 style="color: white;">회원 서비스</h1>
      <nav class="breadcrumbs">
        <div>
          <span class="responsive-span" style="color: white;">한국 야구를 사랑하는, KIA팬 여러분의 열정 가득한 응원 공간에 오신 것을 환영합니다!</span>
        </div>
      </nav>
    </div>
  </div>

  <section id="services-2" class="services-2 section">
    <div class="container mb-3">
      <div class="row justify-content-center" data-aos="fade-up">
        <div class="col-md-6 col-lg-6">

          <h2 style="font-weight: bold; color: #222;  line-height: 4rem; margin-bottom: 3rem;">새 비밀번호 설정</h2>

          <div class="alert alert-danger" role="alert" style="display:none;"></div>

          <form id="newPasswordForm" action="/User/New_Password" method="post">
            <input type="hidden" id="userid" name="userid" />

            <div class="input-wrap">
              <label for="newPassword" style="margin-bottom: 1rem"><strong>새 비밀번호</strong></label>
              <input type="password" id="newPassword" name="newPassword" required
                     maxlength="64"
                     placeholder="새 비밀번호를 입력하세요"
                     onfocus="this.placeholder = ''" onblur="this.placeholder = '새 비밀번호를 입력하세요'">
            </div>

            <div class="input-wrap">
              <label for="confirmPassword" style="margin-bottom: 1rem"><strong>새 비밀번호 확인</strong></label>
              <input type="password" id="confirmPassword" name="confirmPassword" required
                     maxlength="64"
                     placeholder="새 비밀번호를 다시 입력하세요"
                     onfocus="this.placeholder = ''" onblur="this.placeholder = '새 비밀번호를 다시 입력하세요'">
            </div>

            <div style="display: flex; justify-content: flex-end; width: 100%; margin-top: 1rem">
              <button type="submit" class="btn btn-success rounded-pill me-2">변경</button>
            </div>
          </form>

        </div>
      </div>
    </div>
  </section>
</main>`;

const scripts = [
  "(() => {\n" +
    "  const params = new URLSearchParams(window.location.search);\n" +
    "  const userid = params.get('userid') || '';\n" +
    "  const error = params.get('error') || params.get('errorMessage') || '';\n" +
    "  const useridEl = document.getElementById('userid');\n" +
    "  if (useridEl) useridEl.value = userid;\n" +
    "  const alertEl = document.querySelector('.alert.alert-danger');\n" +
    "  const setAlert = (msg) => {\n" +
    "    if (!alertEl) { if (msg) alert(msg); return; }\n" +
    "    alertEl.textContent = msg || '';\n" +
    "    alertEl.style.display = msg ? 'block' : 'none';\n" +
    "  };\n" +
    "  if (error) setAlert(decodeURIComponent(error));\n" +
    "\n" +
    "  const form = document.getElementById('newPasswordForm');\n" +
    "  if (!form) return;\n" +
    "  form.addEventListener('submit', (e) => {\n" +
    "    setAlert('');\n" +
    "    const p1 = String(document.getElementById('newPassword')?.value || '');\n" +
    "    const p2 = String(document.getElementById('confirmPassword')?.value || '');\n" +
    "    if (p1 !== p2) { e.preventDefault(); setAlert('새 비밀번호와 확인 값이 일치하지 않습니다.'); return; }\n" +
    "    if (!userid) { e.preventDefault(); setAlert('요청 정보(userid)가 없습니다. 다시 시도해주세요.'); return; }\n" +
    "  });\n" +
    "})();",
  "AOS.init();",
];

export default function NewPasswordPage() {
  usePageCss("/assets/css/pages/new_password.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
