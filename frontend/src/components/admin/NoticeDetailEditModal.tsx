import { useState, useEffect } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { toast } from "sonner";
import { getNoticeAdmin, updateNotice } from "@/api/admin";

export interface NoticeDetail {
    id: number;
    title: string;
    body: string;
    isPublic: boolean;
    noticeType: string;
    status: string;
    createdAt?: string;
    updatedAt?: string;
}

interface NoticeDetailEditModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    noticeId: number | null;
    onSuccess: () => void;
}

const NOTICE_TYPE_OPTIONS = [
    { value: "OPS", label: "운영 공지" },
    { value: "TERMS", label: "이용약관" },
    { value: "PRIVACY", label: "개인정보 처리방침" },
    { value: "POLICY", label: "정책" },
];

const STATUS_OPTIONS = [
    { value: "ACTIVE", label: "활성" },
    { value: "PENDING_DELETE", label: "삭제 예정" },
    { value: "DELETED", label: "삭제됨" },
];

export const NoticeDetailEditModal = ({
    open,
    onOpenChange,
    noticeId,
    onSuccess,
}: NoticeDetailEditModalProps) => {
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(false);
    const [title, setTitle] = useState("");
    const [body, setBody] = useState("");
    const [noticeType, setNoticeType] = useState("OPS");
    const [status, setStatus] = useState("ACTIVE");
    const [isPublic, setIsPublic] = useState(true);

    useEffect(() => {
        if (open && noticeId != null) {
            setFetching(true);
            getNoticeAdmin(noticeId)
                .then((data: NoticeDetail) => {
                    setTitle(data.title ?? "");
                    setBody(data.body ?? "");
                    setNoticeType(data.noticeType ?? "OPS");
                    setStatus(data.status ?? "ACTIVE");
                    setIsPublic(data.isPublic ?? true);
                })
                .catch((err) => {
                    console.error(err);
                    toast.error("공지 내용을 불러오는데 실패했습니다.");
                    onOpenChange(false);
                })
                .finally(() => setFetching(false));
        }
    }, [open, noticeId, onOpenChange]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (noticeId == null) return;
        if (!title.trim() || !body.trim()) {
            toast.error("제목과 내용을 모두 입력해주세요.");
            return;
        }

        setLoading(true);
        try {
            await updateNotice(noticeId, {
                title: title.trim(),
                body: body.trim(),
                noticeType,
                isPublic,
                status,
            });
            toast.success("수정되었습니다.");
            onOpenChange(false);
            onSuccess();
        } catch (error) {
            console.error(error);
            toast.error("수정에 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[600px]">
                <DialogHeader>
                    <DialogTitle>공지사항/게시글 상세 · 수정</DialogTitle>
                </DialogHeader>
                {fetching ? (
                    <div className="py-8 text-center text-muted-foreground">
                        로딩 중...
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label>유형</Label>
                            <Select
                                value={noticeType}
                                onValueChange={setNoticeType}
                            >
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {NOTICE_TYPE_OPTIONS.map((opt) => (
                                        <SelectItem
                                            key={opt.value}
                                            value={opt.value}
                                        >
                                            {opt.label}
                                        </SelectItem>
                                    ))}
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

                        <div className="flex items-center justify-between">
                            <Label htmlFor="isPublic">공개</Label>
                            <Switch
                                id="isPublic"
                                checked={isPublic}
                                onCheckedChange={setIsPublic}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label>상태</Label>
                            <Select value={status} onValueChange={setStatus}>
                                <SelectTrigger>
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {STATUS_OPTIONS.map((opt) => (
                                        <SelectItem
                                            key={opt.value}
                                            value={opt.value}
                                        >
                                            {opt.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        <DialogFooter>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => onOpenChange(false)}
                            >
                                취소
                            </Button>
                            <Button type="submit" disabled={loading}>
                                {loading ? "저장 중..." : "저장"}
                            </Button>
                        </DialogFooter>
                    </form>
                )}
            </DialogContent>
        </Dialog>
    );
};
