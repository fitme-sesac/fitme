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
    Flame
} from "lucide-react";

// Mock data for popular posts
const POPULAR_POSTS = [
    { id: "1", title: "개발자 커리어 성장을 위한 5가지 팁", likes: 128, author: "김개발" },
    { id: "2", title: "스타트업 vs 대기업, 어디가 좋을까?", likes: 95, author: "이창업" },
    { id: "3", title: "면접에서 자주 받는 질문 총정리", likes: 87, author: "박면접" },
    { id: "4", title: "2024 개발자 연봉 트렌드", likes: 76, author: "최연봉" },
    { id: "5", title: "주니어 개발자가 알아야 할 것들", likes: 64, author: "정주니" },
];

// Mock data for recommended members
const RECOMMENDED_MEMBERS = [
    { id: "1", name: "김개발", title: "Senior Developer", company: "테크컴퍼니", avatar: "" },
    { id: "2", name: "이스타트업", title: "Co-Founder", company: "AI스타트업", avatar: "" },
    { id: "3", name: "박리크루터", title: "HR Manager", company: "빅테크", avatar: "" },
];

// Mock data for announcements
const ANNOUNCEMENTS = [
    { id: "1", title: "FitMe 커뮤니티 이용 가이드", date: "2024.01.15", isNew: true },
    { id: "2", title: "개인정보 처리방침 개정 안내", date: "2024.01.10", isNew: false },
    { id: "3", title: "서비스 점검 안내 (1/20)", date: "2024.01.08", isNew: false },
];

export function CommunitySidebar() {
    return (
        <div className="space-y-6">
            {/* Popular Posts */}
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm font-semibold flex items-center gap-2">
                        <Flame className="h-4 w-4 text-orange-500" />
                        인기 게시물
                    </CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                    <div className="space-y-3">
                        {POPULAR_POSTS.map((post, idx) => (
                            <Link
                                key={post.id}
                                to={`/community/post/${post.id}`}
                                className="flex items-start gap-3 group"
                            >
                                <span className={`text-sm font-bold ${idx < 3 ? "text-[#5A639C]" : "text-muted-foreground"}`}>
                                    {idx + 1}
                                </span>
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium truncate group-hover:text-[#5A639C] transition-colors">
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
                </CardContent>
            </Card>

            {/* Recommended Members */}
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm font-semibold flex items-center gap-2">
                        <Users className="h-4 w-4 text-blue-500" />
                        추천 멤버
                    </CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                    <div className="space-y-3">
                        {RECOMMENDED_MEMBERS.map((member) => (
                            <div key={member.id} className="flex items-center gap-3">
                                <Avatar className="h-9 w-9">
                                    <AvatarImage src={member.avatar} />
                                    <AvatarFallback className="bg-gradient-to-br from-[#5A639C] to-[#9B86BD] text-white text-xs font-bold">
                                        {member.name.charAt(0)}
                                    </AvatarFallback>
                                </Avatar>
                                <div>
                                    <p className="text-sm font-medium">{member.name}</p>
                                    <p className="text-xs text-muted-foreground">{member.title}</p>
                                </div>
                            </div>
                        ))}
                    </div>
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
                        <Link to="/announcements" className="text-xs text-muted-foreground hover:text-[#5A639C] flex items-center">
                            더보기 <ChevronRight className="h-3 w-3" />
                        </Link>
                    </div>
                </CardHeader>
                <CardContent className="pt-0">
                    <div className="space-y-3">
                        {ANNOUNCEMENTS.map((notice) => (
                            <Link
                                key={notice.id}
                                to={`/announcements/${notice.id}`}
                                className="block p-3 rounded-lg border border-border hover:bg-muted/50 transition-colors"
                            >
                                <div className="flex items-center gap-2">
                                    <p className="text-sm font-medium flex-1 truncate">{notice.title}</p>
                                    {notice.isNew && (
                                        <Badge className="bg-red-500 text-white text-[10px] px-1.5 py-0">NEW</Badge>
                                    )}
                                </div>
                                <p className="text-xs text-muted-foreground mt-1">{notice.date}</p>
                            </Link>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* CTA Card */}
            <Card className="bg-gradient-to-br from-[#5A639C] to-[#9B86BD] text-white border-0">
                <CardContent className="p-5 text-center">
                    <h3 className="font-bold mb-2">커뮤니티에 참여하세요!</h3>
                    <p className="text-sm text-white/80 mb-4">
                        업계 전문가들과 네트워킹하고 커리어 기회를 발견하세요.
                    </p>
                    <Button variant="secondary" size="sm" className="w-full bg-white text-[#5A639C] hover:bg-white/90">
                        프로필 완성하기
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
