import { useState, useEffect } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { PostCard } from "@/components/community/PostCard";
import { PostComposer } from "@/components/community/PostComposer";
import { CommunitySidebar } from "@/components/community/CommunitySidebar";
import { CommunityNav } from "@/components/community/CommunityNav";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Loader2, RefreshCw } from "lucide-react";
import { useNavigate } from "react-router-dom";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";

// Mock posts data
const MOCK_POSTS = [
    {
        id: "1",
        author: {
            id: "user1",
            name: "김개발",
            title: "Senior Frontend Developer",
            company: "테크스타트업",
            avatar: "",
        },
        content: "개발자로 성장하면서 가장 중요했던 건 '왜'를 끊임없이 질문하는 것이었습니다.\n\n코드를 작성할 때 단순히 동작하는 것에 만족하지 않고, 왜 이 방식이 최선인지, 더 나은 방법은 없는지 고민하는 습관이 저를 성장시켰어요.\n\n주니어 개발자분들께 조금이나마 도움이 되었으면 합니다. 💪",
        category: "career_tips" as const,
        likes: 42,
        comments: 8,
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
    {
        id: "2",
        author: {
            id: "company1",
            name: "인공지능스타트업",
            title: "AI 혁신 기업",
            avatar: "",
        },
        content: "🎉 시리즈 A 투자 유치를 완료했습니다!\n\n저희 팀과 함께 AI의 미래를 만들어갈 인재를 찾고 있습니다. Frontend, Backend, ML Engineer 포지션 오픈했으니 많은 관심 부탁드립니다.",
        category: "company_news" as const,
        link: {
            url: "https://example.com/careers",
            title: "AI스타트업 채용 공고",
            description: "Frontend, Backend, ML Engineer 포지션 채용 중",
            image: "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=600",
        },
        likes: 128,
        comments: 23,
        createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
        isLiked: true,
    },
    {
        id: "3",
        author: {
            id: "user2",
            name: "이면접",
            title: "HR Manager",
            company: "빅테크",
        },
        content: "기술 면접에서 가장 많이 하는 실수 TOP 3\n\n1. 모르는 문제에 대해 아는 척하기\n→ 차라리 모른다고 솔직히 말하고, 어떻게 접근할지 설명하세요\n\n2. 코드만 작성하고 설명하지 않기\n→ 생각 과정을 말로 표현하세요\n\n3. 질문하지 않기\n→ 요구사항이 불명확하면 반드시 물어보세요\n\n이것만 기억해도 면접 결과가 달라집니다! 🎯",
        category: "career_tips" as const,
        likes: 256,
        comments: 45,
        createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
    {
        id: "4",
        author: {
            id: "user3",
            name: "박주니어",
            title: "Junior Developer",
            company: "스타트업",
        },
        content: "스타트업과 대기업 중에 어디가 더 성장하기 좋을까요? 🤔\n\n현재 2년차 개발자인데, 이직을 고민하고 있습니다. 대기업의 체계적인 교육 vs 스타트업의 다양한 경험... 선배님들의 조언 부탁드립니다!",
        category: "qna" as const,
        likes: 18,
        comments: 32,
        createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
    {
        id: "5",
        author: {
            id: "company2",
            name: "핀테크은행",
            title: "디지털 금융 혁신",
        },
        content: "📢 2024 개발자 채용 시작!\n\nSpring Boot, React 기반의 핀테크 서비스를 함께 만들어갈 분을 찾습니다.\n\n✅ 경력 3년 이상\n✅ 유연근무제\n✅ 스톡옵션 제공\n\n관심 있으시면 DM 주세요!",
        category: "company_news" as const,
        likes: 67,
        comments: 12,
        createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
];

type CategoryType = 'all' | 'general' | 'company_news' | 'career_tips' | 'qna';
type PostCategory = 'general' | 'company_news' | 'career_tips' | 'qna';

interface Post {
    id: string;
    author: {
        id: string;
        name: string;
        title: string;
        company?: string;
        avatar?: string;
    };
    content: string;
    category: PostCategory;
    link?: {
        url: string;
        title: string;
        description?: string;
        image?: string;
    };
    likes: number;
    comments: number;
    createdAt: string;
    isLiked: boolean;
    isBookmarked?: boolean;
}

type SortType = 'ai_recommend' | 'latest';

export default function Community() {
    const { user } = useAuth();
    const [activeCategory, setActiveCategory] = useState<CategoryType>('all');
    const [activeSort, setActiveSort] = useState<SortType>('ai_recommend');
    const [posts, setPosts] = useState<Post[]>(MOCK_POSTS);
    const [isLoading, setIsLoading] = useState(false);

    const filteredPosts = activeCategory === 'all'
        ? posts
        : posts.filter(post => post.category === activeCategory);

    // 정렬 적용
    const sortedPosts = [...filteredPosts].sort((a, b) => {
        if (activeSort === 'latest') {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        }
        // AI 추천 (좋아요 기반 mock)
        return b.likes - a.likes;
    });

    const handleNewPost = (content: string, category: string) => {
        const newPost = {
            id: `new-${Date.now()}`,
            author: {
                id: user?.id || "unknown",
                name: user?.user_metadata?.display_name || "사용자",
                title: "Member",
                avatar: user?.user_metadata?.avatar_url,
            },
            content,
            category: category as 'general' | 'company_news' | 'career_tips' | 'qna',
            likes: 0,
            comments: 0,
            createdAt: new Date().toISOString(),
            isLiked: false,
        };
        setPosts([newPost, ...posts]);
    };

    const handleRefresh = () => {
        setIsLoading(true);
        setTimeout(() => setIsLoading(false), 1000);
    };

    const navigate = useNavigate();

    // Login Prompt Logic
    const [showLoginPrompt, setShowLoginPrompt] = useState(false);

    useEffect(() => {
        if (!user) {
            const timer = setTimeout(() => {
                setShowLoginPrompt(true);
            }, 10000); // 10 seconds
            return () => clearTimeout(timer);
        }
    }, [user]);

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />

            <div className="lg:pl-64 transition-all duration-300">
                <Header />

                <main className="container max-w-7xl mx-auto py-6 px-4 md:px-8">
                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                        {/* Main Feed */}
                        <div className="lg:col-span-8">
                            {/* Category Navigation */}
                            <CommunityNav
                                activeCategory={activeCategory}
                                onCategoryChange={setActiveCategory}
                                activeSort={activeSort}
                                onSortChange={setActiveSort}
                            />

                            {/* Post Composer */}
                            {user && (
                                <div className="mb-6">
                                    <PostComposer onSubmit={handleNewPost} />
                                </div>
                            )}

                            {/* Refresh Button */}
                            <div className="flex justify-end mb-4">
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={handleRefresh}
                                    disabled={isLoading}
                                    className="text-muted-foreground hover:text-foreground"
                                >
                                    <RefreshCw className={`h-4 w-4 mr-2 ${isLoading ? "animate-spin" : ""}`} />
                                    새로고침
                                </Button>
                            </div>

                            {/* Posts Feed */}
                            <div className="space-y-4">
                                {isLoading ? (
                                    <div className="flex justify-center py-12">
                                        <Loader2 className="h-8 w-8 animate-spin text-sky-500" />
                                    </div>
                                ) : sortedPosts.length > 0 ? (
                                    sortedPosts.map((post) => (
                                        <PostCard key={post.id} post={post} />
                                    ))
                                ) : (
                                    <div className="text-center py-12 text-muted-foreground">
                                        이 카테고리에 게시물이 없습니다.
                                    </div>
                                )}
                            </div>

                            {/* Load More */}
                            {sortedPosts.length > 0 && (
                                <div className="flex justify-center mt-8">
                                    <Button variant="outline" className="text-sky-600 border-sky-200 hover:bg-sky-50 hover:text-sky-700">
                                        더 보기
                                    </Button>
                                </div>
                            )}
                        </div>

                        {/* Sidebar */}
                        <div className="hidden lg:block lg:col-span-4">
                            <div className="sticky top-20">
                                <CommunitySidebar />
                            </div>
                        </div>
                    </div>
                </main>

                <Footer />
            </div>

            {/* Login Prompt Dialog */}
            <Dialog open={showLoginPrompt} onOpenChange={setShowLoginPrompt}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>로그인이 필요합니다</DialogTitle>
                        <DialogDescription>
                            커뮤니티의 더 많은 기능을 이용하시려면 로그인이 필요합니다.<br />
                            3초 만에 로그인하고 다양한 정보를 확인해보세요!
                        </DialogDescription>
                    </DialogHeader>
                    <div className="flex justify-end gap-3 mt-4">
                        <Button variant="outline" onClick={() => setShowLoginPrompt(false)}>
                            구경하기
                        </Button>
                        <Button onClick={() => navigate("/auth")}>
                            로그인 하러가기
                        </Button>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
}
