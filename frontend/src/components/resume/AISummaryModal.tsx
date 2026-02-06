import React from "react";
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
import { Bot, AlertCircle } from "lucide-react";

interface AISummaryModalProps {
    isOpen: boolean;
    onClose: () => void;
    summaryText: string;
    onSummaryChange: (text: string) => void;
    onSave: () => void;
}

export const AISummaryModal: React.FC<AISummaryModalProps> = ({
    isOpen,
    onClose,
    summaryText,
    onSummaryChange,
    onSave,
}) => {
    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-5xl w-[90vw]">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Bot className="h-5 w-5 text-sky-500" />
                        AI 요약 (기업 제공용)
                    </DialogTitle>
                </DialogHeader>
                <div className="space-y-4 py-4">
                    <div className="bg-sky-50 border border-sky-200 rounded-lg p-3 text-sm text-sky-800 flex gap-2 items-start">
                        <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
                        <span>
                            <strong>안내:</strong> 이 내용은 기업이 인재 검색 시 확인하는 <strong>요약 정보</strong>입니다. 수정 시 원본 자기소개는 변경되지 않습니다.
                        </span>
                    </div>
                    <div className="space-y-2">
                        <Label>요약 내용</Label>
                        <Textarea
                            value={summaryText}
                            onChange={(e) => onSummaryChange(e.target.value)}
                            className="min-h-[600px] text-lg leading-relaxed font-normal p-6"
                            placeholder="AI 요약 결과가 없습니다."
                        />
                    </div>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>닫기</Button>
                    <Button onClick={onSave} className="bg-sky-500 hover:bg-sky-600 text-white">
                        저장하기
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};
