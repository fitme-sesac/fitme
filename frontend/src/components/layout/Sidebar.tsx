import { Home, Briefcase, FileText, User, Settings, LogIn, LogOut, Target, HelpCircle, Bot, Users, Building2, CreditCard, MessageSquare, Calendar } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { Link, useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { AIChatWidget } from "@/components/chat/AIChatWidget";
import { EmployerAIChatWidget } from "@/components/chat/EmployerAIChatWidget";

// 구직자용 메뉴
const jobSeekerNavItems = [
  { icon: Home, label: "홈", href: "/" },
  { icon: Briefcase, label: "채용공고", href: "/jobs", badge: "NEW" },
  { icon: Calendar, label: "면접", href: "/interview" },
  { icon: FileText, label: "이력서", href: "/resume" },
  { icon: MessageSquare, label: "커뮤니티", href: "/community" },
  { icon: HelpCircle, label: "고객센터", href: "/support" },
  { icon: Bot, label: "AI", href: "#", action: "chat" }, // href changed to # and action added
];

// 기업용 메뉴
const companyNavItems = [
  { icon: Home, label: "홈", href: "/" },
  { icon: Briefcase, label: "채용공고", href: "/jobs" },
  { icon: Calendar, label: "면접", href: "/company/interviews" },
  { icon: Users, label: "인재풀", href: "/talents" },
  { icon: Building2, label: "기업", href: "/companies" },
  { icon: CreditCard, label: "구독", href: "/subscription" },
  { icon: MessageSquare, label: "커뮤니티", href: "/community" },
  { icon: HelpCircle, label: "고객센터", href: "/support" },
  { icon: Bot, label: "AI", href: "#", action: "chat" }, // href changed to # and action added
];

const bottomItems = [
  { icon: User, label: "마이페이지", href: "/mypage" },
  { icon: Settings, label: "설정", href: "/settings" },
];

export function Sidebar() {
  const { user, signOut, isCompany } = useAuth();
  const location = useLocation();
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [chatResetSeq, setChatResetSeq] = useState(0);

  useEffect(() => {
    setIsChatOpen(false);
    setChatResetSeq((v) => v + 1);
  }, [location.pathname]);

  // 기업 회원이면 기업용 메뉴, 아니면 구직자용 메뉴
  const navItems = isCompany ? companyNavItems : jobSeekerNavItems;

  // 기업 회원은 마이페이지 클릭 시 대시보드로 이동
  const getMypageHref = () => isCompany ? "/company/dashboard" : "/mypage";

  const handleItemClick = (e: React.MouseEvent, item: any) => {
    if (item.action === "chat") {
      e.preventDefault();
      setIsChatOpen(!isChatOpen);
    }
  };

  return (
    <>
      <aside className="fixed left-0 top-0 z-40 h-screen w-64 border-r border-sidebar-border bg-sidebar hidden lg:flex flex-col">
        {/* 로고 */}
        <div className="flex h-16 items-center gap-2 border-b border-sidebar-border px-6">
          <Link to="/" className="flex items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-sky-500 to-teal-400 shadow-sm">
              <Target className="h-6 w-6 text-white" strokeWidth={2.5} />
            </div>
            <span className="text-xl font-bold text-sky-600">FitMe</span>
          </Link>
        </div>

        {/* 메인 네비게이션 */}
        <nav className="flex-1 space-y-2 px-3 py-4">
          {navItems.map((item) => {
            const isActive = location.pathname === item.href || (item.action === "chat" && isChatOpen);
            const badge = 'badge' in item ? (item as { badge?: string }).badge : undefined;

            return (
              <Link
                key={item.label}
                to={item.href}
                onClick={(e) => handleItemClick(e, item)}
                className={cn(
                  "nav-link",
                  isActive && "nav-link-active"
                )}
              >
                <item.icon className="h-5 w-5" />
                <span className="flex-1">{item.label}</span>
                {badge && (
                  <span className="rounded-full bg-accent px-2 py-0.5 text-xs font-semibold text-accent-foreground">
                    {badge}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>

        {/* 하단 섹션 */}
        <div className="border-t border-sidebar-border px-3 py-4 space-y-2">
          {bottomItems.map((item) => {
            const href = item.label === "마이페이지" ? getMypageHref() : item.href;
            const isActive = location.pathname === href;
            return (
              <Link
                key={item.label}
                to={href}
                className={cn(
                  "nav-link",
                  isActive && "nav-link-active"
                )}
              >
                <item.icon className="h-5 w-5" />
                <span>{item.label}</span>
              </Link>
            );
          })}

          {/* 로그인/로그아웃 버튼 */}
          {user ? (
            <button
              onClick={signOut}
              className="mt-3 w-full flex items-center justify-center gap-2 rounded-xl px-4 py-3 border border-border text-muted-foreground hover:bg-secondary transition-colors"
            >
              <LogOut className="h-4 w-4" />
              <span>로그아웃</span>
            </button>
          ) : (
            <Link
              to="/auth"
              className="mt-4 w-full text-white shadow-md transition-all duration-300 flex items-center justify-center gap-2 rounded-xl px-4 py-3 font-bold border-0 hover:scale-[1.02]"
              style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}
            >
              <LogIn className="h-4 w-4" />
              <span>로그인</span>
            </Link>
          )}
        </div>

        {/* 저작권 */}
        <div className="border-t border-sidebar-border px-6 py-4">
          <p className="text-xs text-muted-foreground">
            © 2026 FitMe. All rights reserved.
          </p>
        </div>
      </aside>

      {/* AI Chat Widget - 구직자용 vs 기업용 */}
      {isCompany ? (
        <EmployerAIChatWidget
          isOpen={isChatOpen}
          onClose={() => setIsChatOpen(false)}
          resetSeq={chatResetSeq}
        />
      ) : (
        <AIChatWidget
          isOpen={isChatOpen}
          onClose={() => setIsChatOpen(false)}
          resetSeq={chatResetSeq}
        />
      )}
    </>
  );
}
