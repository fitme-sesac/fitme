import HtmlPage from "@/components/HtmlPage";

import { usePageCss } from "@/hooks/usePageCss";
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

        <!-- Services 2 Section -->
        <section id="services-2" class="services-2 section">

            <div class="container mb-3">
                <div class="row justify-content-center" data-aos="fade-up">
                    <div class="col-md-6 col-lg-6">

                        <h2 style="font-weight: bold; color: #222;  line-height: 4rem; margin-bottom: 3rem;">아이디/비밀번호 확인</h2>
                        <form id="reEnterCredentialsForm" action="/User/Re_Enter_Credentials" method="post">
    <input type="hidden" />
                            <!--아이디-->
                            <div class="input-wrap">
                                <label for="userid" style="margin-bottom: 1rem"><strong>아이디</strong></label>
                                <input type="text" id="userid" name="userid" required
                                       placeholder="아이디를 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '아이디를 입력하세요'">
                            </div>

                            <!--비밀번호-->
                            <div class="input-wrap">
                                <label for="password" style="margin-bottom: 1rem"><strong>비밀번호</strong></label>
                                <input type="password" id="password" name="password" required
                                       placeholder="비밀번호를 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '현재 비밀번호를 입력하세요'">
                            </div>

                            <!--비밀번호 확인-->
                            <div class="input-wrap">
                                <label for="confirmPassword" style="margin-bottom: 1rem"><strong>비밀번호 확인</strong></label>
                                <input type="password" id="confirmPassword" name="confirmPassword" required
                                       placeholder="현재 비밀번호를 다시 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '현재 비밀번호를 다시 입력하세요'">
                            </div>


                            <!--확인-->
                            <div style="display: flex; justify-content: flex-end; width: 100%; margin-top: 1rem">
                                <button type="submit" class="btn btn-success rounded-pill me-2">확인</button>
                                <button type="reset" class="btn btn-secondary rounded-pill">취소</button>
                            </div>

                            <!-- 오류 메시지 출력 -->
                            <div class="alert alert-danger mt-3" role="alert">
                                <span></span>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </section>

        <script src="/assets/vendor/bootstrap/js/bootstrap.bundle.min.js"></script>
    </main>`;
const scripts = [
  "(() => {\n" +
    "  const form = document.getElementById('reEnterCredentialsForm') || document.querySelector('form');\n" +
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
    "    const p1 = String(document.getElementById('password')?.value || '');\n" +
    "    const p2 = String(document.getElementById('confirmPassword')?.value || '');\n" +
    "    if (p1 !== p2) { e.preventDefault(); setAlert('비밀번호와 비밀번호 확인 값이 일치하지 않습니다.'); }\n" +
    "  });\n" +
    "})();",
  "AOS.init();",
];

export default function ReEnterCredentialsPage() {
  usePageCss("/assets/css/pages/re_enter_credentials.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
