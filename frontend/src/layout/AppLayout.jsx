import { Outlet, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { http } from "../api/http";
import HtmlPage from "../components/HtmlPage";

/**
 * main.js를 중복 로드하지 않기 위한 헬퍼
 */
function loadScriptOnce(src) {
    return new Promise((resolve, reject) => {
        if (document.querySelector(`script[data-src="${src}"]`)) {
            resolve();
            return;
        }
        const s = document.createElement("script");
        s.src = src;
        s.defer = true;
        s.dataset.src = src;
        s.onload = resolve;
        s.onerror = reject;
        document.body.appendChild(s);
    });
}

/**
 * ✅ 핵심 변경: header도 Home의 "row justify-content-center" + (7+2)폭(=md-9) 기준에 맞춤
 * - container-fluid p-0
 * - row justify-content-center g-0
 * - col-12 col-md-9 p-0  (홈의 col-md-7 + col-md-2 묶음 폭과 동일)
 */
const headerHtml = (isAuthenticated, displayName, apiBase) => `
  <header id="header" class="header sticky-top" style="padding-bottom: 10px;">
    <div class="container-fluid p-0">
      <div class="row justify-content-center g-0">
        <div class="col-12 col-md-9 px-2">
          <div class="d-flex align-items-center justify-content-between py-2">
            <!-- 로고 (왼쪽 고정) -->
            <a href="/" class="logo d-flex align-items-center" style="margin-right: 0; padding-left: 0;">
              <img
                src="/assets/img/main/fit_me_logo.png"
                alt="로고"
                style="height:50px; width:auto; display:block;"
              >
            </a>

            <div class="d-flex align-items-center">
              <nav id="navmenu" class="navmenu me-3">
                <ul>
                  <li><a href="/admin/user_info">관리/운영자</a></li>
                  <li><a href="/guide/info">채용공고</a></li>
                  <li><a href="/diagnose">이력서 팁</a></li>
                  <li><a href="/board/list">고객지원</a></li>

                  <li class="dropdown">
                    <a href="#">
                      <span>${isAuthenticated && displayName ? `${displayName}님` : "방문객"}</span>
                      <i class="bi bi-chevron-down toggle-dropdown"></i>
                    </a>
                    <ul>
                      ${
    isAuthenticated
        ? `
                        <li><a class="dropdown-item" href="/User/Update">회원수정</a></li>

                        <li>
                          <a href="#" class="dropdown-item"
                             onclick="this.nextElementSibling.submit(); return false;">
                            로그아웃
                          </a>
                          <form action="${apiBase}/Logout" method="post" style="display:none;">
                            <input type="hidden" />
                          </form>
                        </li>
                          `
        : `
                        <li><a href="/Login">로그인</a></li>
                        <li><a href="/User/Register">회원가입</a></li>
                          `
}
                    </ul>
                  </li>
                </ul>
                <i class="mobile-nav-toggle d-xl-none bi bi-list"></i>
              </nav>

              <a class="btn-getstarted" href="#">
                마이페이지
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
`;

const footerHtml = `
  <footer id="footer" class="footer" style="background-color: #f0f0f0; padding-top: 0;">
    <!-- ✅ 헤더와 동일한 좌우 기준: container-fluid + centered md-9 + px-2 -->
    <div class="container-fluid p-0">
      <div class="row justify-content-center g-0">
        <div class="col-12 col-md-9 px-2">

          <!-- ✅ 푸터 높이 안에서 세로 가운데 정렬 -->
          <div class="d-flex align-items-center" style="min-height: 140px;">
            <!-- 로고(왼쪽) + 텍스트(오른쪽) -->
            <div class="d-flex align-items-center gap-3 flex-wrap w-100">

              <!-- 왼쪽: 로고 -->
              <a href="/" class="d-flex align-items-center flex-shrink-0">
                <img
                  src="/assets/img/main/fit_me_logo.png"
                  alt="fit_me_logo"
                  style="width: 150px; height: auto;"
                >
              </a>

              <!-- 오른쪽: 텍스트 -->
              <div class="footer-contact" style="min-width: 220px;">
                <p class="text mb-1">서울시 동대문구 용두동 39-1</p>
                <p class="text mb-1">청량리역 한양수자인 그라시엘 3층 동대문 캠퍼스</p>
                <p class="text mb-2">사이트 종합 문의 : h321970921@gmail.com</p>
                <p class="sub-text mb-0">Fitme ⓒ 인재매칭 플랫폼. All Rights Reserved.</p>
              </div>

            </div>
          </div>

        </div>
      </div>
    </div>
  </footer>
`;

const scrollTopHtml = `
  <a href="#" id="scroll-top" class="scroll-top d-flex align-items-center justify-content-center">
    <i class="bi bi-arrow-up-short"></i>
  </a>
`;

export default function AppLayout() {
    const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
    const { pathname } = useLocation();
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [displayName, setDisplayName] = useState("");

    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                const res = await http.get("/api/auth/status");
                if (!alive) return;
                setIsAuthenticated(!!res?.data?.authenticated);
                setDisplayName(res?.data?.name || "");
            } catch (e) {
                if (!alive) return;
                setIsAuthenticated(false);
                setDisplayName("");
            }
        })();
        return () => {
            alive = false;
        };
    }, [pathname]);

    useEffect(() => {
        document.body.classList.remove("mobile-nav-active");

        const t = setTimeout(async () => {
            try {
                await loadScriptOnce("/assets/js/main.js");
                window.dispatchEvent(new Event("load"));
                window.dispatchEvent(new Event("scroll"));
            } catch (e) {
                console.error("Failed to load /assets/js/main.js", e);
            }
        }, 0);

        return () => clearTimeout(t);
    }, [pathname]);

    return (
        <>
            <HtmlPage html={headerHtml(isAuthenticated, displayName, apiBase)} />
            <Outlet />
            <HtmlPage html={footerHtml} />
            <HtmlPage html={scrollTopHtml} />
        </>
    );
}
