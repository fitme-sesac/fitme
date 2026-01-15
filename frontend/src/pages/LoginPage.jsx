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

        <!-- Services 2 Section -->
        <section id="services-2" class="services-2 section">
            <div class="container mb-3">
                <div class="row justify-content-center" data-aos="fade-up">
                    <div class="col-md-6 col-lg-6">
                        <h2 style="font-weight: bold; margin-bottom: 3rem;">회원 로그인</h2>

                        <form action="/Login" method="post">
    <input type="hidden" />
                            <!-- 아이디 -->
                            <div class="input-wrap mb-3">
                                <label for="userid"><strong>아이디</strong></label>
                                <input type="text" id="userid" name="userid" required placeholder="아이디를 입력하세요">
                            </div>
                            <!-- 비밀번호 -->
                            <div class="input-wrap mb-3">
                                <label for="password"><strong>비밀번호</strong></label>
                                <input type="password" id="password" name="password" required placeholder="비밀번호를 입력하세요">
                            </div>
                            <!-- 링크 -->
                            <ul class="link-wrap mb-3">
                                <li><a href="#" onclick="location.href='/User/Find_Userid'">아이디 찾기</a> |</li>
                                <li><a href="#" onclick="location.href='/User/Find_Password'">비밀번호 찾기</a> |</li>
                                <li><a href="#" onclick="location.href='/User/Register'">회원가입</a></li>
                            </ul>

                            <!-- 로그인 버튼 -->
                            <div style="display:flex; justify-content:flex-end; width:100%; margin-top:0.2rem">
                                <button type="submit" class="btn btn-success rounded-pill">로그인</button>
                            </div>

                            <div style="display:flex; justify-content:flex-end; width:100%; margin-top:0.2rem">
                                <button
                                        type="button"
                                        class="gsi-material-button"
                                        onclick="location.href='/oauth2/authorization/google?prompt=select_account'">
                                    <div class="gsi-material-button-state"></div>
                                    <div class="gsi-material-button-content-wrapper">
                                        <div class="gsi-material-button-icon">
                                            <!-- Google 로고 SVG -->
                                            <svg viewBox="0 0 48 48" style="display:block;">
                                                <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                                                <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                                                <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                                                <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                                                <path fill="none" d="M0 0h48v48H0z"/>
                                            </svg>
                                        </div>
                                        <span class="gsi-material-button-contents">Sign in with Google</span>
                                        <span style="display:none">Sign in with Google</span>
                                    </div>
                                </button>
                            </div>

                            <div style="display:flex; justify-content:flex-end; width:100%; margin-top:0.2rem">
                                <button
                                        type="button"
                                        class="gsi-material-button"
                                        style="background-color:#FEE500; border-color:#FEE500;"
                                        onclick="location.href='/oauth2/authorization/kakao'">
                                    <div class="gsi-material-button-state"></div>
                                    <div class="gsi-material-button-content-wrapper">
                                        <div class="gsi-material-button-icon">
                                            <!-- Kakao 로고(단순화) SVG -->
                                            <svg viewBox="0 0 24 24" style="display:block;">
                                                <path d="M12 3C6.477 3 2 6.58 2 11c0 2.78 1.79 5.22 4.53 6.67-.2.74-.74 2.68-.85 3.09-.13.5.18.5.38.37.16-.1 2.55-1.73 3.58-2.44.77.11 1.57.17 2.36.17 5.523 0 10-3.58 10-8s-4.477-8-10-8z" fill="#000000"/>
                                            </svg>
                                        </div>
                                        <span class="gsi-material-button-contents">Sign in with Kakao</span>
                                        <span style="display:none">Sign in with Kakao</span>
                                    </div>
                                </button>
                            </div>

                            <!-- 로그인 실패 메시지(쿼리 파라미터 기반) -->
                            <div class="alert alert-danger mt-3">
                                <span></span>
                            </div>

                            <!-- (혹시 컨트롤러에서 model로 내려주는 경우도 대비) -->
                            <div class="alert alert-danger mt-3">
                                <span></span>
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
    "  const msg = params.get('errorMessage') || params.get('error') || '';\n" +
    "  const alerts = document.querySelectorAll('.alert.alert-danger');\n" +
    "  alerts.forEach((a) => {\n" +
    "    const span = a.querySelector('span');\n" +
    "    if (span) span.textContent = msg || ''; else a.textContent = msg || '';\n" +
    "    a.style.display = msg ? 'block' : 'none';\n" +
    "  });\n" +
    "})();",
  "AOS.init();",
];

export default function LoginPage() {
  usePageCss("/assets/css/pages/login.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
