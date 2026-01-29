import { Button } from "@/components/ui/button";
import {
    LayoutGrid,
    Building2,
    Lightbulb,
    HelpCircle,
    Sparkles,
    Clock,
    ChevronDown
} from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type CategoryType = 'all' | 'general' | 'company_news' | 'career_tips' | 'qna';
type SortType = 'ai_recommend' | 'latest';

interface CommunityNavProps {
    activeCategory: CategoryType;
    onCategoryChange: (category: CategoryType) => void;
    activeSort?: SortType;
    onSortChange?: (sort: SortType) => void;
}

const CATEGORIES = [
    { id: 'all', label: '전체', icon: LayoutGrid },
    { id: 'company_news', label: '기업소식', icon: Building2 },
    { id: 'career_tips', label: '커리어팁', icon: Lightbulb },
    { id: 'qna', label: 'Q&A', icon: HelpCircle },
] as const;

const SORT_OPTIONS = [
    { id: 'ai_recommend', label: 'AI 추천', icon: Sparkles },
    { id: 'latest', label: '최신순', icon: Clock },
] as const;

export function CommunityNav({ activeCategory, onCategoryChange, activeSort = 'ai_recommend', onSortChange }: CommunityNavProps) {
    const currentSort = SORT_OPTIONS.find(s => s.id === activeSort) || SORT_OPTIONS[0];
    const SortIcon = currentSort.icon;

    return (
        <div className="bg-card rounded-xl border border-border p-2 mb-6">
            <div className="flex items-center gap-1 overflow-x-auto scrollbar-hide">
                {CATEGORIES.map((category) => {
                    const Icon = category.icon;
                    const isActive = activeCategory === category.id;

                    return (
                        <Button
                            key={category.id}
                            variant={isActive ? "default" : "ghost"}
                            size="sm"
                            onClick={() => onCategoryChange(category.id)}
                            className={`
                                flex items-center gap-2 rounded-lg px-4 py-2 shrink-0
                                ${isActive
                                    ? "bg-gradient-to-r from-[#5A639C] to-[#9B86BD] text-white hover:from-[#4A538C] hover:to-[#8B76AD]"
                                    : "text-muted-foreground hover:text-foreground hover:bg-muted"
                                }
                            `}
                        >
                            <Icon className="h-4 w-4" />
                            {category.label}
                        </Button>
                    );
                })}

                {/* 정렬 드롭다운 */}
                <div className="ml-auto">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                variant="outline"
                                size="sm"
                                className="flex items-center gap-2 border-[#5A639C]/30 text-[#5A639C] hover:bg-[#5A639C]/10"
                            >
                                <SortIcon className="h-4 w-4" />
                                {currentSort.label}
                                <ChevronDown className="h-3 w-3" />
                            </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                            {SORT_OPTIONS.map((option) => {
                                const OptionIcon = option.icon;
                                return (
                                    <DropdownMenuItem
                                        key={option.id}
                                        onClick={() => onSortChange?.(option.id)}
                                        className={`flex items-center gap-2 ${activeSort === option.id ? "bg-[#5A639C]/10 text-[#5A639C]" : ""}`}
                                    >
                                        <OptionIcon className="h-4 w-4" />
                                        {option.label}
                                    </DropdownMenuItem>
                                );
                            })}
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>
        </div>
    );
}
