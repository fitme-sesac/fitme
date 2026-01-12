import HtmlPage from "../components/HtmlPage";
import { usePageCss } from "../hooks/usePageCss";

const html = `<main class="main">
    <!-- 메인페이지 상단 -->
    <section id="main" class="main section" data-aos="fade-up" style="position: relative; margin-top:  -60px;">
      <div class="container-fluid p-0">
        <div class="row justify-content-center">
          <!-- 왼쪽 이미지 (세로 길이 작게 설정, 둥근 모서리 추가) -->
          <div class="col-md-7 col-lg-7 p-0" style="position: relative;">
            <img src="/assets/img/main/kia_home.png" alt="기아 챔피언스 필드" class="img-fluid w-100"
                 style="height: 500px; object-fit: cover; border-top-left-radius: 20px; border-bottom-left-radius: 20px;">
            <div class="text-wrap" style="position: absolute; top: 20px; left: 20px; color: white; z-index: 1;">
              <span>KIA TIGERS 응원 플랫폼</span>
              <p>호랑이 발검음으로<br class="hidden-pc"> 우승을 향해</p>
            </div>
          </div>


          <!-- 오른쪽 초록색 배경과 흰색 글씨 (둥근 모서리 추가) -->
          <div class="col-md-2 col-lg-2 d-flex align-items-center"
               style="background-color: #161616; color: white; padding: 0; border-top-right-radius: 20px;
     border-bottom-right-radius: 20px; justify-content: center; height: 500px; width:300px"> <!-- 높이를 고정 -->
            <div style="border-radius: 10px; padding: 20px; width: 300px; height: 500px;"> <!-- 너비와 높이 고정 -->
              <div class="link-title" style="color: #ffffff;">
                <h3 style="font-weight: bold; color: #ffffff;">종합정보</h3>
                <p style="margin-bottom: 20px;">주요정보를 확인해보세요</p>
                <hr style="border: 1px; margin-bottom: 30px;">
              </div>
              <div class="link-wrap">
                <ul style="padding: 0;"> <!-- 패딩을 제거하여 항목들 정렬 -->
                  <li style="height: 100px; background-color: #ea0930;"> <!-- 고정된 높이 설정 -->
                    <a href="/document/list" style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #ea0930;" >
                      <img src="/assets/img/main/schedule4.png" alt="경기일정">
                      <span style="color: #ffffff;">경기일정</span>
                    </a>
                  </li>
                  <li style="height: 100px; background-color: #ea0930;">
                    <a href="/plantation/list" style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #ea0930;">
                      <img src="/assets/img/main/player.png" alt="선수단">
                      <span style="color: #ffffff;">선수단</span>
                    </a>
                  </li>
                  <li style="height: 100px; background-color: #ea0930;">
                    <a href="/fruit/list" style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #ea0930;">
                      <img src="/assets/img/main/cheer.png" alt="응원문화">
                      <span style="color: #ffffff;">응원문화</span>
                    </a>
                  </li>
                  <li style="height: 100px; background-color: #ea0930;">
                    <a href="/diagnose" style="height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #ea0930;">
                      <img src="/assets/img/main/history.png" alt="역사관">
                      <span style="color: #ffffff;">역사관</span>
                    </a>
                  </li>
                </ul>
              </div>
            </div>
          </div>

        </div>

        <!-- 주요서비스 섹션 -->
        <div class="container mt-4 mb-3">
          <!-- 주요서비스 -->
          <div class="title-wrap">
            <p class="mb-2">주요서비스</p>
            <span class="hidden-mo">로그인하시면 더 많은 서비스를 이용하실 수 <br>있습니다.</span>
          </div>

          <!-- 서비스 메뉴들 -->
          <div class="service-menu">
            <ul class="featured-services d-flex flex-wrap justify-content-center">
              <li class="guide">
                <a href="/guide/info" class="guide_step">
                  <img src="/assets/img/main/icon_guide.svg" alt="가이드">
                  <span>가이드</span>
                </a>
              </li>
              <li class="experience">
                <a href="/experience/list">
                  <img src="/assets/img/main/icon_experience.svg" alt="체험정보">
                  <span>체험정보</span>
                </a>
              </li>
              <li class="event">
                <a href="/event/list">
                  <img src="/assets/img/main/icon_event.svg" alt="행사정보">
                  <span>행사정보</span>
                </a>
              </li>
              <li class="board">
                <a href="/board/list">
                  <img src="/assets/img/main/icon_board.svg" alt="게시판">
                  <span>게시판</span>
                </a>
              </li>
              <li class="QnA">
                <a href="/qna/list">
                  <img src="/assets/img/main/icon_qna.svg" alt="자주묻는질문">
                  <span>Q&A</span>
                </a>
              </li>
            </ul>
          </div>
        </div>

      </div>
    </section>
  </main>`;

const scripts = ["function adjustDivWidth() {\n      const targetDiv = document.querySelector('.col-md-2'); // 타겟 div 선택\n      if (window.innerWidth <=767) {\n        targetDiv.style.width = '100%'; // 768px 이하일 때 너비를 100%로 설정\n      } else {\n        targetDiv.style.width = '300px'; // 기본 너비로 설정\n      }\n    }\n\n    // 페이지 로드 시 및 창 크기 조정 시 함수 실행\n    window.addEventListener('load', adjustDivWidth);\n    window.addEventListener('resize', adjustDivWidth);"];

export default function HomePage() {
    // ✅ 비로그인 메인에서만 깨지던 “Thymeleaf 인라인 CSS”를 파일로 분리한 것을 로드
    usePageCss("/assets/css/pages/home_index.css");

    return <HtmlPage html={html} scripts={scripts} />;
}
