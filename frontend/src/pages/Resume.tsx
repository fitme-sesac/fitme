import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
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
    Sparkles,
    User
} from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { ResumeTemplateSelector } from "@/components/resume/ResumeTemplateSelector";
import { ResumeEditor, type ResumeData } from "@/components/resume/ResumeEditor";
import { AIProofreadingPanel } from "@/components/resume/AIProofreadingPanel";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

// 저장된 이력서 목록 (목데이터)
const savedResumes = [
    {
        id: "1",
        title: "프론트엔드 개발자 이력서",
        template: "professional",
        updatedAt: "2026-01-28",
        isDefault: true,
    },
    {
        id: "2",
        title: "스타트업 지원용",
        template: "creative",
        updatedAt: "2026-01-25",
        isDefault: false,
    },
];

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

const ResumePage = () => {
    const { user, loading } = useAuth();
    const { toast } = useToast();
    const [selectedTemplate, setSelectedTemplate] = useState("basic");
    const [resumeData, setResumeData] = useState<ResumeData>(initialResumeData);
    const [activeTab, setActiveTab] = useState("list");

    const handleSaveResume = () => {
        toast({
            title: "이력서 저장 완료",
            description: "이력서가 성공적으로 저장되었습니다.",
        });
    };

    const handleApplySuggestion = (newContent: string) => {
        setResumeData({
            ...resumeData,
            summary: newContent,
        });
    };

    const handleCreateNew = () => {
        setResumeData(initialResumeData);
        setActiveTab("edit");
    };

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

                            {/* 이력서 목록 */}
                            <TabsContent value="list" className="mt-6">
                                <div className="grid gap-4">
                                    {savedResumes.map((resume) => (
                                        <Card key={resume.id} className="border-none shadow-sm hover:shadow-md transition-all bg-white overflow-hidden group">
                                            <CardContent className="flex items-center justify-between p-6">
                                                <div className="flex items-center gap-5">
                                                    <div className="h-14 w-14 rounded-xl bg-sky-50 flex items-center justify-center group-hover:bg-sky-100 transition-colors">
                                                        <FileText className="h-7 w-7 text-sky-500" />
                                                    </div>
                                                    <div>
                                                        <div className="flex items-center gap-3">
                                                            <h3 className="font-bold text-lg text-gray-900">{resume.title}</h3>
                                                            {resume.isDefault && (
                                                                <Badge className="bg-sky-500 text-white border-none text-[10px] px-2 py-0">
                                                                    기본
                                                                </Badge>
                                                            )}
                                                        </div>
                                                        <p className="text-sm text-gray-400 mt-1">
                                                            마지막 수정: {resume.updatedAt}
                                                        </p>
                                                    </div>
                                                </div>
                                                <div className="flex items-center gap-3">
                                                    <Button variant="outline" size="sm" className="hidden md:flex gap-2 border-gray-200 text-gray-600 hover:bg-gray-50">
                                                        <Eye className="h-4 w-4" />
                                                        미리보기
                                                    </Button>
                                                    <Button variant="outline" size="sm" className="hidden md:flex gap-2 border-gray-200 text-gray-600 hover:bg-gray-50">
                                                        <Download className="h-4 w-4" />
                                                        다운로드
                                                    </Button>
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <Button variant="ghost" size="icon" className="text-gray-400 hover:text-gray-600">
                                                                <MoreVertical className="h-4 w-4" />
                                                            </Button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end" className="w-48 p-2">
                                                            <DropdownMenuItem onClick={() => setActiveTab("edit")} className="rounded-md">
                                                                수정하기
                                                            </DropdownMenuItem>
                                                            <DropdownMenuItem className="rounded-md">기본 이력서로 설정</DropdownMenuItem>
                                                            <DropdownMenuItem className="rounded-md">복사하기</DropdownMenuItem>
                                                            <DropdownMenuItem className="text-destructive rounded-md focus:bg-destructive/10">
                                                                <Trash2 className="h-4 w-4 mr-2" />
                                                                삭제하기
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                </div>
                                            </CardContent>
                                        </Card>
                                    ))}

                                    {/* 빈 상태 */}
                                    {savedResumes.length === 0 && (
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
    );
};

export default ResumePage;
