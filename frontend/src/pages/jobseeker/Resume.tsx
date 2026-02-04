import { useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
    FileText,
    Bot,
    Layout,
    Plus,
    Eye,
    Download,
    Trash2,
    MoreVertical,
    LogIn,
    Loader2,
    Star,
} from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { ResumeTemplateSelector } from "@/components/resume/ResumeTemplateSelector";
import { ResumeEditor, type ResumeData } from "@/components/resume/ResumeEditor";
import { AIProofreadingPanel } from "@/components/resume/AIProofreadingPanel";
import { AISummaryModal } from "@/components/resume/AISummaryModal";
import { PrimaryResumeConfirmModal } from "@/components/resume/PrimaryResumeConfirmModal";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
    useMyResumes,
    useCreateResume,
    useUpdateResume,
    useDeleteResume,
    useSetPrimaryResume,
    useCopyResume,
} from "@/hooks/useResumes";
import { getResume, updateResumeSummary } from "@/api/resumes";
import { useQueryClient } from "@tanstack/react-query";

const initialResumeData: ResumeData = {
    personalInfo: {
        name: "",
        email: "",
        phone: "",
        address: "",
        birthDate: "",
    },
    summary: "",
    experiences: [],
    education: [],
    skills: [],
};

/** API 응답 → 에디터용 ResumeData */
function mapApiResumeToResumeData(res: {
    title?: string;
    content?: string;
    summary?: string;
    profile?: { address?: string };
    careers?: { id?: number; companyName?: string; department?: string; role?: string; startDate?: string; endDate?: string }[];
    reStack?: string | string[];
}): ResumeData {
    const careers = res.careers ?? [];
    const reStack = res.reStack;
    const skills = Array.isArray(reStack) ? reStack : (typeof reStack === "string" ? reStack.split(",").map((s) => s.trim()).filter(Boolean) : []);
    return {
        personalInfo: {
            name: res.title ?? "",
            email: "",
            phone: "",
            address: res.profile?.address ?? "",
            birthDate: "",
        },
        summary: res.content ?? res.summary ?? "",
        aiSummary: res.summary, // AI 요약 결과 원본
        experiences: careers.map((c) => ({
            id: String(c.id ?? ""),
            company: c.companyName ?? "",
            position: c.role ?? "",
            startDate: c.startDate ?? "",
            endDate: c.endDate ?? "",
            description: "",
        })),
        education: [],
        skills,
    };
}

/** 에디터 ResumeData → 백엔드 create/update 요청 body */
function buildResumeRequest(resumeData: ResumeData) {
    const title = resumeData.personalInfo?.name?.trim() || "제목 없는 이력서";
    const careers = (resumeData.experiences ?? [])
        .filter((e) => e.company || e.position)
        .map((e) => ({
            companyName: e.company || "(회사명)",
            department: "-",
            role: e.position || "(직책)",
            startDate: e.startDate || "2020-01-01",
            endDate: e.endDate || null,
            current: !e.endDate,
        }));
    return {
        title,
        field: "RESUME",
        content: resumeData.summary ?? "",
        summary: resumeData.summary ?? "",
        tagline: "",
        primary: false,
        reStack: resumeData.skills ?? [],
        profile: {
            address: resumeData.personalInfo?.address ?? "",
            photoUrl: null,
        },
        careers,
    };
}

function formatResumeDate(dateStr: string | undefined): string {
    if (!dateStr) return "-";
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });
    } catch {
        return String(dateStr);
    }
}

