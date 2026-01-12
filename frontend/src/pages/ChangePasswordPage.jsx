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

        <section id="change-password" class="change-password section">

            <div class="container mb-3">
                <div class="row justify-content-center" data-aos="fade-up">
                    <div class="col-md-6 col-lg-6">

                        <h2 style="font-weight: bold; color: #222;  line-height: 4rem; margin-bottom: 3rem;">비밀번호 변경</h2>
                        <form id="changePasswordForm" action="/User/Change_Password" method="post">
    <input type="hidden" />

                            <!--현재 비밀번호-->
                            <div class="input-wrap">
                                <label for="currentPassword" style="margin-bottom: 1rem"><strong>현재 비밀번호</strong></label>
                                <input type="password" id="currentPassword" name="currentPassword" required
                                       placeholder="현재 비밀번호를 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '현재 비밀번호를 입력하세요'">
                            </div>

                            <!--새비밀번호-->
                            <div class="input-wrap">
                                <label for="newPassword" style="margin-bottom: 1rem"><strong>새 비밀번호</strong></label>
                                <input type="password" id="newPassword" name="newPassword" required
                                       placeholder="새 비밀번호를 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '새 비밀번호를 입력하세요'">
                            </div>
                            <div class="input-wrap">
                                <label for="confirmPassword" style="margin-bottom: 1rem"><strong>새 비밀번호 확인</strong></label>
                                <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" required
                                       placeholder="새 비밀번호를 다시 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '새 비밀번호를 다시 입력하세요'">
                            </div>

                            <div class="alert alert-danger">
                                <span></span>
                            </div>

                            <!--확인-->
                            <div style="display: flex; justify-content: flex-end; width: 100%; margin-top: 1rem">
                                <button type="submit" class="btn btn-success rounded-pill me-2">확인</button>
                                <a href="/Login" class="btn btn-secondary rounded-pill mb-2" style="display: inline-block; text-align: center; min-width: 110px;">취소</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </section><!-- /Change Password Section -->
    </main>`;
const scripts = [
  "(() => {\n" +
    "  const form = document.getElementById('changePasswordForm') || document.querySelector('form');\n" +
    "  if (!form) return;\n" +
    "  const alertEl = document.querySelector('.alert.alert-danger');\n" +
    "  const span = alertEl ? alertEl.querySelector('span') : null;\n" +
    "  const setAlert = (msg) => {\n" +
    "    if (!alertEl) { if (msg) alert(msg); return; }\n" +
    "    if (span) span.textContent = msg || ''; else alertEl.textContent = msg || '';\n" +
    "    alertEl.style.display = msg ? 'block' : 'none';\n" +
    "  };\n" +
    "  setAlert('');\n" +
    "  form.addEventListener('submit', (e) => {\n" +
    "    setAlert('');\n" +
    "    const p1 = String(document.getElementById('newPassword')?.value || '');\n" +
    "    const p2 = String(document.getElementById('confirmPassword')?.value || '');\n" +
    "    if (p1 !== p2) { e.preventDefault(); setAlert('새 비밀번호와 확인 값이 일치하지 않습니다.'); }\n" +
    "  });\n" +
    "})();",
  "AOS.init();",
];

export default function ChangePasswordPage() {
  usePageCss("/assets/css/pages/change_password.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
