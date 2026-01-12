import HtmlPage from "../components/HtmlPage";

import { usePageCss } from "../hooks/usePageCss";
const html = `<main class="main">

        <!-- Page Title -->
        <div class="page-title" style="background-color: #003300;">
            <div class="container text-center">
                <h1 style="color: white;">회원 서비스</h1>
                <nav class="breadcrumbs">
                    <div>
                        <span class="responsive-span" style="color: white;">
                            한국 야구를 사랑하는, KIA팬 여러분의 열정 가득한 응원 공간에 오신 것을 환영합니다!
                        </span>
                    </div>
                </nav>
            </div>
        </div>

        <section id="services-2" class="services-2 section">
            <div class="container mb-3">
                <div class="row justify-content-center" data-aos="fade-up">
                    <div class="col-md-6 col-lg-6">

                        <h2 style="font-weight: bold; color: #222; line-height: 4rem; margin-bottom: 3rem;">아이디찾기</h2>

                        <!-- ✅ 에러 메시지 -->
                        <div class="alert alert-danger" role="alert"></div>

                        <!-- ✅ 이메일만 입력 -->
                        <form action="/User/Find_Userid" method="post">
    <input type="hidden" />

                            <div class="input-wrap">
                                <label for="email" style="margin-bottom: 1rem"><strong>이메일</strong></label>
                                <input type="text" id="email" name="email" required
                                       placeholder="이메일주소를 입력하세요"
                                       onfocus="this.placeholder = ''" onblur="this.placeholder = '이메일주소를 입력하세요'">
                            </div>

                            <div style="display: flex; justify-content: flex-end; width: 100%; margin-top: 1rem">
                                <button type="submit" class="btn btn-success rounded-pill me-2">인증번호 받기</button>
                                <button type="reset" class="btn btn-secondary rounded-pill">취소</button>
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
    "  const err = document.querySelector('.alert.alert-danger');\n" +
    "  if (err) { err.textContent = msg || ''; err.style.display = msg ? 'block' : 'none'; }\n" +
    "})();",
  "AOS.init();",
];

export default function FindUserIdPage() {
  usePageCss("/assets/css/pages/find_userid.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
