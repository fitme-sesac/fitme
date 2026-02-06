import React from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Bot } from "lucide-react";

interface PrimaryResumeConfirmModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
}

export const PrimaryResumeConfirmModal: React.FC<PrimaryResumeConfirmModalProps> = ({
    isOpen,
    onClose,
    onConfirm,
}) => {
    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 text-sky-600">
                        <Bot className="h-5 w-5" />
                        나에게 꼭 맞는 채용 추천 받기
                    </DialogTitle>
                </DialogHeader>
                <div className="py-4 text-sm text-gray-600 space-y-4">
                    <p className="text-base font-medium text-gray-900">
                        이 이력서를 <span className="text-sky-600">대표 이력서</span>로 설정하시겠습니까?
                    </p>
                    <div className="bg-sky-50 border border-sky-100 rounded-xl p-4 text-sky-800 leading-relaxed">
                        <strong>💡 더 나은 매칭을 위한 안내</strong><br />
                        대표 이력서로 설정하시면, AI가 회원님의 이력서를 꼼꼼히 분석하여 **가장 잘 어울리는 공고를 우선적으로 매칭**해 드립니다.
                        이 과정에서 생성된 분석 데이터는 오직 회원님만을 위한 맞춤형 서비스 제공에만 소중히 활용됩니다.
                    </div>
                    <p>
                        동의하시면 설정을 완료하고 스마트한 채용 추천을 받아보세요!
                    </p>
                </div>
                <DialogFooter className="gap-2 sm:gap-0">
                    <Button variant="ghost" onClick={onClose} className="flex-1 sm:flex-none">다음에 하기</Button>
                    <Button onClick={onConfirm} className="bg-sky-500 hover:bg-sky-600 text-white flex-1 sm:flex-none shadow-sm">
                        네, 설정할게요
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};
