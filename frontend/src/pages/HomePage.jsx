// src/pages/HomePage.jsx

import HtmlPage from "../components/HtmlPage";
import { usePageCss } from "../hooks/usePageCss";

// NOTE
// - CSS는 public/assets/css/pages/home_index.css 에서 제어
// - 탭: "회원님을 위한 추천공고" 1개만 표시
// - 우측 링크: "AI 자소서 코칭" 1개만 표시

const html = `
<main class="main">
  <section id="main" class="main section" data-aos="fade-up">
    <div class="container-fluid p-0">
      <div class="main-top">

        <!-- 좌측: 추천공고 -->
        <section id="section_contents" class="left-panel main_content" role="region" aria-label="추천공고">
          <div class="inner">
            <!-- 탭 -->
            <ul class="tab">
              <li>
                <button type="button" class="tab_btn active">회원님을 위한 추천공고</button>
              </li>
            </ul>

            <!-- 카드 영역 -->
            <div class="wrap_cont">
              <div class="wrap_swiper">
                <div class="wrap_slides">
                  <ul class="items">

                    <li class="item item_recruit">
                      <a href="#" class="card">
                        <div class="item_header">
                          <div class="logo"></div>
                          <div class="dday">~01.31(토)</div>
                        </div>
                        <div class="item_body">
                          <div class="title">공고 제목(placeholder)</div>
                          <div class="company">회사명(placeholder)</div>
                        </div>
                      </a>
                    </li>

                    <li class="item item_recruit">
                      <a href="#" class="card">
                        <div class="item_header">
                          <div class="logo"></div>
                          <div class="dday">D-6</div>
                        </div>
                        <div class="item_body">
                          <div class="title">공고 제목(placeholder)</div>
                          <div class="company">회사명(placeholder)</div>
                        </div>
                      </a>
                    </li>

                    <li class="item item_recruit">
                      <a href="#" class="card">
                        <div class="item_header">
                          <div class="logo"></div>
                          <div class="dday">D-7</div>
                        </div>
                        <div class="item_body">
                          <div class="title">공고 제목(placeholder)</div>
                          <div class="company">회사명(placeholder)</div>
                        </div>
                      </a>
                    </li>

                    <li class="item item_recruit">
                      <a href="#" class="card">
                        <div class="item_header">
                          <div class="logo"></div>
                          <div class="dday">D-10</div>
                        </div>
                        <div class="item_body">
                          <div class="title">공고 제목(placeholder)</div>
                          <div class="company">회사명(placeholder)</div>
                        </div>
                      </a>
                    </li>

                  </ul>
                </div>

                <!-- 4개가 한줄에 다 보이므로 버튼 숨김(CSS) -->
                <button type="button" class="btn_prev" aria-label="이전">
                  <img src="/assets/img/common/arrow.png" alt="이전" />
                </button>
                <button type="button" class="btn_next" aria-label="다음">
                  <img src="/assets/img/common/arrow.png" alt="다음" />
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 우측: 배너 + 링크 -->
        <aside class="right-panel main_side">
          <div class="main_banner">
            <div>
              <div class="banner_title">배너 영역</div>
              <div class="banner_sub">이미지/광고 삽입 예정</div>
            </div>
          </div>

          <div class="link-wrap">
            <ul>
              <li>
                <a href="#">
                  <img src="/assets/img/main/icon_guide.svg" alt="AI 자소서 코칭" />
                  <span>AI 자소서 코칭</span>
                </a>
              </li>
            </ul>
          </div>
        </aside>

      </div>
    </div>
  </section>
</main>
`;

const scripts = [];

export default function HomePage() {
    usePageCss("/assets/css/pages/home_index.css");
    return <HtmlPage html={html} scripts={scripts} />;
}
