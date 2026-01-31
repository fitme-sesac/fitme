import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
    FileText,
    MessageSquare,
    Heart,
    Search,
    MoreHorizontal,
    Edit,
    Trash
} from "lucide-react";

import {
    COMPANY_MY_POSTS as MY_POSTS,
    COMPANY_MY_COMMENTS as MY_COMMENTS,
    COMPANY_LIKED_POSTS as LIKED_POSTS
} from "@/data/mockCommunityData";

export function CompanyCommunityManagement() {
    const [activeTab, setActiveTab] = useState("posts");
    const [searchQuery, setSearchQuery] = useState("");

    const MENU_Items = [
        { id: "posts", label: "작성한 글", icon: FileText, count: MY_POSTS.length },
        { id: "comments", label: "작성한 댓글", icon: MessageSquare, count: MY_COMMENTS.length },
        { id: "likes", label: "좋아요한 글", icon: Heart, count: LIKED_POSTS.length },
    ];

    const renderContent = () => {
        switch (activeTab) {
            case "posts":
                return (
                    <div className="space-y-4">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-xl font-bold">작성한 글 <span className="text-sky-600 ml-1">{MY_POSTS.length}</span></h2>
                            <div className="relative w-64">
                                <Input
                                    placeholder="제목, 내용 검색"
                                    className="pl-9 h-10 bg-white"
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                />
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                            </div>
                        </div>

                        {MY_POSTS.map(post => (
                            <div key={post.id} className="bg-white p-5 rounded-xl border hover:border-sky-500/50 hover:shadow-sm transition-all cursor-pointer group">
                                <div className="flex justify-between items-start mb-2">
                                    <Badge variant="secondary" className="bg-gray-100 text-gray-600 font-normal">
                                        {post.categoryLabel}
                                    </Badge>
                                    <div className="flex items-center gap-2">
                                        <span className="text-xs text-gray-400">{post.createdAt}</span>
                                        <button className="p-1 hover:bg-gray-100 rounded-full text-gray-400">
                                            <MoreHorizontal className="h-4 w-4" />
                                        </button>
                                    </div>
                                </div>
                                <h3 className="text-lg font-bold mb-1 group-hover:text-sky-600 transition-colors">{post.title}</h3>
                                <p className="text-sm text-gray-500 line-clamp-1 mb-4">{post.content}</p>

                                <div className="flex items-center gap-4 text-xs text-gray-400">
                                    <span className="flex items-center gap-1">
                                        <Heart className="h-3 w-3" /> {post.likes}
                                    </span>
                                    <span className="flex items-center gap-1">
                                        <MessageSquare className="h-3 w-3" /> {post.comments}
                                    </span>
                                    <span className="flex items-center gap-1">
                                        조회 {post.views}
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                );
            case "comments":
                return (
                    <div className="space-y-4">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-xl font-bold">작성한 댓글 <span className="text-sky-600 ml-1">{MY_COMMENTS.length}</span></h2>
                        </div>
                        {MY_COMMENTS.map(comment => (
                            <div key={comment.id} className="bg-white p-5 rounded-xl border hover:border-sky-500/50 transition-all">
                                <div className="text-sm font-medium text-gray-900 mb-2">
                                    원문: <span className="text-gray-500 hover:underline cursor-pointer">{comment.postTitle}</span>
                                </div>
                                <div className="bg-gray-50 p-3 rounded-lg text-sm text-gray-600 mb-2">
                                    {comment.content}
                                </div>
                                <div className="flex justify-between items-center text-xs text-gray-400">
                                    <span>{comment.createdAt}</span>
                                    <div className="flex gap-2">
                                        <button className="flex items-center gap-1 hover:text-sky-600">
                                            <Edit className="h-3 w-3" /> 수정
                                        </button>
                                        <button className="flex items-center gap-1 hover:text-red-500">
                                            <Trash className="h-3 w-3" /> 삭제
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                );
            case "likes":
                return (
                    <div className="space-y-4">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-xl font-bold">좋아요한 글 <span className="text-sky-600 ml-1">{LIKED_POSTS.length}</span></h2>
                        </div>
                        {LIKED_POSTS.map(post => (
                            <div key={post.id} className="bg-white p-4 rounded-xl border flex items-center justify-between hover:bg-gray-50 cursor-pointer">
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <Badge variant="outline" className="text-xs font-normal">
                                            {post.categoryLabel}
                                        </Badge>
                                        <span className="text-xs text-gray-400">{post.createdAt}</span>
                                    </div>
                                    <h3 className="font-bold text-gray-800">{post.title}</h3>
                                    <p className="text-xs text-gray-500 mt-1">Written by {post.author}</p>
                                </div>
                                <Heart className="h-5 w-5 text-red-500 fill-current" />
                            </div>
                        ))}
                    </div>
                );
            default:
                return null;
        }
    };

    return (
        <div className="bg-background rounded-2xl">
            <div className="flex flex-col lg:flex-row gap-8">
                {/* Sidebar Menu */}
                <div className="w-full lg:w-60 flex-shrink-0 space-y-6">
                    <div className="bg-white rounded-2xl border p-4 shadow-sm">
                        <div className="space-y-1">
                            {MENU_Items.map((item) => {
                                const isActive = activeTab === item.id;
                                const Icon = item.icon;
                                return (
                                    <button
                                        key={item.id}
                                        onClick={() => setActiveTab(item.id)}
                                        className={`w-full flex items-center justify-between px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200
                                            ${isActive
                                                ? "text-white shadow-md shadow-sky-500/20"
                                                : "text-gray-500 hover:bg-gray-50 hover:text-gray-900"
                                            }`}
                                        style={{
                                            background: isActive ? "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" : undefined
                                        }}
                                    >
                                        <div className="flex items-center gap-3">
                                            <Icon className={`h-4 w-4 ${isActive ? "text-white" : "text-gray-400"}`} />
                                            <span>{item.label}</span>
                                        </div>
                                        {item.count > 0 && (
                                            <span className={`text-xs ${isActive ? "text-white/80" : "text-gray-400"}`}>
                                                {item.count}
                                            </span>
                                        )}
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {/* Profile Summary Tiny Card */}
                    <div className="bg-gradient-to-br from-sky-500/10 to-teal-400/10 rounded-2xl p-5 border border-sky-500/10 text-center">
                        <h3 className="text-sm font-bold text-sky-600 mb-1">기업 커뮤니티 지수</h3>
                        <div className="text-3xl font-black text-gray-800 mb-2">Top 5%</div>
                        <p className="text-xs text-gray-500">
                            활발한 소통으로<br />기업 브랜딩이 강화되고 있어요!
                        </p>
                    </div>
                </div>

                {/* Content Area */}
                <div className="flex-1">
                    <div className="bg-white rounded-2xl border shadow-sm p-6 lg:p-8 min-h-full">
                        {renderContent()}
                    </div>
                </div>
            </div>
        </div>
    );
}
