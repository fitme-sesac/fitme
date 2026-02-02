import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Link } from "react-router-dom";
import {
    Heart,
    Users,
    Bell,
    ChevronRight,
    Flame,
    Loader2
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { CommunityManagement } from "@/components/mypage/CommunityManagement";
import { CompanyCommunityManagement } from "@/components/company/CompanyCommunityManagement";
import {
    getPopularPosts,
    getRecommendedMembers,
    getAnnouncements,
    getMyCommunityStats
} from "@/api/community";

interface PopularPost {
    id: string;
    title: string;
    author: string;
    likes: number;
}

interface RecommendedMember {
    id: string;
    name: string;
    title: string;
    company?: string;
    avatar?: string;
}

interface Announcement {
    id: string;
    title: string;
    date: string;
    isNew: boolean;
}

interface CommunityStats {
    postCount: number;
    commentCount: number;
    receivedLikes: number;
}

export function CommunitySidebar() {
    const { user, isCompany } = useAuth();
    const [showActivity, setShowActivity] = useState(false);

    // 데이터 상태
    const [popularPosts, setPopularPosts] = useState<PopularPost[]>([]);
    const [recommendedMembers, setRecommendedMembers] = useState<RecommendedMember[]>([]);
    const [announcements, setAnnouncements] = useState<Announcement[]>([]);
    const [myStats, setMyStats] = useState<CommunityStats>({ postCount: 0, commentCount: 0, receivedLikes: 0 });
    const [loading, setLoading] = useState(true);

    // 데이터 로드
    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            try {
                const [popular, members, notices] = await Promise.all([
                    getPopularPosts(5),
                    getRecommendedMembers(3),
                    getAnnouncements(3)
                ]);

                setPopularPosts(popular?.content || popular || []);
                setRecommendedMembers(members?.content || members || []);
                setAnnouncements(notices?.content || notices || []);

                // 로그인한 경우 내 통계도 가져옴
                if (user) {
                    const stats = await getMyCommunityStats();
                    setMyStats(stats || { postCount: 0, commentCount: 0, receivedLikes: 0 });
                }
            } catch (error) {
                console.error("Failed to fetch community sidebar data:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [user]);

    return (
        <div className="space-y-6">
            {/* User Profile Card (Logged in) or Popular Posts (Guest) */}
            {user ? (
                <>
                    <Card className="cursor-pointer hover:border-primary/50 transition-colors group" onClick={() => setShowActivity(true)}>
                        <CardHeader className="pb-3">
                            <CardTitle className="text-sm font-semibold flex items-center justify-between">
                                <span className="flex items-center gap-2">내가 활동한 내역</span>
                                <Badge variant={isCompany ? "default" : "secondary"} className={isCompany ? "bg-indigo-600 hover:bg-indigo-700" : ""}>
                                    {isCompany ? "기업 회원" : "개인 회원"}
                                </Badge>
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="pt-0">
                            <div className="flex items-center gap-3 mb-4">
                                <Avatar className="h-12 w-12 border">
                                    <AvatarImage src={user.user_metadata?.avatar_url} />
                                    <AvatarFallback className="bg-primary/10 text-primary font-bold">
                                        {(user.user_metadata?.display_name || user.name)?.charAt(0) || "U"}
                                    </AvatarFallback>
                                </Avatar>
                                <div>
                                    <p className="font-bold text-base group-hover:text-primary transition-colors">{user.user_metadata?.display_name || user.name || "사용자"}</p>
                                    <p className="text-xs text-muted-foreground">{user.user_metadata?.job_title || "FitMe 회원"}</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-2 text-center border-t border-border pt-4">
                                <div>
                                    <p className="text-xs text-muted-foreground">작성글</p>
                                    <p className="font-bold text-sm">{myStats.postCount}</p>
                                </div>
                                <div>
                                    <p className="text-xs text-muted-foreground">댓글</p>
                                    <p className="font-bold text-sm">{myStats.commentCount}</p>
                                </div>
                                <div>
                                    <p className="text-xs text-muted-foreground">받은 좋아요</p>
                                    <p className="font-bold text-sm">{myStats.receivedLikes}</p>
                                </div>
                            </div>
                            <div className="mt-4 text-center">
                                <p className="text-xs text-primary font-medium hover:underline">활동 내역 자세히 보기 →</p>
                            </div>
                        </CardContent>
                    </Card>

                    <Dialog open={showActivity} onOpenChange={setShowActivity}>
                        <DialogContent className="max-w-4xl h-[80vh] overflow-y-auto">
                            <DialogHeader>
                                <DialogTitle>나의 커뮤니티 활동</DialogTitle>
                            </DialogHeader>
                            <div className="mt-4">
                                {isCompany ? (
                                    <CompanyCommunityManagement />
                                ) : (
                                    <CommunityManagement onBack={() => setShowActivity(false)} />
                                )}
                            </div>
                        </DialogContent>
                    </Dialog>
                </>
            ) : (
                <Card>
                    <CardHeader className="pb-3">
                        <CardTitle className="text-sm font-semibold flex items-center gap-2">
                            <Flame className="h-4 w-4 text-sky-500" />
                            인기 게시물
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="pt-0">
                        {loading ? (
                            <div className="flex justify-center py-4">
                                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                            </div>
                        ) : popularPosts.length > 0 ? (
                            <div className="space-y-3">
                                {popularPosts.map((post, idx) => (
                                    <Link
                                        key={post.id}
                                        to={`/community/post/${post.id}`}
                                        className="flex items-start gap-3 group"
                                    >
                                        <span className={`text-sm font-bold ${idx < 3 ? "text-sky-600" : "text-muted-foreground"}`}>
                                            {idx + 1}
                                        </span>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-sm font-medium truncate group-hover:text-sky-600 transition-colors">
                                                {post.title}
                                            </p>
                                            <div className="flex items-center gap-2 text-xs text-muted-foreground mt-0.5">
                                                <span>{post.author}</span>
                                                <span className="flex items-center gap-1">
                                                    <Heart className="h-3 w-3" fill="currentColor" />
                                                    {post.likes}
                                                </span>
                                            </div>
                                        </div>
                                    </Link>
                                ))}
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground text-center py-4">
                                인기 게시물이 없습니다.
                            </p>
                        )}
                    </CardContent>
                </Card>
            )}

            {/* Recommended Members */}
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm font-semibold flex items-center gap-2">
                        <Users className="h-4 w-4 text-teal-500" />
                        추천 멤버
                    </CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                    {loading ? (
                        <div className="flex justify-center py-4">
                            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                        </div>
                    ) : recommendedMembers.length > 0 ? (
                        <div className="space-y-3">
                            {recommendedMembers.map((member) => (
                                <div key={member.id} className="flex items-center gap-3">
                                    <Avatar className="h-9 w-9">
                                        <AvatarImage src={member.avatar} />
                                        <AvatarFallback className="bg-gradient-to-br from-sky-500 to-teal-400 text-white text-xs font-bold">
                                            {member.name?.charAt(0)}
                                        </AvatarFallback>
                                    </Avatar>
                                    <div>
                                        <p className="text-sm font-medium">{member.name}</p>
                                        <p className="text-xs text-muted-foreground">{member.title}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-sm text-muted-foreground text-center py-4">
                            추천 멤버가 없습니다.
                        </p>
                    )}
                </CardContent>
            </Card>

            {/* Announcements */}
            <Card>
                <CardHeader className="pb-3">
                    <div className="flex items-center justify-between">
                        <CardTitle className="text-sm font-semibold flex items-center gap-2">
                            <Bell className="h-4 w-4 text-amber-500" />
                            공지사항
                        </CardTitle>
                        <Link to="/announcements" className="text-xs text-muted-foreground hover:text-sky-600 flex items-center">
                            더보기 <ChevronRight className="h-3 w-3" />
                        </Link>
                    </div>
                </CardHeader>
                <CardContent className="pt-0">
                    {loading ? (
                        <div className="flex justify-center py-4">
                            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                        </div>
                    ) : announcements.length > 0 ? (
                        <div className="space-y-3">
                            {announcements.map((notice) => (
                                <Link
                                    key={notice.id}
                                    to={`/announcements/${notice.id}`}
                                    className="block p-3 rounded-lg border border-border hover:bg-muted/50 transition-colors"
                                >
                                    <div className="flex items-center gap-2">
                                        <p className="text-sm font-medium flex-1 truncate">{notice.title}</p>
                                        {notice.isNew && (
                                            <Badge className="bg-sky-500 text-white text-[10px] px-1.5 py-0">NEW</Badge>
                                        )}
                                    </div>
                                    <p className="text-xs text-muted-foreground mt-1">{notice.date}</p>
                                </Link>
                            ))}
                        </div>
                    ) : (
                        <p className="text-sm text-muted-foreground text-center py-4">
                            공지사항이 없습니다.
                        </p>
                    )}
                </CardContent>
            </Card>

            {/* CTA Card - Hidden if logged in */}
            {!user && (
                <Card className="bg-gradient-primary text-white border-0">
                    <CardContent className="p-5 text-center">
                        <h3 className="font-bold mb-2">커뮤니티에 참여하세요!</h3>
                        <p className="text-sm text-white/90 mb-4">
                            업계 전문가들과 네트워킹하고 커리어 기회를 발견하세요.
                        </p>
                        <Button variant="secondary" size="sm" className="w-full bg-white text-sky-600 hover:bg-white/90 font-bold border-none">
                            프로필 완성하기
                        </Button>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}
