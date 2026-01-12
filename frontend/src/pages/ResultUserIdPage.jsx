import HtmlPage from "../components/HtmlPage";

import { usePageCss } from "../hooks/usePageCss";
const html = `<main class="main">

        <div class="page-title" style="background-color: #003300;">
            <div class="container text-center">
                <h1 style="color: white;">회원 서비스</h1>
            </div>
        </div>

        <section class="services-2 section">
            <div class="container mb-3">
                <div class="row justify-content-center">
                    <div class="col-md-6 col-lg-6">

                        <h2 style="font-weight: bold; color: #222; line-height: 4rem; margin-bottom: 2rem;">아이디 찾기 결과</h2>

                        <div class="alert alert-success" role="alert">
                            당신의 아이디는: sampleId
                        </div>

                        <div class="d-flex justify-content-end">
                            <a href="/Login" class="btn btn-success rounded-pill me-2">로그인</a>
                            <a href="/User/Find_Password" class="btn btn-secondary rounded-pill">비밀번호 찾기</a>
                        </div>

                    </div>
                </div>
            </div>
        </section>

    </main>`;
const scripts = [];

export default function ResultUserIdPage() {
  usePageCss("/assets/css/pages/find_userid.css");
  return <HtmlPage html={html} scripts={scripts} />;
}
