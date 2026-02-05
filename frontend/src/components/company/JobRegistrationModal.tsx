import { useState, useRef, useEffect } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { X, Plus, Upload, Loader2 } from "lucide-react";
import { createJobPosting, getJobPosting, updateJobPosting } from "@/features/job/api/jobApi";
import { http } from "@/api/http";

interface JobRegistrationModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    /** 등록/수정 성공 시 호출 (목록 새로고침 등) */
    onSuccess?: () => void;
    /** 수정 모드: 채용공고 jobUid 지정 시 해당 공고를 불러와 수정 */
    editJobUid?: string | null;
}

const POSITIONS = [
    { value: "frontend", label: "프론트엔드 개발자" },
    { value: "backend", label: "백엔드 개발자" },
    { value: "fullstack", label: "풀스택 개발자" },
    { value: "mobile_ios", label: "iOS 개발자" },
    { value: "mobile_android", label: "Android 개발자" },
    { value: "devops", label: "DevOps / 인프라" },
    { value: "data_engineer", label: "데이터 엔지니어" },
    { value: "data_scientist", label: "데이터 사이언티스트" },
    { value: "ai_ml", label: "AI / 머신러닝" },
    { value: "security", label: "정보보안" },
    { value: "qa", label: "QA / 테스트 엔지니어" },
    { value: "pm", label: "서비스 기획 / PM" },
    { value: "designer", label: "UI/UX 디자이너" },
];

const LOCATIONS = [
    "서울 강남구", "서울 서초구", "서울 송파구", "서울 구로구", "서울 금천구", "서울 마포구", "서울 성동구",
    "경기 성남시 분당구 (판교)", "경기 성남시 수정구", "인천", "대전", "대구", "부산", "광주", "재택근무",
    "부산광역시 수영구", "부산광역시 해운대구", "경기", "세종", "제주",
];

const COMMON_STACKS = ["React", "Vue.js", "Next.js", "TypeScript", "Node.js", "Java", "Spring Boot", "Python", "Django", "FastAPI", "Go", "AWS", "Docker", "Kubernetes", "Flutter", "Swift", "Kotlin"];

