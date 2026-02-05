import { Outlet, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { http } from "../api/http";
import HtmlPage from "../components/HtmlPage";
import Header from "../components/Header";

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
    const [role, setRole] = useState("");

    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                const res = await http.get("/api/auth/status");
                if (!alive) return;
                setIsAuthenticated(!!res?.data?.authenticated);
                setDisplayName(res?.data?.name || "");
                setRole(res?.data?.role || "");
            } catch (e) {
                if (!alive) return;
                setIsAuthenticated(false);
                setDisplayName("");
                setRole("");
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
            <Header
                isAuthenticated={isAuthenticated}
                displayName={displayName}
                apiBase={apiBase}
                role={role}
            />
            <Outlet />
            <HtmlPage html={footerHtml} />
            <HtmlPage html={scrollTopHtml} />
        </>
    );
}
