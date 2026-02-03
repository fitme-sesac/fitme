import { Link, useLocation } from "react-router-dom";
import {
    LayoutDashboard,
    Users,
    Briefcase,
    Building2,
    MessageSquare,
    Flag,
    HelpCircle,
    Shield,
    LogOut,
    CreditCard
} from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
    { icon: LayoutDashboard, label: "대시보드", href: "/admin" },
    { icon: Users, label: "회원 관리", href: "/admin/members" },
    { icon: Briefcase, label: "채용공고 관리", href: "/admin/jobs" },
    { icon: Building2, label: "기업 관리", href: "/admin/companies" },
    { icon: MessageSquare, label: "커뮤니티 관리", href: "/admin/community" },
    { icon: Flag, label: "신고 관리", href: "/admin/reports" },
    { icon: HelpCircle, label: "문의 관리", href: "/admin/inquiries" },
    { icon: CreditCard, label: "구독 관리", href: "/admin/subscriptions" },
];

export const AdminSidebar = () => {
    const location = useLocation();

    return (
        <aside className="fixed inset-y-0 left-0 z-20 w-64 bg-card border-r border-border hidden lg:block">
            {/* Logo */}
            <div className="flex items-center gap-3 px-6 py-5 border-b border-border">
                <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center">
                    <Shield className="w-5 h-5 text-primary-foreground" />
                </div>
                <div>
                    <h1 className="font-bold text-lg">FitMe Admin</h1>
                    <p className="text-xs text-muted-foreground">관리자 패널</p>
                </div>
            </div>

            {/* Navigation */}
            <nav className="p-4 space-y-1">
                {navItems.map((item) => {
                    const isActive = location.pathname === item.href;
                    return (
                        <Link
                            key={item.href}
                            to={item.href}
                            className={cn(
                                "flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors",
                                isActive
                                    ? "bg-primary text-primary-foreground"
                                    : "text-muted-foreground hover:text-foreground hover:bg-muted"
                            )}
                        >
                            <item.icon className="w-5 h-5" />
                            {item.label}
                        </Link>
                    );
                })}
            </nav>

            {/* Bottom */}
            <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-border">
                <Link
                    to="/"
                    className="flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                >
                    <LogOut className="w-5 h-5" />
                    사이트로 돌아가기
                </Link>
            </div>
        </aside>
    );
};

export default AdminSidebar;
