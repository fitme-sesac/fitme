import { Link, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import { ChevronRight, Home, FileText, Send, Star, User } from "lucide-react";

const menuSections = [
    {
        title: "MY HOME",
        items: [
            { label: "개인회원 홈", href: "/mypage", icon: Home, exact: true },
        ],
    },
    {
        title: "이력서 관리",
        items: [
            { label: "이력서 등록", href: "/resume/register", icon: FileText },
            { label: "이력서 관리", href: "/resume", icon: FileText },
        ],
    },
    {
        title: "입사지원·제안 관리",
        items: [
            { label: "입사지원 현황", href: "/applications", icon: Send },
            { label: "받은 포지션 제안", href: "/proposals", icon: Send },
        ],
    },
    {
        title: "AI추천",
        items: [
            { label: "새로운 추천공고", href: "/recommendations/new", icon: Star },
            { label: "확인한 추천공고", href: "/recommendations/viewed", icon: Star },
        ],
    },
];

export function JobSeekerSidebar() {
    const location = useLocation();

    return (
        <div className="space-y-8">
            {menuSections.map((section, idx) => (
                <div key={idx}>
                    <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-wider mb-3 px-4">
                        {section.title}
                    </h3>
                    <div className="space-y-1">
                        {section.items.map((item, itemIdx) => {
                            const isActive = item.exact
                                ? location.pathname === item.href
                                : location.pathname.startsWith(item.href);

                            const Icon = item.icon;

                            return (
                                <Link
                                    key={itemIdx}
                                    to={item.href}
                                    className={cn(
                                        "w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200",
                                        isActive
                                            ? "bg-[#5A639C] text-white shadow-md shadow-[#5A639C]/20"
                                            : "text-muted-foreground hover:bg-muted hover:text-foreground"
                                    )}
                                >
                                    <div className="flex items-center gap-3">
                                        {Icon && <Icon className={cn("h-4 w-4", !isActive && "text-muted-foreground")} />}
                                        <span>{item.label}</span>
                                    </div>
                                    {isActive && <ChevronRight className="h-4 w-4 text-white/80" />}
                                </Link>
                            );
                        })}
                    </div>
                </div>
            ))}
        </div>
    );
}