const ResumePage = () => {
    const { user, loading } = useAuth();
    const { toast } = useToast();
    const [selectedTemplate, setSelectedTemplate] = useState("basic");
    const [resumeData, setResumeData] = useState<ResumeData>(initialResumeData);
    const [activeTab, setActiveTab] = useState("list");
    const [editingResumeId, setEditingResumeId] = useState<number | null>(null);
    const [viewingResumeId, setViewingResumeId] = useState<number | null>(null);
    const [viewingResumeData, setViewingResumeData] = useState<ResumeData | null>(null);
    const [loadingViewing, setLoadingViewing] = useState(false);

    // AI Summary Modal State
    const [aiSummaryModalOpen, setAiSummaryModalOpen] = useState(false);
    const [aiSummaryData, setAiSummaryData] = useState<ResumeData | null>(null);
    const [aiSummaryText, setAiSummaryText] = useState("");
    const [aiSummaryResumeId, setAiSummaryResumeId] = useState<number | null>(null);
    const [primaryConfirmId, setPrimaryConfirmId] = useState<number | null>(null);

    const { data: resumesList = [], isLoading: listLoading } = useMyResumes({ enabled: !!user });
    const createMutation = useCreateResume();
    const updateMutation = useUpdateResume();
    const deleteMutation = useDeleteResume();
    const setPrimaryMutation = useSetPrimaryResume();
    const copyMutation = useCopyResume();
    const queryClient = useQueryClient();

    const handleViewAiSummary = async (id: number) => {
        try {
            const res = await getResume(id);
            const data = mapApiResumeToResumeData(res);
            setAiSummaryResumeId(id);
            setAiSummaryData(data);
            setAiSummaryText(data.aiSummary ?? data.summary ?? ""); // AI 요약 우선, 없으면 내용
            setAiSummaryModalOpen(true);
        } catch (error) {
            console.error(error);
            toast({ variant: "destructive", title: "정보를 불러오는데 실패했습니다." });
        }
    };

    const handleConfirmSetPrimary = () => {
        if (!primaryConfirmId) return;
        setPrimaryMutation.mutate(
            primaryConfirmId,
            {
                onSuccess: () => {
                    toast({ title: "대표 이력서가 설정되었습니다." });
                    setPrimaryConfirmId(null);
                    // invalidate queries to refresh list
                    queryClient.invalidateQueries({ queryKey: ["my-resumes"] });
                },
                onError: () => {
                    toast({ variant: "destructive", title: "설정에 실패했습니다." });
                }
            }
        );
    };

    const handleSaveAiSummary = async () => {
        if (!aiSummaryResumeId) return;

        try {
            await updateResumeSummary(aiSummaryResumeId, aiSummaryText);
            toast({ title: "AI 요약이 저장되었습니다." });
            setAiSummaryModalOpen(false);
            queryClient.invalidateQueries({ queryKey: ["my-resumes"] });
        } catch (error) {
            console.error(error);
            toast({ variant: "destructive", title: "저장에 실패했습니다." });
        }
    };

    const handleApplyAiSummary = () => {
        if (!aiSummaryData || !aiSummaryResumeId) return;

        // Update the resume content with the text from modal
        const updatedData = { ...aiSummaryData, summary: aiSummaryText };
        const payload = buildResumeRequest(updatedData);

        updateMutation.mutate(
            { resumeId: aiSummaryResumeId, data: payload },
            {
                onSuccess: () => {
                    toast({ title: "자기소개가 업데이트되었습니다." });
                    setAiSummaryModalOpen(false);
                },
                onError: () => {
                    toast({ variant: "destructive", title: "업데이트에 실패했습니다." });
                }
            }
        );
    };

    const list = Array.isArray(resumesList) ? resumesList : [];

    const handleSaveResume = useCallback(() => {
        const payload = buildResumeRequest(resumeData);
        if (editingResumeId != null) {
            updateMutation.mutate(
                { resumeId: editingResumeId, resume: payload },
                {
                    onSuccess: () => {
                        toast({ title: "이력서 저장 완료", description: "수정 내용이 반영되었습니다." });
                        setActiveTab("list");
                        setEditingResumeId(null);
                        setResumeData(initialResumeData);
                    },
                    onError: (err: { response?: { data?: { message?: string } }; message?: string }) => {
                        toast({
                            variant: "destructive",
                            title: "저장 실패",
                            description: err?.response?.data?.message || err?.message || "다시 시도해 주세요.",
                        });
                    },
                }
            );
        } else {
            createMutation.mutate(payload, {
                onSuccess: () => {
                    toast({ title: "이력서 저장 완료", description: "새 이력서가 추가되었습니다." });
                    setActiveTab("list");
                    setEditingResumeId(null);
                    setResumeData(initialResumeData);
                },
                onError: (err: { response?: { data?: { message?: string } }; message?: string }) => {
                    toast({
                        variant: "destructive",
                        title: "저장 실패",
                        description: err?.response?.data?.message || err?.message || "다시 시도해 주세요.",
                    });
                },
            });
        }
    }, [resumeData, editingResumeId, updateMutation, createMutation, toast]);

    const handleApplySuggestion = (newContent: string) => {
        setResumeData((prev) => ({ ...prev, summary: newContent }));
    };

    const handleCreateNew = () => {
        setResumeData(initialResumeData);
        setEditingResumeId(null);
        setActiveTab("edit");
    };

    const handleEditResume = (resumeId: number) => {
        setViewingResumeId(null);
        setViewingResumeData(null);
        getResume(resumeId)
            .then((res) => {
                setResumeData(mapApiResumeToResumeData(res));
                setEditingResumeId(resumeId);
                setActiveTab("edit");
            })
            .catch(() => {
                toast({ variant: "destructive", title: "이력서를 불러올 수 없습니다." });
            });
    };

    const handleViewResume = (resumeId: number) => {
        setViewingResumeId(resumeId);
        setViewingResumeData(null);
        setLoadingViewing(true);
        getResume(resumeId)
            .then((res) => setViewingResumeData(mapApiResumeToResumeData(res)))
            .catch(() => {
                toast({ variant: "destructive", title: "이력서를 불러올 수 없습니다." });
                setViewingResumeId(null);
            })
            .finally(() => setLoadingViewing(false));
    };

    const closeViewModal = () => {
        setViewingResumeId(null);
        setViewingResumeData(null);
    };



    const handleCopyResume = (resumeId: number) => {
        copyMutation.mutate(resumeId, {
            onSuccess: () => toast({ title: "이력서가 복사되었습니다." }),
            onError: () => toast({ variant: "destructive", title: "복사에 실패했습니다." }),
        });
    };

    const handleDeleteResume = (resumeId: number) => {
        if (!window.confirm("이 이력서를 삭제하시겠습니까?")) return;
        deleteMutation.mutate(resumeId, {
            onSuccess: () => toast({ title: "이력서가 삭제되었습니다." }),
            onError: (err: { response?: { data?: { message?: string } }; message?: string }) => {
                toast({
                    variant: "destructive",
                    title: "삭제 실패",
                    description: err?.response?.data?.message || err?.message || "이미 지원에 사용된 이력서는 삭제할 수 없습니다.",
                });
            },
        });
    };

    const saving = createMutation.isPending || updateMutation.isPending;

    if (!user && !loading) {
        return (
            <div className="min-h-screen bg-[#F8F9FA]">
                <Sidebar />
                <div className="lg:pl-64 flex flex-col min-h-screen">
                    <Header />
                    <main className="flex-1 p-6 lg:p-10 flex items-center justify-center">
                        <div className="max-w-2xl w-full">
                            <Card className="overflow-hidden border-none shadow-lg">
                                <div className="bg-gradient-to-br from-sky-500/10 to-sky-500/5 p-10 text-center border-b border-sky-100/50">
                                    <div className="mx-auto h-20 w-20 rounded-2xl bg-white shadow-sm flex items-center justify-center mb-6">
                                        <FileText className="h-10 w-10 text-sky-500" />
                                    </div>
                                    <h1 className="text-2xl font-bold text-gray-900 mb-2">이력서 관리</h1>
                                    <p className="text-gray-500">
                                        로그인하고 나만의 전문적인 이력서를 작성해보세요
                                    </p>
                                </div>
                                <CardContent className="p-8">
                                    <div className="grid gap-4 mb-8">
                                        <div className="flex items-center gap-4 p-4 rounded-xl bg-gray-50/50 border border-gray-100/50">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <Layout className="h-5 w-5 text-sky-500" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900">전문적인 템플릿</p>
                                                <p className="text-sm text-gray-500">다양한 디자인의 템플릿으로 나를 더 돋보이게 만드세요</p>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-4 p-4 rounded-xl bg-gray-50/50 border border-gray-100/50">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <Bot className="h-5 w-5 text-sky-500" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900">AI 첨삭 서비스</p>
                                                <p className="text-sm text-gray-500">AI가 문장 하나하나를 정교하게 다듬어 드립니다</p>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-4 p-4 rounded-xl bg-gray-50/50 border border-gray-100/50">
                                            <div className="h-10 w-10 rounded-lg bg-white shadow-sm flex items-center justify-center">
                                                <Download className="h-5 w-5 text-sky-500" />
                                            </div>
                                            <div>
                                                <p className="font-bold text-gray-900">PDF 간편 소장</p>
                                                <p className="text-sm text-gray-500">완성된 이력서를 깔끔한 PDF 파일로 다운로드하세요</p>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="flex flex-col gap-4">
                                        <Button asChild className="w-full text-white h-12 shadow-md border-0 transition-all hover:scale-[1.02]" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }} size="lg">
                                            <Link to="/auth">
                                                <LogIn className="h-5 w-5 mr-2" />
                                                로그인하고 시작하기
                                            </Link>
                                        </Button>
                                        <p className="text-center text-sm text-gray-400">
                                            아직 회원이 아니신가요?{" "}
                                            <Link to="/auth?tab=register" className="text-sky-500 font-bold hover:underline">
                                                간편 회원가입
                                            </Link>
                                        </p>
                                    </div>
                                </CardContent>
                            </Card>
                        </div>
                    </main>
                    <Footer />
                </div>
            </div>
        );
    }

    return (
        <>
            <div className="min-h-screen bg-[#F8F9FA]">
                <Sidebar />

                <div className="lg:pl-64 flex flex-col min-h-screen">
                    <Header />

                    <main className="flex-1 p-6 lg:p-10">
                        <div className="max-w-7xl mx-auto">
                            {/* 페이지 헤더 */}
                            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-8">
                                <div>
                                    <h1 className="text-2xl font-bold text-gray-900">이력서 관리</h1>
                                    <p className="text-gray-500 mt-1">
                                        나만의 전문적인 이력서를 작성하고 AI의 도움을 받아보세요
                                    </p>
                                </div>
                                <Button onClick={handleCreateNew} className="text-white shadow-md border-0 gap-2 transition-all hover:scale-[1.02]" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}>
                                    <Plus className="h-4 w-4" />
                                    새 이력서 작성
                                </Button>
                            </div>

                            <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
                                <TabsList className="bg-white p-1 rounded-xl shadow-sm border-none w-full max-w-md grid grid-cols-3">
                                    <TabsTrigger value="list" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                        <FileText className="h-4 w-4" />
                                        내 이력서
                                    </TabsTrigger>
                                    <TabsTrigger value="edit" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                        <Layout className="h-4 w-4" />
                                        작성하기
                                    </TabsTrigger>
                                    <TabsTrigger value="ai" className="gap-2 rounded-lg data-[state=active]:bg-sky-50 data-[state=active]:text-sky-600">
                                        <Bot className="h-4 w-4" />
                                        AI 첨삭
                                    </TabsTrigger>
                                </TabsList>

                                {/* 이력서 목록 (DB 연동) */}
                                <TabsContent value="list" className="mt-6">
                                    {/* 대표 이력서 안내 */}
                                    {!listLoading && list.length > 1 && (
                                        <div className="mb-4 p-4 bg-sky-50/50 border border-sky-100 rounded-xl">
                                            <p className="text-sm text-sky-700">
                                                <Star className="h-4 w-4 inline-block mr-1.5 text-yellow-500 fill-yellow-500" />
                                                <strong>대표 이력서</strong>를 설정하면 지원 시 기본으로 선택됩니다. 별표 버튼을 클릭하여 대표 이력서를 지정하세요.
                                            </p>
                                        </div>
                                    )}
                                    <div className="grid gap-4">
                                        {listLoading ? (
                                            <div className="flex justify-center py-12">
                                                <Loader2 className="h-10 w-10 animate-spin text-sky-500" />
                                            </div>
                                        ) : (
                                            list.map((resume: { id: number; title?: string; lastModifiedAt?: string; primary?: boolean }) => (
                                                <Card key={resume.id} className={`border-none shadow-sm hover:shadow-md transition-all bg-white overflow-hidden group ${resume.primary ? 'ring-2 ring-sky-500/50' : ''}`}>
                                                    <CardContent className="flex items-center justify-between p-6">
                                                        <div
                                                            className="flex items-center gap-5 flex-1 min-w-0 cursor-pointer"
                                                            onClick={() => handleViewResume(resume.id)}
                                                            role="button"
                                                            tabIndex={0}
                                                            onKeyDown={(e) => e.key === "Enter" && handleViewResume(resume.id)}
                                                            aria-label={`이력서 ${resume.title ?? "제목 없음"} 보기`}
                                                        >
                                                            <div className={`h-14 w-14 rounded-xl flex items-center justify-center transition-colors shrink-0 ${resume.primary ? 'bg-sky-100' : 'bg-sky-50 group-hover:bg-sky-100'}`}>
                                                                <FileText className="h-7 w-7 text-sky-500" />
                                                            </div>
                                                            <div className="min-w-0">
                                                                <div className="flex items-center gap-3">
                                                                    <h3 className="font-bold text-lg text-gray-900 truncate">{resume.title ?? "제목 없음"}</h3>
                                                                    {resume.primary && (
                                                                        <Badge className="bg-sky-500 text-white border-none text-[10px] px-2 py-0 shrink-0">
                                                                            대표
                                                                        </Badge>
                                                                    )}
                                                                </div>
                                                                <p className="text-sm text-gray-400 mt-1">
                                                                    마지막 수정: {formatResumeDate(resume.lastModifiedAt)}
                                                                </p>
                                                            </div>
                                                        </div>
                                                        <div className="flex items-center gap-2 shrink-0" onClick={(e) => e.stopPropagation()}>
                                                            {/* 대표 이력서 설정 버튼 */}
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className={`transition-colors ${resume.primary ? 'text-yellow-500 hover:text-yellow-600' : 'text-gray-300 hover:text-yellow-500'}`}
                                                                onClick={() => {
                                                                    if (!resume.primary) setPrimaryConfirmId(resume.id);
                                                                }}
                                                                disabled={!!resume.primary || setPrimaryMutation.isPending}
                                                                title={resume.primary ? "현재 대표 이력서" : "대표 이력서로 설정"}
                                                            >
                                                                <Star className={`h-5 w-5 ${resume.primary ? 'fill-yellow-500' : ''}`} />
                                                            </Button>

                                                            {/* AI 요약 보기 버튼 */}
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                className="text-sky-500 hover:text-sky-600 hover:bg-sky-50 transition-colors"
                                                                onClick={(e) => { e.stopPropagation(); handleViewAiSummary(resume.id); }}
                                                                title="AI 요약 결과 보기"
                                                            >
                                                                <Bot className="h-5 w-5" />
                                                            </Button>
                                                            <DropdownMenu>
                                                                <DropdownMenuTrigger asChild>
                                                                    <Button variant="ghost" size="icon" className="text-gray-400 hover:text-gray-600">
                                                                        <MoreVertical className="h-4 w-4" />
                                                                    </Button>
                                                                </DropdownMenuTrigger>
                                                                <DropdownMenuContent align="end" className="w-48 p-2">
                                                                    <DropdownMenuItem onClick={() => handleEditResume(resume.id)} className="rounded-md">
                                                                        수정하기
                                                                    </DropdownMenuItem>
                                                                    <DropdownMenuItem onClick={() => setPrimaryConfirmId(resume.id)} className="rounded-md" disabled={!!resume.primary}>
                                                                        <Star className="h-4 w-4 mr-2" />
                                                                        대표 이력서로 설정
                                                                    </DropdownMenuItem>
                                                                    <DropdownMenuItem onClick={() => handleCopyResume(resume.id)} className="rounded-md" disabled={copyMutation.isPending}>
                                                                        복사하기
                                                                    </DropdownMenuItem>
                                                                    <DropdownMenuItem
                                                                        onClick={() => handleDeleteResume(resume.id)}
                                                                        className="text-destructive rounded-md focus:bg-destructive/10"
                                                                        disabled={deleteMutation.isPending}
                                                                    >
                                                                        <Trash2 className="h-4 w-4 mr-2" />
                                                                        삭제하기
                                                                    </DropdownMenuItem>
                                                                </DropdownMenuContent>
                                                            </DropdownMenu>
                                                        </div>
                                                    </CardContent>
                                                </Card>
                                            ))
                                        )}

                                        {/* 빈 상태 */}
                                        {!listLoading && list.length === 0 && (
                                            <Card className="border-none shadow-sm bg-white border-dashed border-2 border-gray-100">
                                                <CardContent className="flex flex-col items-center justify-center py-20">
                                                    <div className="h-16 w-16 bg-gray-50 rounded-full flex items-center justify-center mb-6">
                                                        <FileText className="h-8 w-8 text-gray-300" />
                                                    </div>
                                                    <h3 className="text-xl font-bold text-gray-900 mb-2">
                                                        아직 작성된 이력서가 없습니다
                                                    </h3>
                                                    <p className="text-gray-500 mb-8">
                                                        첫 이력서를 작성하고 나만의 경쟁력을 높여보세요!
                                                    </p>
                                                    <Button onClick={handleCreateNew} className="text-white border-0 transition-all hover:scale-[1.02]" style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}>
                                                        <Plus className="h-4 w-4 mr-2" />
                                                        새 이력서 작성
                                                    </Button>
                                                </CardContent>
                                            </Card>
                                        )}
                                    </div>
                                </TabsContent>

                                {/* 이력서 작성 */}
                                <TabsContent value="edit" className="mt-6">
                                    <div className="space-y-8">
                                        {/* 템플릿 선택 */}
                                        <Card className="border-none shadow-sm bg-white p-6">
                                            <h2 className="text-lg font-bold text-gray-900 mb-6">템플릿 선택</h2>
                                            <ResumeTemplateSelector
                                                selectedTemplate={selectedTemplate}
                                                onSelectTemplate={setSelectedTemplate}
                                            />
                                        </Card>

                                        {/* 이력서 에디터 */}
                                        <ResumeEditor
                                            resumeData={resumeData}
                                            onUpdateResume={setResumeData}
                                            onSave={handleSaveResume}
                                            saving={saving}
                                        />
                                    </div>
                                </TabsContent>

                                {/* AI 첨삭 */}
                                <TabsContent value="ai" className="mt-6">
                                    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                                        {/* 현재 이력서 내용 */}
                                        <div className="lg:col-span-7">
                                            <h2 className="text-lg font-bold text-gray-900 mb-4">이력서 내용</h2>
                                            <Card className="border-none shadow-sm bg-white min-h-[600px]">
                                                <CardContent className="p-8">
                                                    {resumeData.summary ? (
                                                        <div className="prose prose-sky max-w-none">
                                                            <h3 className="text-xl font-bold border-b pb-2 mb-4">자기소개</h3>
                                                            <p className="text-gray-700 whitespace-pre-wrap leading-relaxed">
                                                                {resumeData.summary}
                                                            </p>

                                                            {resumeData.experiences.length > 0 && (
                                                                <div className="mt-10">
                                                                    <h3 className="text-xl font-bold border-b pb-2 mb-4">경력</h3>
                                                                    {resumeData.experiences.map((exp) => (
                                                                        <div key={exp.id} className="mb-6">
                                                                            <div className="flex justify-between items-start">
                                                                                <p className="font-bold text-gray-900">{exp.company}</p>
                                                                                <span className="text-sm text-gray-400">{exp.startDate} ~ {exp.endDate}</span>
                                                                            </div>
                                                                            <p className="text-sky-600 font-medium text-sm">{exp.position}</p>
                                                                            <p className="text-gray-600 text-sm mt-2">{exp.description}</p>
                                                                        </div>
                                                                    ))}
                                                                </div>
                                                            )}
                                                        </div>
                                                    ) : (
                                                        <div className="flex flex-col items-center justify-center h-full py-20 text-center">
                                                            <div className="h-16 w-16 bg-sky-50 rounded-full flex items-center justify-center mb-6">
                                                                <Bot className="h-8 w-8 text-sky-500" />
                                                            </div>
                                                            <h3 className="text-xl font-bold text-gray-900 mb-2">분석할 내용이 아직 없습니다</h3>
                                                            <p className="text-gray-500 mb-8">
                                                                이력서를 먼저 작성하시면 AI가 전문적인 피드백을 제공해 드립니다.
                                                            </p>
                                                            <Button
                                                                variant="outline"
                                                                className="border-gray-200 text-gray-600"
                                                                onClick={() => setActiveTab("edit")}
                                                            >
                                                                이력서 작성하러 가기
                                                            </Button>
                                                        </div>
                                                    )}
                                                </CardContent>
                                            </Card>
                                        </div>

                                        {/* AI 첨삭 패널 */}
                                        <div className="lg:col-span-5">
                                            <h2 className="text-lg font-bold text-gray-900 mb-4">AI 피드백 및 첨삭</h2>
                                            <div className="sticky top-24">
                                                <AIProofreadingPanel
                                                    content={resumeData.summary}
                                                    onApplySuggestion={handleApplySuggestion}
                                                />
                                            </div>
                                        </div>
                                    </div>
                                </TabsContent>
                            </Tabs>
                        </div>
                    </main>

                    <Footer />
                </div>
            </div>

            {/* Primary Resume Confirmation Modal */}
            <PrimaryResumeConfirmModal
                isOpen={primaryConfirmId !== null}
                onClose={() => setPrimaryConfirmId(null)}
                onConfirm={handleConfirmSetPrimary}
            />

            {/* AI Summary Modal */}
            <AISummaryModal
                isOpen={aiSummaryModalOpen}
                onClose={() => setAiSummaryModalOpen(false)}
                summaryText={aiSummaryText}
                onSummaryChange={setAiSummaryText}
                onSave={handleSaveAiSummary}
            />

            {/* 이력서 상세 보기 모달 */}
            <Dialog open={viewingResumeId != null} onOpenChange={(open) => !open && closeViewModal()}>
                <DialogContent className="sm:max-w-[640px] max-h-[90vh] overflow-y-auto">
                    <DialogHeader>
                        <DialogTitle>이력서 보기</DialogTitle>
                    </DialogHeader>
                    {loadingViewing ? (
                        <div className="flex justify-center py-12">
                            <Loader2 className="h-10 w-10 animate-spin text-sky-500" />
                        </div>
                    ) : viewingResumeData ? (
                        <div className="prose prose-sky max-w-none py-2">
                            <h3 className="text-lg font-bold border-b border-sky-100 pb-2 mb-4 text-gray-900">
                                {viewingResumeData.personalInfo?.name?.trim() || "제목 없는 이력서"}
                            </h3>
                            {viewingResumeData.summary ? (
                                <>
                                    <h4 className="text-base font-semibold text-gray-800 mt-6 mb-2">자기소개</h4>
                                    <p className="text-gray-700 whitespace-pre-wrap leading-relaxed text-sm">
                                        {viewingResumeData.summary}
                                    </p>
                                </>
                            ) : null}
                            {viewingResumeData.experiences?.length > 0 ? (
                                <div className="mt-6">
                                    <h4 className="text-base font-semibold text-gray-800 mb-3">경력</h4>
                                    <ul className="space-y-4 list-none p-0 m-0">
                                        {viewingResumeData.experiences.map((exp) => (
                                            <li key={exp.id} className="border-b border-gray-100 pb-4 last:border-0">
                                                <div className="flex justify-between items-start gap-2">
                                                    <p className="font-semibold text-gray-900">{exp.company || "(회사명)"}</p>
                                                    <span className="text-xs text-gray-400 shrink-0">
                                                        {exp.startDate || "-"} ~ {exp.endDate || "재직 중"}
                                                    </span>
                                                </div>
                                                <p className="text-sky-600 font-medium text-sm mt-1">{exp.position || "-"}</p>
                                                {exp.description ? (
                                                    <p className="text-gray-600 text-sm mt-2 whitespace-pre-wrap">{exp.description}</p>
                                                ) : null}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            ) : null}
                            {viewingResumeData.skills?.length > 0 ? (
                                <div className="mt-6">
                                    <h4 className="text-base font-semibold text-gray-800 mb-2">기술 스택</h4>
                                    <div className="flex flex-wrap gap-2">
                                        {viewingResumeData.skills.map((s, i) => (
                                            <Badge key={i} variant="secondary" className="font-normal">
                                                {s}
                                            </Badge>
                                        ))}
                                    </div>
                                </div>
                            ) : null}
                            {!viewingResumeData.summary && !viewingResumeData.experiences?.length && !viewingResumeData.skills?.length ? (
                                <p className="text-gray-500 text-sm py-4">작성된 내용이 없습니다.</p>
                            ) : null}
                        </div>
                    ) : null}
                    <DialogFooter className="gap-2 sm:gap-0">
                        <Button variant="outline" onClick={closeViewModal}>
                            닫기
                        </Button>
                        {viewingResumeId != null && (
                            <Button
                                onClick={() => {
                                    if (viewingResumeId != null) handleEditResume(viewingResumeId);
                                    closeViewModal();
                                }}
                                className="bg-sky-500 hover:bg-sky-600 text-white"
                            >
                                <Layout className="h-4 w-4 mr-2" />
                                수정하기
                            </Button>
                        )}
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
};

export default ResumePage;
