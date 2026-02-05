import { useState } from "react";
import NotificationBell from "../features/notification/components/NotificationBell";
import "../features/notification/components/NotificationBell.css";
import "../features/notification/components/NotificationDropdown.css";

/**
 * 헤더 컴포넌트 - 알림 아이콘 포함
 */
export default function Header({ isAuthenticated, displayName, apiBase, role }) {
    const [mobileNavActive, setMobileNavActive] = useState(false);
    
    // 디버깅: role 값 확인
    console.log("[Header] role:", role, "isAuthenticated:", isAuthenticated);

    // 모바일 네비게이션 토글
    const toggleMobileNav = () => {
        setMobileNavActive(!mobileNavActive);
        document.body.classList.toggle("mobile-nav-active");
    };

    // 로그아웃 처리
    const handleLogout = (e) => {
        e.preventDefault();
        // 로그아웃 폼 제출
        const form = document.getElementById("logout-form");
        if (form) {
            form.submit();
        }
    };

    return (
        <header id="header" className="header sticky-top" style={{ paddingBottom: "10px" }}>
            <div className="container-fluid p-0">
                <div className="row justify-content-center g-0">
                    <div className="col-12 col-md-9 px-2">
                        <div className="d-flex align-items-center justify-content-between py-2">
                            {/* 로고 */}
                            <a 
                                href="/" 
                                className="logo d-flex align-items-center" 
                                style={{ marginRight: 0, paddingLeft: 0 }}
                            >
                                <img
                                    src="/assets/img/main/fit_me_logo.png"
                                    alt="로고"
                                    style={{ height: "50px", width: "auto", display: "block" }}
                                />
                            </a>

                            <div className="d-flex align-items-center">
                                <nav id="navmenu" className="navmenu me-3">
                                    <ul>
                                        <li><a href="/admin/user_info">관리/운영자</a></li>
                                        <li><a href="/jobs">채용공고</a></li>
                                        <li><a href="/diagnose">이력서 팁</a></li>
                                        <li><a href="/board/list">고객지원</a></li>

                                        <li className="dropdown">
                                            <a href="#">
                                                <span>
                                                    {isAuthenticated && displayName 
                                                        ? `${displayName}님` 
                                                        : "방문객"
                                                    }
                                                </span>
                                                <i className="bi bi-chevron-down toggle-dropdown"></i>
                                            </a>
                                            <ul>
                                                {isAuthenticated ? (
                                                    <>
                                                        <li>
                                                            <a className="dropdown-item" href="/User/Update">
                                                                회원수정
                                                            </a>
                                                        </li>
                                                        <li>
                                                            <a 
                                                                href="#" 
                                                                className="dropdown-item"
                                                                onClick={handleLogout}
                                                            >
                                                                로그아웃
                                                            </a>
                                                        </li>
                                                    </>
                                                ) : (
                                                    <>
                                                        <li>
                                                            <a href="/Login">로그인</a>
                                                        </li>
                                                        <li>
                                                            <a href="/User/Register">회원가입</a>
                                                        </li>
                                                    </>
                                                )}
                                            </ul>
                                        </li>
                                    </ul>
                                    <i 
                                        className="mobile-nav-toggle d-xl-none bi bi-list"
                                        onClick={toggleMobileNav}
                                    ></i>
                                </nav>

                                {/* 알림 아이콘 */}
                                <NotificationBell isAuthenticated={isAuthenticated} />

                                <a 
                                    className="btn-getstarted ms-2" 
                                    href={role?.toUpperCase() === "EMPLOYER" ? "/employer/dashboard" : "/MyPage"}
                                    onClick={(e) => {
                                        // 기업회원일 경우 프론트엔드 SPA로 이동
                                        if (role?.toUpperCase() === "EMPLOYER") {
                                            e.preventDefault();
                                            window.location.href = "/employer/dashboard";
                                        }
                                    }}
                                >
                                    마이페이지
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* 로그아웃 폼 (숨김) */}
            <form 
                id="logout-form"
                action={`${apiBase}/Logout`} 
                method="post" 
                style={{ display: "none" }}
            >
                <input type="hidden" />
            </form>
        </header>
    );
}