export function JobRegistrationModal({ open, onOpenChange, onSuccess, editJobUid }: JobRegistrationModalProps) {
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [submitting, setSubmitting] = useState(false);
    const [loadingJob, setLoadingJob] = useState(false);
    const [uploadingImages, setUploadingImages] = useState(false);
    const [formData, setFormData] = useState({
        title: "",
        position: "",
        location: "",
        experience: "",
        salary: "",
        deadline: "",
        description: "",
        techStack: [] as string[],
        techInput: "",
        recruitmentCapacity: "",
        images: [] as File[]
    });
    const [previewUrls, setPreviewUrls] = useState<string[]>([]);
    const [uploadedImageUrls, setUploadedImageUrls] = useState<string[]>([]);

    const isEditMode = !!editJobUid;

    // 등록 모드로 열릴 때 폼 초기화
    useEffect(() => {
        if (open && !editJobUid) {
            setFormData({
                title: "", position: "", location: "", experience: "", salary: "", deadline: "",
                description: "", techStack: [], techInput: "", recruitmentCapacity: "", images: [],
            });
            setPreviewUrls([]);
        }
    }, [open, editJobUid]);

    // 수정 모드: 공고 데이터 로드
    useEffect(() => {
        if (!open || !editJobUid) return;
        let cancelled = false;
        setLoadingJob(true);
        getJobPosting(editJobUid)
            .then((job: Record<string, unknown>) => {
                if (cancelled) return;
                const stackStr = (job.stack as string) ?? "";
                const techStack = stackStr ? stackStr.split(",").map((s: string) => s.trim()).filter(Boolean) : [];
                const loc = (job.location as string) ?? "";
                const locOptions = LOCATIONS.includes(loc) ? LOCATIONS : [loc, ...LOCATIONS];
                setFormData({
                    title: (job.title as string) ?? "",
                    position: (job.position as string) ?? "",
                    location: loc,
                    experience: String((job.requiredExperience as number) ?? 0),
                    salary: (job.salaryText as string) ?? "",
                    deadline: "",
                    description: (job.description as string) ?? "",
                    techStack,
                    techInput: "",
                    recruitmentCapacity: String((job.recruitmentCapacity as number) ?? ""),
                    images: [],
                });
                setPreviewUrls([]);
            })
            .catch(() => {
                if (!cancelled) alert("채용공고를 불러오는데 실패했습니다.");
            })
            .finally(() => {
                if (!cancelled) setLoadingJob(false);
            });
        return () => { cancelled = true; };
    }, [open, editJobUid]);

    const handleAddTech = (tech?: string) => {
        const value = tech || formData.techInput;
        if (value.trim() && !formData.techStack.includes(value.trim())) {
            setFormData(prev => ({
                ...prev,
                techStack: [...prev.techStack, value.trim()],
                techInput: ""
            }));
        }
    };

    const handleRemoveTech = (tech: string) => {
        setFormData(prev => ({
            ...prev,
            techStack: prev.techStack.filter(t => t !== tech)
        }));
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const files = Array.from(e.target.files);
            setFormData(prev => ({ ...prev, images: [...prev.images, ...files] }));

            const newPreviews = files.map(file => URL.createObjectURL(file));
            setPreviewUrls(prev => [...prev, ...newPreviews]);
        }
    };

    const handleRemoveImage = (index: number) => {
        setFormData(prev => ({
            ...prev,
            images: prev.images.filter((_, i) => i !== index)
        }));
        setPreviewUrls(prev => prev.filter((_, i) => i !== index));
    };

    // 연봉 텍스트에 "만원" 자동 추가
    const formatSalaryText = (salary: string): string => {
        if (!salary || salary.trim() === "") return "";
        const trimmed = salary.trim();
        // 이미 "만원", "원", "협의" 등이 포함되어 있으면 그대로 반환
        if (/만원|원|협의|면접|회의/i.test(trimmed)) {
            return trimmed;
        }
        // 숫자만 있거나 숫자~숫자 형태인 경우 "만원" 추가
        if (/^[\d,.\s~\-]+$/.test(trimmed)) {
            return `${trimmed}만원`;
        }
        return trimmed;
    };

    // 이미지 업로드 함수
    const uploadImages = async (files: File[]): Promise<string[]> => {
        if (files.length === 0) return [];
        
        const formDataObj = new FormData();
        files.forEach((file) => {
            formDataObj.append("files", file);
        });
        
        const response = await http.post("/api/jobs/upload-images", formDataObj, {
            headers: { "Content-Type": "multipart/form-data" },
        });
        
        return response.data.urls || [];
    };

    const handleSubmit = async () => {
        if (!formData.title || !formData.position || !formData.location || !formData.description) {
            alert("필수 항목을 모두 입력해주세요.");
            return;
        }

        setSubmitting(true);
        try {
            // 이미지가 있으면 먼저 업로드
            let imageUrls: string[] = [...uploadedImageUrls];
            if (formData.images.length > 0) {
                setUploadingImages(true);
                const newUrls = await uploadImages(formData.images);
                imageUrls = [...imageUrls, ...newUrls];
                setUploadingImages(false);
            }

            const payload = {
                title: formData.title,
                description: formData.description,
                location: formData.location,
                salaryText: formatSalaryText(formData.salary) || null,
                stack: formData.techStack.length > 0 ? formData.techStack.join(",") : null,
                requiredExperience: formData.experience ? parseInt(formData.experience, 10) : 0,
                recruitmentCapacity: formData.recruitmentCapacity ? parseInt(formData.recruitmentCapacity, 10) : 0,
                status: "OPEN",
                images: imageUrls.length > 0 ? imageUrls : null,
            };
            if (isEditMode && editJobUid) {
                await updateJobPosting(editJobUid, payload);
                alert("채용공고가 수정되었습니다.");
            } else {
                await createJobPosting(payload);
                alert("채용공고가 등록되었습니다.");
            }
            onOpenChange(false);
            setFormData({
                title: "",
                position: "",
                location: "",
                experience: "",
                salary: "",
                deadline: "",
                description: "",
                techStack: [],
                techInput: "",
                recruitmentCapacity: "",
                images: [],
            });
            setPreviewUrls([]);
            setUploadedImageUrls([]);
            onSuccess?.();
        } catch (err: unknown) {
            setUploadingImages(false);
            const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error ?? (isEditMode ? "수정에 실패했습니다." : "등록에 실패했습니다.");
            alert(msg);
        } finally {
            setSubmitting(false);
        }
    };

    const handleOpenChange = (next: boolean) => {
        if (!next) {
            if (!isEditMode) {
                setFormData({
                    title: "", position: "", location: "", experience: "", salary: "", deadline: "",
                    description: "", techStack: [], techInput: "", recruitmentCapacity: "", images: [],
                });
                setPreviewUrls([]);
            }
        }
        onOpenChange(next);
    };

    const locationOptions = formData.location && !LOCATIONS.includes(formData.location)
        ? [formData.location, ...LOCATIONS]
        : LOCATIONS;
    const positionOptions = formData.position && !POSITIONS.some((p) => p.value === formData.position)
        ? [{ value: formData.position, label: formData.position }, ...POSITIONS]
        : POSITIONS;

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="sm:max-w-4xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="text-xl">
                        {isEditMode ? "채용공고 수정" : "새 채용공고 등록"}
                    </DialogTitle>
                    <DialogDescription>
                        {isEditMode
                            ? "내용을 수정한 뒤 아래 버튼으로 저장하세요."
                            : "우수한 인재를 찾기 위한 매력적인 채용공고를 작성해주세요."}
                    </DialogDescription>
                </DialogHeader>
                {loadingJob ? (
                    <div className="flex items-center justify-center py-8 text-muted-foreground">
                        채용공고를 불러오는 중...
                    </div>
                ) : (
                <>
                <div className="grid gap-6 py-4">
                    {/* 1. 기본 정보 */}
                    <div className="space-y-4">
                        <h3 className="font-semibold text-lg flex items-center gap-2">
                            <span className="w-1 h-6 bg-primary rounded-full"></span>
                            기본 정보
                        </h3>
                        <div className="grid gap-4">
                            <div className="grid gap-2">
                                <Label htmlFor="title">공고 제목 <span className="text-red-500">*</span></Label>
                                <Input
                                    id="title"
                                    placeholder="예: [신입/경력] 프론트엔드 개발자 채용 (React, TypeScript)"
                                    value={formData.title}
                                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                                />
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div className="grid gap-2">
                                    <Label htmlFor="position">직무 <span className="text-red-500">*</span></Label>
                                    <Select
                                        value={formData.position}
                                        onValueChange={(val) => setFormData({ ...formData, position: val })}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="직무 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {positionOptions.map((pos) => (
                                                <SelectItem key={pos.value} value={pos.value}>{pos.label}</SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="experience">경력 요건 <span className="text-red-500">*</span></Label>
                                    <Select
                                        value={formData.experience}
                                        onValueChange={(val) => setFormData({ ...formData, experience: val })}
                                    >
                                        <SelectTrigger>
                                            <SelectValue placeholder="경력 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            <SelectItem value="0">신입</SelectItem>
                                            <SelectItem value="1">1년 이상</SelectItem>
                                            <SelectItem value="3">3년 이상</SelectItem>
                                            <SelectItem value="5">5년 이상</SelectItem>
                                            <SelectItem value="7">7년 이상</SelectItem>
                                            <SelectItem value="10">10년 이상</SelectItem>
                                        </SelectContent>
                                    </Select>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 2. 근무 조건 */}
                    <div className="space-y-4">
                        <h3 className="font-semibold text-lg flex items-center gap-2">
                            <span className="w-1 h-6 bg-primary rounded-full"></span>
                            근무 조건
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="grid gap-2">
                                <Label htmlFor="location">근무지 <span className="text-red-500">*</span></Label>
                                <Select
                                    value={formData.location}
                                    onValueChange={(val) => setFormData({ ...formData, location: val })}
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="근무지 선택" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {locationOptions.map(loc => <SelectItem key={loc} value={loc}>{loc}</SelectItem>)}
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="salary">연봉 (만원)</Label>
                                <Input
                                    id="salary"
                                    placeholder="예: 4000 ~ 6000 (면접 후 협의)"
                                    value={formData.salary}
                                    onChange={(e) => setFormData({ ...formData, salary: e.target.value })}
                                />
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="capacity">모집 인원 (명)</Label>
                                <Input
                                    id="capacity"
                                    type="number"
                                    placeholder="0 (제한 없음)"
                                    value={formData.recruitmentCapacity}
                                    onChange={(e) => setFormData({ ...formData, recruitmentCapacity: e.target.value })}
                                />
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="deadline">마감일</Label>
                                <Input
                                    id="deadline"
                                    type="date"
                                    value={formData.deadline}
                                    onChange={(e) => setFormData({ ...formData, deadline: e.target.value })}
                                />
                            </div>
                        </div>
                    </div>

                    {/* 3. 기술 스택 */}
                    <div className="space-y-4">
                        <h3 className="font-semibold text-lg flex items-center gap-2">
                            <span className="w-1 h-6 bg-primary rounded-full"></span>
                            기술 스택
                        </h3>
                        <div className="grid gap-2">
                            <div className="flex flex-wrap gap-2 mb-2">
                                {COMMON_STACKS.map(stack => (
                                    <Badge
                                        key={stack}
                                        variant="outline"
                                        className="cursor-pointer hover:bg-secondary"
                                        onClick={() => handleAddTech(stack)}
                                    >
                                        + {stack}
                                    </Badge>
                                ))}
                            </div>
                            <div className="flex gap-2">
                                <Input
                                    placeholder="직접 입력 (Enter로 추가)"
                                    value={formData.techInput}
                                    onChange={(e) => setFormData({ ...formData, techInput: e.target.value })}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter') {
                                            e.preventDefault();
                                            handleAddTech();
                                        }
                                    }}
                                />
                                <Button type="button" onClick={() => handleAddTech()} size="icon">
                                    <Plus className="h-4 w-4" />
                                </Button>
                            </div>
                            <div className="flex flex-wrap gap-2 min-h-[40px] p-2 bg-muted/30 rounded-md border border-dashed">
                                {formData.techStack.length === 0 && <span className="text-sm text-muted-foreground self-center">선택된 기술 스택이 없습니다.</span>}
                                {formData.techStack.map((tech) => (
                                    <Badge key={tech} variant="secondary" className="flex items-center gap-1">
                                        {tech}
                                        <button onClick={() => handleRemoveTech(tech)} className="hover:text-destructive">
                                            <X className="h-3 w-3" />
                                        </button>
                                    </Badge>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* 4. 상세 내용 & 이미지 */}
                    <div className="space-y-4">
                        <h3 className="font-semibold text-lg flex items-center gap-2">
                            <span className="w-1 h-6 bg-primary rounded-full"></span>
                            상세 내용
                        </h3>
                        <div className="grid gap-2">
                            <Label htmlFor="description">상세 업무 및 자격 요건 <span className="text-red-500">*</span></Label>
                            <Textarea
                                id="description"
                                placeholder="주요 업무, 자격 요건, 우대 사항, 혜택 및 복지 등을 상세히 작성해주세요."
                                className="min-h-[300px]"
                                value={formData.description}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            />
                        </div>

                        <div className="grid gap-2">
                            <Label>채용 공고 이미지 첨부</Label>
                            <div className="border-2 border-dashed border-input hover:bg-muted/50 rounded-lg p-6 flex flex-col items-center justify-center cursor-pointer transition-colors"
                                onClick={() => fileInputRef.current?.click()}
                            >
                                <Upload className="h-8 w-8 text-muted-foreground mb-2" />
                                <p className="text-sm font-medium">이미지를 클릭하여 업로드하거나 드래그 앤 드롭하세요</p>
                                <p className="text-xs text-muted-foreground mt-1">PNG, JPG, GIF (최대 10MB)</p>
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    multiple
                                    accept="image/*"
                                    className="hidden"
                                    onChange={handleFileChange}
                                />
                            </div>

                            {previewUrls.length > 0 && (
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-2">
                                    {previewUrls.map((url, idx) => (
                                        <div key={idx} className="relative aspect-video rounded-lg overflow-hidden border bg-muted">
                                            <img src={url} alt={`Preview ${idx}`} className="w-full h-full object-cover" />
                                            <button
                                                onClick={() => handleRemoveImage(idx)}
                                                className="absolute top-1 right-1 bg-black/50 hover:bg-red-500 text-white p-1 rounded-full transition-colors"
                                            >
                                                <X className="h-3 w-3" />
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={submitting}>취소</Button>
                    <Button onClick={handleSubmit} className="btn-gradient-primary" disabled={submitting}>
                        {submitting
                            ? (isEditMode ? "수정 중..." : "등록 중...")
                            : (isEditMode ? "수정 완료" : "공고 등록하기")}
                    </Button>
                </DialogFooter>
                </>
                )}
            </DialogContent>
        </Dialog>
    );
}
