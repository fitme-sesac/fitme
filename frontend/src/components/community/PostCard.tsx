import { useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
    Heart,
    MessageCircle,
    Share2,
    MoreHorizontal,
    ExternalLink,
    Bookmark
} from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface PostAuthor {
    id: string;
    name: string;
    avatar?: string;
    title: string;
    company?: string;
}

interface PostLink {
    url: string;
    title: string;
    image?: string;
    description?: string;
}

interface Post {
    id: string;
    author: PostAuthor;
    content: string;
    category: 'general' | 'company_news' | 'career_tips' | 'qna';
    link?: PostLink;
    likes: number;
    comments: number;
    createdAt: string;
    isLiked: boolean;
    isBookmarked?: boolean;
}

const CATEGORY_LABELS: Record<string, { label: string; color: string }> = {
    general: { label: "일반", color: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300" },
    company_news: { label: "기업소식", color: "bg-sky-100 text-sky-700 dark:bg-sky-900/30 dark:text-sky-300" },
    career_tips: { label: "커리어팁", color: "bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-300" },
    qna: { label: "Q&A", color: "bg-indigo-100 text-indigo-700 dark:bg-indigo-900/30 dark:text-indigo-300" },
};

function formatTimeAgo(dateString: string): string {
    const now = new Date();
    const date = new Date(dateString);
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diff < 60) return "방금 전";
    if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
    if (diff < 604800) return `${Math.floor(diff / 86400)}일 전`;
    return date.toLocaleDateString("ko-KR");
}

interface PostCardProps {
    post: Post;
    onLike?: (postId: string) => void;
    onComment?: (postId: string) => void;
    onShare?: (postId: string) => void;
    onBookmark?: (postId: string) => void;
}

export function PostCard({ post, onLike, onComment, onShare, onBookmark }: PostCardProps) {
    const [isLiked, setIsLiked] = useState(post.isLiked);
    const [likes, setLikes] = useState(post.likes);
    const [isBookmarked, setIsBookmarked] = useState(post.isBookmarked);

    const handleLike = () => {
        setIsLiked(!isLiked);
        setLikes(isLiked ? likes - 1 : likes + 1);
        onLike?.(post.id);
    };

    const handleBookmark = () => {
        setIsBookmarked(!isBookmarked);
        onBookmark?.(post.id);
    };

    const categoryInfo = CATEGORY_LABELS[post.category] || CATEGORY_LABELS.general;

    return (
        <Card className="overflow-hidden hover:shadow-md transition-shadow">
            <CardContent className="p-5">
                {/* Header: Author Info */}
                <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                        <Avatar className="h-12 w-12">
                            <AvatarImage src={post.author.avatar} />
                            <AvatarFallback className="bg-gradient-to-br from-sky-500 to-teal-400 text-white font-bold">
                                {post.author.name.charAt(0)}
                            </AvatarFallback>
                        </Avatar>
                        <div>
                            <div className="flex items-center gap-2">
                                <span className="font-semibold text-foreground">{post.author.name}</span>
                                {post.author.company && (
                                    <span className="text-sm text-muted-foreground">@ {post.author.company}</span>
                                )}
                            </div>
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                <span>{post.author.title}</span>
                                <span>·</span>
                                <span>{formatTimeAgo(post.createdAt)}</span>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <Badge className={`${categoryInfo.color} border-0 text-xs`}>
                            {categoryInfo.label}
                        </Badge>
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="h-8 w-8">
                                    <MoreHorizontal className="h-4 w-4" />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                <DropdownMenuItem>신고하기</DropdownMenuItem>
                                <DropdownMenuItem>숨기기</DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>

                {/* Content */}
                <div className="mb-4">
                    <p className="text-foreground whitespace-pre-wrap leading-relaxed">
                        {post.content}
                    </p>
                </div>

                {/* Link Preview */}
                {post.link && (
                    <a
                        href={post.link.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="block mb-4 rounded-xl border border-border overflow-hidden hover:bg-muted/50 transition-colors"
                    >
                        {post.link.image && (
                            <div className="h-48 bg-muted overflow-hidden">
                                <img
                                    src={post.link.image}
                                    alt={post.link.title}
                                    className="w-full h-full object-cover"
                                />
                            </div>
                        )}
                        <div className="p-4">
                            <div className="flex items-center gap-2 text-xs text-muted-foreground mb-1">
                                <ExternalLink className="h-3 w-3" />
                                <span className="text-sky-600">{new URL(post.link.url).hostname}</span>
                            </div>
                            <h4 className="font-medium text-foreground line-clamp-1">{post.link.title}</h4>
                            {post.link.description && (
                                <p className="text-sm text-muted-foreground line-clamp-2 mt-1">
                                    {post.link.description}
                                </p>
                            )}
                        </div>
                    </a>
                )}

                {/* Actions */}
                <div className="flex items-center justify-between pt-3 border-t border-border">
                    <div className="flex items-center gap-1">
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={handleLike}
                            className={`gap-2 ${isLiked ? "text-red-500 hover:text-red-600" : "text-muted-foreground hover:text-foreground"}`}
                        >
                            <Heart className={`h-4 w-4 ${isLiked ? "fill-current" : ""}`} />
                            <span>{likes > 0 ? likes : "좋아요"}</span>
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => onComment?.(post.id)}
                            className="gap-2 text-muted-foreground hover:text-foreground"
                        >
                            <MessageCircle className="h-4 w-4" />
                            <span>{post.comments > 0 ? post.comments : "댓글"}</span>
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => onShare?.(post.id)}
                            className="gap-2 text-muted-foreground hover:text-foreground"
                        >
                            <Share2 className="h-4 w-4" />
                            <span>공유</span>
                        </Button>
                    </div>
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={handleBookmark}
                        className={`h-8 w-8 ${isBookmarked ? "text-sky-500" : "text-muted-foreground hover:text-foreground"}`}
                    >
                        <Bookmark className={`h-4 w-4 ${isBookmarked ? "fill-current" : ""}`} />
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}
