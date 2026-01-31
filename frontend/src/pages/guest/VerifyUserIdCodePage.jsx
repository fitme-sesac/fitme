import HtmlPage from "@/components/HtmlPage";

import { usePageCss } from "@/hooks/usePageCss";
const html = `<main class="main">

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

        <section class="services-2 section">
            <div class="container mb-3">
                <div class="row justify-content-center">
                    <div class="col-md-6 col-lg-6">

                        <h2 style="font-weight: bold; color: #222; line-height: 4rem; margin-bottom: 3rem;">인증번호 확인</h2>

                        <div class="alert alert-info" role="alert">
                            입력하신 이메일로 인증번호를 발송했습니다.
                            <br>
                            <strong></strong>
                        </div>

                        <!-- ✅ 에러 메시지 -->
                        <div class="alert alert-danger" role="alert"></div>

                        <form action="/User/Verify_Userid_Code" method="post">
    <input type="hidden" />

                            <div class="input-wrap">
                                <label for="inputCode" style="margin-bottom: 1rem"><strong>인증번호</strong></label>
                                <input type="text" id="inputCode" name="inputCode" required
                                       placeholder="6자리 인증번호를 입력하세요"
                                       onfocus="this.placeholder=''" onblur="this.placeholder='6자리 인증번호를 입력하세요'">
                            </div>

                            <div style="display: flex; justify-content: flex-end; width: 100%; margin-top: 1rem">
                                <button type="submit" class="btn btn-success rounded-pill me-2">확인</button>
                                <a href="/User/Find_Userid" class="btn btn-secondary rounded-pill">다시받기</a>
                            </div>

                        </form>
                    </div>
                </div>
            </div>
        </section>
    </main>`;
const scripts = [];

export default function VerifyUserIdCodePage() {
  usePageCss("/assets/css/pages/verify_userid_code.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
