import { useState, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { useToast } from "@/hooks/use-toast";
import { Loader2, FileText, Upload } from "lucide-react";
import { Link } from "react-router-dom";

// Mock API function (replace with actual API later)
const fetchMyResumes = async () => {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 500));
    return [
        { id: 1, title: "기본 이력서 (경력 중심)", isDefault: true, updatedAt: "2024.01.20" },
        { id: 2, title: "프로젝트 포트폴리오 첨부", isDefault: false, updatedAt: "2023.12.15" },
    ];
};

const submitApplication = async (jobId, resumeId, message) => {
    await new Promise(resolve => setTimeout(resolve, 1500));
    return { ok: true };
};

export function ApplyModal({ isOpen, onClose, jobTitle, jobId }) {
    const [resumes, setResumes] = useState([]);
    const [selectedResumeId, setSelectedResumeId] = useState(null);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const { toast } = useToast();

    useEffect(() => {
        if (isOpen) {
            setIsLoading(true);
            fetchMyResumes()
                .then(data => {
                    setResumes(data);
                    const defaultResume = data.find(r => r.isDefault);
                    if (defaultResume) setSelectedResumeId(defaultResume.id);
                    else if (data.length > 0) setSelectedResumeId(data[0].id);
                })
                .catch(console.error)
                .finally(() => setIsLoading(false));
        }
    }, [isOpen]);

    const handleSubmit = async () => {
        if (!selectedResumeId) {
            toast({ title: "이력서 선택", description: "지원할 이력서를 선택해주세요.", variant: "destructive" });
            return;
        }

        setIsSubmitting(true);
        try {
            await submitApplication(jobId, selectedResumeId, message);
            toast({ title: "지원 완료", description: "성공적으로 지원했습니다. 좋은 결과 있기를 바랍니다!" });
            onClose();
        } catch (error) {
            toast({ title: "지원 실패", description: "지원을 처리하는 중 오류가 발생했습니다.", variant: "destructive" });
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Dialog open={isOpen} onOpenChange={onClose}>
            <DialogContent className="sm:max-w-[500px]">
                <DialogHeader>
                    <DialogTitle>지원하기</DialogTitle>
                    <DialogDescription className="line-clamp-1">
                        {jobTitle} 공고에 지원합니다.
                    </DialogDescription>
                </DialogHeader>

                <div className="py-4 space-y-6">
                    {/* Resume Selection */}
                    <div className="space-y-3">
                        <div className="flex items-center justify-between">
                            <Label className="text-base font-semibold">이력서 선택</Label>
                            <Link to="/resume" className="text-xs text-sky-600 hover:underline">
                                새 이력서 작성
                            </Link>
                        </div>

                        {isLoading ? (
                            <div className="flex justify-center py-4">
                                <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                            </div>
                        ) : resumes.length > 0 ? (
                            <RadioGroup value={String(selectedResumeId)} onValueChange={(v) => setSelectedResumeId(Number(v))} className="space-y-3">
                                {resumes.map((resume) => (
                                    <Label
                                        key={resume.id}
                                        htmlFor={`r-${resume.id}`}
                                        className={`flex items-start gap-3 p-3 rounded-xl border border-border cursor-pointer transition-all hover:bg-slate-50 ${selectedResumeId === resume.id ? "border-sky-500 bg-sky-50/50 ring-1 ring-sky-500" : ""}`}
                                    >
                                        <RadioGroupItem value={String(resume.id)} id={`r-${resume.id}`} className="mt-1" />
                                        <div className="flex-1">
                                            <div className="flex items-center gap-2 mb-1">
                                                <span className="font-semibold text-sm">{resume.title}</span>
                                                {resume.isDefault && (
                                                    <span className="bg-slate-100 text-slate-600 text-[10px] px-1.5 py-0.5 rounded font-medium">기본</span>
                                                )}
                                            </div>
                                            <div className="flex items-center gap-1 text-xs text-muted-foreground">
                                                <FileText className="h-3 w-3" />
                                                <span>최근 수정: {resume.updatedAt}</span>
                                            </div>
                                        </div>
                                    </Label>
                                ))}
                            </RadioGroup>
                        ) : (
                            <div className="text-center py-6 border border-dashed rounded-xl bg-slate-50">
                                <FileText className="h-8 w-8 text-slate-300 mx-auto mb-2" />
                                <p className="text-sm text-slate-500 mb-3">등록된 이력서가 없습니다.</p>
                                <Button asChild variant="outline" size="sm">
                                    <Link to="/resume">이력서 작성하러 가기</Link>
                                </Button>
                            </div>
                        )}
                    </div>

                    {/* Message (Optional) */}
                    <div className="space-y-2">
                        <Label htmlFor="message">전달 메시지 (선택)</Label>
                        <Textarea
                            id="message"
                            placeholder="채용 담당자에게 전달할 간단한 메시지가 있다면 입력해주세요."
                            className="resize-none h-24"
                            value={message}
                            onChange={(e) => setMessage(e.target.value)}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={onClose} disabled={isSubmitting}>취소</Button>
                    <Button
                        onClick={handleSubmit}
                        disabled={isSubmitting || resumes.length === 0}
                        className="bg-gradient-to-r from-sky-500 to-teal-400 text-white border-0"
                    >
                        {isSubmitting ? (
                            <>
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                지원 중...
                            </>
                        ) : (
                            <>
                                <Upload className="mr-2 h-4 w-4" />
                                지원하기
                            </>
                        )}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
