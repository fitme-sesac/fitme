import { useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/contexts/AuthContext";
import {
    Image,
    Link,
    FileText,
    Send,
    X
} from "lucide-react";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";

interface PostComposerProps {
    onSubmit?: (content: string, category: string) => void;
}

export function PostComposer({ onSubmit }: PostComposerProps) {
    const { user, profile } = useAuth();
    const [content, setContent] = useState("");
    const [category, setCategory] = useState("general");
    const [isFocused, setIsFocused] = useState(false);

    const handleSubmit = () => {
        if (content.trim()) {
            onSubmit?.(content, category);
            setContent("");
            setIsFocused(false);
        }
    };

    const displayName = profile?.display_name || user?.user_metadata?.display_name || "사용자";

    return (
        <Card className="overflow-hidden">
            <CardContent className="p-4">
                <div className="flex gap-3">
                    <Avatar className="h-10 w-10 shrink-0">
                        <AvatarImage src={user?.user_metadata?.avatar_url} />
                        <AvatarFallback className="bg-gradient-to-br from-[#5A639C] to-[#9B86BD] text-white font-bold">
                            {displayName.charAt(0)}
                        </AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                        <Textarea
                            placeholder="커뮤니티에 소식을 공유해보세요..."
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            onFocus={() => setIsFocused(true)}
                            className="min-h-[60px] resize-none border-0 p-0 focus-visible:ring-0 text-base placeholder:text-muted-foreground/60"
                            rows={isFocused ? 3 : 1}
                        />

                        {isFocused && (
                            <div className="flex items-center justify-between mt-4 pt-4 border-t border-border">
                                <div className="flex items-center gap-2">
                                    <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-[#5A639C]">
                                        <Image className="h-5 w-5" />
                                    </Button>
                                    <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-[#5A639C]">
                                        <Link className="h-5 w-5" />
                                    </Button>
                                    <Button variant="ghost" size="icon" className="h-9 w-9 text-muted-foreground hover:text-[#5A639C]">
                                        <FileText className="h-5 w-5" />
                                    </Button>

                                    <div className="h-6 w-px bg-border mx-2" />

                                    <Select value={category} onValueChange={setCategory}>
                                        <SelectTrigger className="w-[120px] h-9 text-sm">
                                            <SelectValue placeholder="카테고리" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="general">일반</SelectItem>
                                            <SelectItem value="company_news">기업소식</SelectItem>
                                            <SelectItem value="career_tips">커리어팁</SelectItem>
                                            <SelectItem value="qna">Q&A</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>

                                <div className="flex items-center gap-2">
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => {
                                            setContent("");
                                            setIsFocused(false);
                                        }}
                                        className="text-muted-foreground"
                                    >
                                        취소
                                    </Button>
                                    <Button
                                        size="sm"
                                        onClick={handleSubmit}
                                        disabled={!content.trim()}
                                        className="bg-gradient-to-r from-[#5A639C] to-[#9B86BD] hover:from-[#4A538C] hover:to-[#8B76AD] text-white gap-2"
                                    >
                                        <Send className="h-4 w-4" />
                                        게시
                                    </Button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
