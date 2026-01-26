import { Home, Briefcase, Users, GraduationCap, MessageSquare, Building2, User, Settings, LogIn, LogOut, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { Link, useLocation } from "react-router-dom";

const navItems = [
  { icon: Home, label: "홈", href: "/" },
  { icon: Briefcase, label: "채용", href: "/jobs", badge: "NEW" },
  { icon: Users, label: "인재풀", href: "/talents" },
  { icon: Building2, label: "기업", href: "/companies" },
  { icon: GraduationCap, label: "교육", href: "/education" },
  { icon: MessageSquare, label: "메시지", href: "/messages" },
];

const bottomItems = [
  { icon: User, label: "마이페이지", href: "/mypage" },
  { icon: Settings, label: "설정", href: "/settings" },
];

export function Sidebar() {
  const { user, signOut } = useAuth();
  const location = useLocation();

  return (
    <aside className="fixed left-0 top-0 z-40 h-screen w-64 border-r border-sidebar-border bg-sidebar hidden lg:flex flex-col">
      {/* 로고 */}
      <div className="flex h-16 items-center gap-2 border-b border-sidebar-border px-6">
        <Link to="/" className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary">
            <Sparkles className="h-5 w-5 text-primary-foreground" />
          </div>
          <span className="text-xl font-bold text-foreground">FitMe</span>
        </Link>
      </div>

      {/* 메인 네비게이션 */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {navItems.map((item) => {
          const isActive = location.pathname === item.href;
          return (
            <Link
              key={item.label}
              to={item.href}
              className={cn(
                "nav-link",
                isActive && "nav-link-active"
              )}
            >
              <item.icon className="h-5 w-5" />
              <span className="flex-1">{item.label}</span>
              {item.badge && (
                <span className="rounded-full bg-accent px-2 py-0.5 text-xs font-semibold text-accent-foreground">
                  {item.badge}
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      {/* 하단 섹션 */}
      <div className="border-t border-sidebar-border px-3 py-4">
        {bottomItems.map((item) => {
          const isActive = location.pathname === item.href;
          return (
            <Link
              key={item.label}
              to={item.href}
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
            to="/Login"
            className="mt-3 w-full btn-gradient-primary flex items-center justify-center gap-2 rounded-xl px-4 py-3"
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
  );
}
