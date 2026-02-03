import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toast } from "sonner";
import { createNotice } from "@/api/admin";

interface CreateNoticeModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onSuccess: () => void;
}

export const CreateNoticeModal = ({ open, onOpenChange, onSuccess }: CreateNoticeModalProps) => {
    const [loading, setLoading] = useState(false);
    const [title, setTitle] = useState("");
    const [body, setBody] = useState("");
    const [type, setType] = useState("COMMUNITY"); // 기본값 COMMUNITY

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim() || !body.trim()) {
            toast.error("제목과 내용을 모두 입력해주세요.");
            return;
        }

        setLoading(true);
        try {
            await createNotice({
                title,
                body,
                noticeType: type,
                isPublic: true,
                status: "ACTIVE"
            });
            toast.success("공지사항이 등록되었습니다.");
            onOpenChange(false);
            setTitle("");
            setBody("");
            setType("COMMUNITY");
            onSuccess();
        } catch (error) {
            console.error(error);
            toast.error("공지사항 등록에 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader>
                    <DialogTitle>공지사항/게시글 작성</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label>유형</Label>
                        <Select value={type} onValueChange={setType}>
                            <SelectTrigger>
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="COMMUNITY">커뮤니티 게시글</SelectItem>
                                <SelectItem value="OPS">운영 공지</SelectItem>
                                <SelectItem value="TERMS">이용약관</SelectItem>
                                <SelectItem value="PRIVACY">개인정보 처리방침</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="space-y-2">
                        <Label>제목</Label>
                        <Input
                            placeholder="제목을 입력하세요"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label>내용</Label>
                        <Textarea
                            placeholder="내용을 입력하세요"
                            className="min-h-[200px]"
                            value={body}
                            onChange={(e) => setBody(e.target.value)}
                        />
                    </div>

                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>cancel</Button>
                        <Button type="submit" disabled={loading}>
                            {loading ? "작성 중..." : "작성 완료"}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
};
