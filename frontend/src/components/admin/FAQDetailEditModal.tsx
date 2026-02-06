import { useState, useEffect } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { toast } from "sonner";
import { getFAQAdmin, updateFAQ } from "@/api/admin";

export interface FAQDetail {
    id: number;
    question: string;
    answer: string;
    isPublic: boolean;
    locked?: boolean;
    createdAt?: string;
    updatedAt?: string;
}

interface FAQDetailEditModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    faqId: number | null;
    onSuccess: () => void;
}

export const FAQDetailEditModal = ({
    open,
    onOpenChange,
    faqId,
    onSuccess,
}: FAQDetailEditModalProps) => {
    const [loading, setLoading] = useState(false);
    const [fetching, setFetching] = useState(false);
    const [question, setQuestion] = useState("");
    const [answer, setAnswer] = useState("");
    const [isPublic, setIsPublic] = useState(true);
    const [locked, setLocked] = useState(false);

    useEffect(() => {
        if (open && faqId != null) {
            setFetching(true);
            getFAQAdmin(faqId)
                .then((data: FAQDetail) => {
                    setQuestion(data.question ?? "");
                    setAnswer(data.answer ?? "");
                    setIsPublic(data.isPublic ?? true);
                    setLocked(data.locked ?? false);
                })
                .catch((err) => {
                    console.error(err);
                    toast.error("FAQ 내용을 불러오는데 실패했습니다.");
                    onOpenChange(false);
                })
                .finally(() => setFetching(false));
        }
    }, [open, faqId, onOpenChange]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (faqId == null) return;
        if (!question.trim() || !answer.trim()) {
            toast.error("질문과 답변을 모두 입력해주세요.");
            return;
        }

        setLoading(true);
        try {
            await updateFAQ(faqId, {
                question: question.trim(),
                answer: answer.trim(),
                isPublic,
                locked,
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
                    <DialogTitle>FAQ 상세 · 수정</DialogTitle>
                </DialogHeader>
                {fetching ? (
                    <div className="py-8 text-center text-muted-foreground">
                        로딩 중...
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label>질문</Label>
                            <Textarea
                                placeholder="질문을 입력하세요"
                                className="min-h-[80px]"
                                value={question}
                                onChange={(e) => setQuestion(e.target.value)}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label>답변</Label>
                            <Textarea
                                placeholder="답변을 입력하세요"
                                className="min-h-[160px]"
                                value={answer}
                                onChange={(e) => setAnswer(e.target.value)}
                            />
                        </div>

                        <div className="flex items-center justify-between">
                            <Label htmlFor="faq-isPublic">공개</Label>
                            <Switch
                                id="faq-isPublic"
                                checked={isPublic}
                                onCheckedChange={setIsPublic}
                            />
                        </div>

                        <div className="flex items-center justify-between">
                            <Label htmlFor="faq-locked">잠금</Label>
                            <Switch
                                id="faq-locked"
                                checked={locked}
                                onCheckedChange={setLocked}
                            />
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
