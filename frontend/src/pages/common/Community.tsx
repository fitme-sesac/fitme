import { useState, useEffect, useCallback } from "react";
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
import { getPosts as fetchPosts, createPost as apiCreatePost } from "@/api/community";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";



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
    const [activeSort, setActiveSort] = useState<SortType>('latest');
    const [posts, setPosts] = useState<Post[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    // API에서 게시글 로드
    const loadPosts = useCallback(async (page = 0, append = false) => {
        setIsLoading(true);
        try {
            const category = activeCategory === 'all' ? '' : activeCategory;
            const response = await fetchPosts({ page, size: 10, category });
            const newPosts = response.content || [];

            // API 응답을 Post 타입에 맞게 변환
            const mappedPosts: Post[] = newPosts.map((p: any) => ({
                id: String(p.id),
                author: {
                    id: String(p.authorId || p.author?.id || ''),
                    name: p.authorName || p.author?.name || '익명',
                    title: p.authorTitle || p.author?.title || '',
                    company: p.authorCompany || p.author?.company,
                    avatar: p.authorAvatar || p.author?.avatar,
                },
                content: p.content,
                category: p.category as PostCategory,
                likes: p.likeCount || p.likes || 0,
                comments: p.commentCount || p.comments || 0,
                createdAt: p.createdAt,
                isLiked: p.isLiked || false,
            }));

            if (append) {
                setPosts(prev => [...prev, ...mappedPosts]);
            } else {
                setPosts(mappedPosts);
            }
            setHasMore(!response.last);
            setCurrentPage(page);
        } catch (error) {
            console.error('게시글 로드 실패:', error);
        } finally {
            setIsLoading(false);
        }
    }, [activeCategory]);

    // 카테고리 변경 시 다시 로드
    useEffect(() => {
        loadPosts(0);
    }, [loadPosts]);

    // 정렬 적용 (클라이언트 사이드)
    const sortedPosts = [...posts].sort((a, b) => {
        if (activeSort === 'latest') {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        }
        // AI 추천 (좋아요 기반)
        return b.likes - a.likes;
    });

    const handleNewPost = async (content: string, category: string) => {
        try {
            await apiCreatePost({ title: content.slice(0, 50), content, category });
            // 게시글 목록 새로고침 (새 글이 맨 위에 표시됨)
            await loadPosts(0);
        } catch (error) {
            console.error('게시글 작성 실패:', error);
        }
    };

    const handleRefresh = () => {
        loadPosts(0);
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
