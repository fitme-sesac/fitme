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

const headerHtml = (isAuthenticated, displayName, apiBase) => `<header id="header" class="header sticky-top" style="padding-bottom: 10px;">

        <!-- 1) 상단 줄: 주요사이트 바로가기 드롭다운 (변경 없음) -->
        <div class="container-fluid container-xl d-flex justify-content-end py-2 px-2">
            <div class="dropdown">
                <a href="#" class="btn btn-outline-success rounded-pill dropdown-toggle"
                   data-bs-toggle="dropdown" aria-expanded="false" style="font-size:0.8rem;">
                    주요사이트 바로가기
                </a>
                <ul class="dropdown-menu" style="font-size:0.8rem;">
                    <li><a class="dropdown-item" href="https://tigers.co.kr/" target="_blank">KIA타이거즈 바로가기↗</a></li>
                    <li><a class="dropdown-item" href="https://teamstore.tigers.co.kr/" target="_blank">KIA타이거즈 스토어 바로가기↗</a></li>
                    <li><a class="dropdown-item" href="https://www.ticketlink.co.kr/sports" target="_blank">티켓링크 바로가기↗</a></li>
                    <li><a class="dropdown-item" href="https://www.tving.com/sports/kbo" target="_blank">TVING 야구 중계 바로가기↗</a></li>
                    <li><a class="dropdown-item" href="https://www.msn.com/ko-kr/weather/forecast" target="_blank">일기예보 바로가기↗</a></li>
                </ul>
            </div>
        </div>

        <!-- 2) 하단 줄: 로고 · 메뉴 -->
        <div class="container-fluid container-xl d-flex align-items-center justify-content-between py-2 px-0">
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
                        <li><a href="/guide/info">경기일정</a></li>

                        <li class="dropdown">
                            <a href="#"><span>선수단</span><i class="bi bi-chevron-down toggle-dropdown"></i></a>
                            <ul>
                                <li><a href="/experience/list">선수정보</a></li>
                                <li><a href="/event/list">코치정보</a></li>
                                <li><a href="/event/list">감독정보</a></li>
                            </ul>
                        </li>

                        <li class="dropdown">
                            <a href="#"><span>응원문화</span><i class="bi bi-chevron-down toggle-dropdown"></i></a>
                            <ul>
                                <li><a href="/plantation/list">응원도구</a></li>
                                <li><a href="/document/list">응원가</a></li>
                            </ul>
                        </li>

                        <li><a href="/diagnose">역사관</a></li>
                        <li><a href="/board/list">게시판</a></li>
                        <li><a href="/qna/list">Q&amp;A</a></li>

                        <li class="dropdown">
                            <a href="#">
                                <span>${isAuthenticated && displayName ? `${displayName}님` : "방문객"}</span>
                                <i class="bi bi-chevron-down toggle-dropdown"></i>
                            </a>
                            <ul>
                                ${isAuthenticated
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
                     (준비중)
                </a>
            </div>
        </div>
    </header>`;

const footerHtml = `<footer id="footer" class="footer" style="background-color: #f0f0f0; padding-top: 0;">
        <div class="container footer-top" style="text-align: center;">
            <div class="row gy-4">
                <div class="col-lg-8 footer-about" style="display: inline-block; text-align: left;">
                    <a href="/" class="d-flex align-items-center">
                        <img src="/assets/img/main/fit_me_logo.png" alt="KIAFAN" style="width: 150px; height: auto;">
                    </a>
                    <div class="footer-contact pt-1">
                        <p class="text">경기도 부천시 부천로 245번길 44</p>
                        <p class="text">사이트 종합 문의 : rhtkdwls21@naver.com </p>
                        <p class="sub-text mb-3">Tigers ⓒ 기아팬 플랫폼. All Rights Reserved.</p>
                    </div>
                </div>
            </div>
        </div>
    </footer>`;

const scrollTopHtml = `<a href="#" id="scroll-top" class="scroll-top d-flex align-items-center justify-content-center"><i class="bi bi-arrow-up-short"></i></a>`;

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
        return () => { alive = false; };
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
