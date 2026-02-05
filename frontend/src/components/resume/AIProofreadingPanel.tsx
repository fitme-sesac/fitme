import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {
    Bot,
    Sparkles,
    CheckCircle2,
    Wand2,
    SpellCheck,
    FileEdit,
    Lightbulb,
    Loader2,
    Briefcase,
    RefreshCw,
    Heart
} from "lucide-react";
import { requestAiAnalysis, requestAiAnalysisForJob, getAiAnalysisStatus } from "@/api/resumes";
import { getMyScrapedJobs, scrapJob } from "@/api/jobs";
import { useQueryClient, useQuery } from "@tanstack/react-query";
import { useAuth } from "@/contexts/AuthContext";

interface AIProofreadingPanelProps {
    content: string;
    onApplySuggestion: (newContent: string) => void;
    resumeId?: number | null;
}

interface Suggestion {
    id: string;
    type: "spelling" | "grammar" | "improvement" | "tip";
    original?: string;
    suggestion: string;
    explanation: string;
}

interface ScrapedJob {
    id?: number;
    jobId?: number;
    title?: string;
    companyName?: string;
    job?: {
        id?: number;
        title?: string;
        companyName?: string;
    };
}

export function AIProofreadingPanel({ content, onApplySuggestion, resumeId }: AIProofreadingPanelProps) {
    const queryClient = useQueryClient();
    const { user } = useAuth();
    const [isAnalyzing, setIsAnalyzing] = useState(false);
    const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
    const [selectedText, setSelectedText] = useState("");
    const [improvedText, setImprovedText] = useState("");
    const [analysisStatus, setAnalysisStatus] = useState<string | null>(null);
    const [statusMessage, setStatusMessage] = useState<string>("");
    
    // 채용공고 맞춤 첨삭 관련 상태
    const [selectedJobId, setSelectedJobId] = useState<string>("");
    const [isJobMatchAnalyzing, setIsJobMatchAnalyzing] = useState(false);
    const [unscrapingJobId, setUnscrapingJobId] = useState<string | null>(null);
    const [unscrappedJobIds, setUnscrappedJobIds] = useState<Set<string>>(new Set()); // 해제된 공고 ID 추적

    // 스크랩한 공고 목록 불러오기 (React Query 사용)
    const { data: scrapedJobsData, isLoading: loadingJobs } = useQuery({
        queryKey: ["myScrapedJobs"],
        queryFn: async () => {
            const response = await getMyScrapedJobs({ page: 0, size: 50 });
            const list = Array.isArray(response?.content) 
                ? response.content 
                : (Array.isArray(response) ? response : []);
            return list as ScrapedJob[];
        },
        enabled: !!user, // 로그인한 경우에만 조회
        staleTime: 1000 * 30, // 30초간 캐시 유지
    });

    const scrapedJobs = scrapedJobsData ?? [];

    // AI 분석 상태 폴링
    useEffect(() => {
        let interval: NodeJS.Timeout | null = null;
        
        if (resumeId && (analysisStatus === "PENDING" || analysisStatus === "PROCESSING")) {
            interval = setInterval(async () => {
                try {
                    const response = await getAiAnalysisStatus(resumeId);
                    const status = response?.data?.status || response?.status;
                    const message = response?.data?.message || response?.message;
                    setAnalysisStatus(status);
                    setStatusMessage(message || "");
                    
                    if (status === "COMPLETED" || status === "FAILED") {
                        setIsAnalyzing(false);
                        setIsJobMatchAnalyzing(false);
                        if (interval) clearInterval(interval);
                    }
                } catch (error) {
                    console.error("상태 조회 실패:", error);
                }
            }, 3000);
        }

        return () => {
            if (interval) clearInterval(interval);
        };
    }, [resumeId, analysisStatus]);

    // 기본 AI 분석 요청
    const analyzeContent = async () => {
        if (!resumeId) {
            setStatusMessage("이력서를 먼저 저장해주세요.");
            return;
        }

        setIsAnalyzing(true);
        setAnalysisStatus("PENDING");
        setStatusMessage("AI 분석 요청 중...");

        try {
            await requestAiAnalysis(resumeId, "detailed");
            setAnalysisStatus("PENDING");
            setStatusMessage("AI 분석이 요청되었습니다. 잠시만 기다려주세요...");
        } catch (error: unknown) {
            console.error("AI 분석 요청 실패:", error);
            setIsAnalyzing(false);
            setAnalysisStatus("FAILED");
            const errorMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message || "AI 분석 요청에 실패했습니다.";
            setStatusMessage(errorMessage);
        }
    };

    // 채용공고 맞춤 AI 분석 요청
    const analyzeForJob = async () => {
        if (!resumeId || !selectedJobId) {
            return;
        }

        setIsJobMatchAnalyzing(true);
        setAnalysisStatus("PENDING");
        setStatusMessage("채용공고 맞춤 AI 분석 요청 중...");

        try {
            await requestAiAnalysisForJob(resumeId, parseInt(selectedJobId), "detailed");
            setAnalysisStatus("PENDING");
            setStatusMessage("채용공고 맞춤 AI 분석이 요청되었습니다. 잠시만 기다려주세요...");
        } catch (error: unknown) {
            console.error("채용공고 맞춤 AI 분석 요청 실패:", error);
            setIsJobMatchAnalyzing(false);
            setAnalysisStatus("FAILED");
            const errorMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message || "AI 분석 요청에 실패했습니다.";
            setStatusMessage(errorMessage);
        }
    };

    const improveText = async () => {
        if (!selectedText.trim()) return;
        
        // 문장 개선 기능은 준비 중
        setImprovedText("문장 개선 기능은 준비 중입니다. AI 분석 시작 버튼을 사용해주세요.");
    };

    const getJobDisplayName = (job: ScrapedJob) => {
        const jobData = job.job || job;
        const title = jobData.title || "제목 없음";
        const company = jobData.companyName || "";
        return company ? `${company} - ${title}` : title;
    };

    const getJobId = (job: ScrapedJob) => {
        return String(job.job?.id || job.jobId || job.id || "");
    };

    // 관심 공고 해제 (목록에서 즉시 사라지지 않고, 하트만 빈 상태로 변경)
    const handleUnscrapJob = async (jobId: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!jobId) return;
        
        setUnscrapingJobId(jobId);
        try {
            await scrapJob(parseInt(jobId)); // 토글 방식
            // 해제된 공고 ID 추적 (목록에서 즉시 제거하지 않음)
            setUnscrappedJobIds((prev) => new Set(prev).add(jobId));
            // 선택된 공고였다면 선택 해제
            if (selectedJobId === jobId) {
                setSelectedJobId("");
            }
            // React Query 캐시 무효화 (다음 페이지 이동/새로고침 시 반영)
            queryClient.invalidateQueries({ queryKey: ["scrapedJobs"] });
            queryClient.invalidateQueries({ queryKey: ["myScrapedJobs"] });
            queryClient.invalidateQueries({ queryKey: ["profileSummary"] });
            queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
        } catch (error) {
            console.error("관심 공고 해제 실패:", error);
        } finally {
            setUnscrapingJobId(null);
        }
    };

    // 관심 공고 다시 추가 (해제 취소)
    const handleRescrapJob = async (jobId: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!jobId) return;
        
        setUnscrapingJobId(jobId);
        try {
            await scrapJob(parseInt(jobId)); // 토글 방식으로 다시 추가
            // 해제 목록에서 제거
            setUnscrappedJobIds((prev) => {
                const newSet = new Set(prev);
                newSet.delete(jobId);
                return newSet;
            });
            queryClient.invalidateQueries({ queryKey: ["scrapedJobs"] });
            queryClient.invalidateQueries({ queryKey: ["myScrapedJobs"] });
            queryClient.invalidateQueries({ queryKey: ["profileSummary"] });
            queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
        } catch (error) {
            console.error("관심 공고 추가 실패:", error);
        } finally {
            setUnscrapingJobId(null);
        }
    };

    const getSuggestionIcon = (type: string) => {
        switch (type) {
            case "spelling":
                return <SpellCheck className="h-4 w-4" />;
            case "grammar":
                return <FileEdit className="h-4 w-4" />;
            case "improvement":
                return <Wand2 className="h-4 w-4" />;
            case "tip":
                return <Lightbulb className="h-4 w-4" />;
            default:
                return <CheckCircle2 className="h-4 w-4" />;
        }
    };

    const getSuggestionColor = (type: string) => {
        switch (type) {
            case "spelling":
                return "text-red-500";
            case "grammar":
                return "text-yellow-500";
            case "improvement":
                return "text-blue-500";
            case "tip":
                return "text-green-500";
            default:
                return "text-muted-foreground";
        }
    };

    const getSuggestionBadge = (type: string) => {
        switch (type) {
            case "spelling":
                return "맞춤법";
            case "grammar":
                return "문법";
            case "improvement":
                return "개선";
            case "tip":
                return "팁";
            default:
                return type;
        }
    };

    return (
        <div className="space-y-4">
            {/* AI 분석 버튼 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <Bot className="h-5 w-5 text-primary" />
                        AI 이력서 첨삭
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        AI가 이력서를 분석하여 맞춤법, 문법, 표현 개선점을 제안합니다.
                    </p>
                    <Button
                        onClick={analyzeContent}
                        disabled={isAnalyzing || isJobMatchAnalyzing}
                        className="w-full gap-2"
                    >
                        {isAnalyzing ? (
                            <>
                                <Loader2 className="h-4 w-4 animate-spin" />
                                분석 중...
                            </>
                        ) : (
                            <>
                                <Sparkles className="h-4 w-4" />
                                AI 분석 시작
                            </>
                        )}
                    </Button>
                </CardContent>
            </Card>

            {/* 채용공고 맞춤 AI 첨삭 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <Briefcase className="h-5 w-5 text-primary" />
                        채용공고 맞춤 AI 첨삭
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <p className="text-sm text-muted-foreground">
                        관심 등록한 채용공고에 맞춰 이력서를 어떻게 수정하면 좋을지 AI가 분석합니다.
                    </p>
                    
                    {loadingJobs ? (
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <Loader2 className="h-4 w-4 animate-spin" />
                            관심 공고 불러오는 중...
                        </div>
                    ) : scrapedJobs.length > 0 ? (
                        <>
                            <div className="space-y-2">
                                <label className="text-sm font-medium flex items-center gap-2">
                                    <Heart className="h-4 w-4 text-red-500 fill-red-500" />
                                    관심 공고 선택
                                </label>
                                <div className="space-y-2 max-h-[200px] overflow-y-auto border rounded-lg p-2">
                                    {scrapedJobs.map((job, index) => {
                                        const jobId = getJobId(job);
                                        const isSelected = selectedJobId === jobId;
                                        const isUnscraping = unscrapingJobId === jobId;
                                        const isUnscrapped = unscrappedJobIds.has(jobId); // 해제된 상태인지 확인
                                        return (
                                            <div 
                                                key={jobId || index}
                                                className={`flex items-center justify-between p-2 rounded-lg cursor-pointer transition-all ${
                                                    isSelected 
                                                        ? 'bg-primary/10 border border-primary' 
                                                        : 'hover:bg-muted border border-transparent'
                                                } ${isUnscrapped ? 'opacity-60' : ''}`}
                                                onClick={() => !isUnscrapped && setSelectedJobId(jobId)}
                                            >
                                                <span className={`text-sm flex-1 truncate ${isSelected ? 'font-medium' : ''}`}>
                                                    {getJobDisplayName(job)}
                                                </span>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    className="h-8 w-8 p-0 ml-2 hover:bg-red-50 shrink-0"
                                                    onClick={(e) => isUnscrapped ? handleRescrapJob(jobId, e) : handleUnscrapJob(jobId, e)}
                                                    disabled={isUnscraping}
                                                    title={isUnscrapped ? "관심 추가" : "관심 해제"}
                                                >
                                                    {isUnscraping ? (
                                                        <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
                                                    ) : isUnscrapped ? (
                                                        <Heart className="h-4 w-4 text-gray-400" />
                                                    ) : (
                                                        <Heart className="h-4 w-4 text-red-500 fill-red-500" />
                                                    )}
                                                </Button>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                            <Button
                                onClick={analyzeForJob}
                                disabled={!selectedJobId || !resumeId || isJobMatchAnalyzing || isAnalyzing}
                                className="w-full gap-2"
                                variant="outline"
                            >
                                {isJobMatchAnalyzing ? (
                                    <>
                                        <Loader2 className="h-4 w-4 animate-spin" />
                                        맞춤 분석 중...
                                    </>
                                ) : (
                                    <>
                                        <Wand2 className="h-4 w-4" />
                                        채용공고 맞춤 첨삭 시작
                                    </>
                                )}
                            </Button>
                            {!resumeId && (
                                <p className="text-xs text-amber-600">
                                    * 이력서를 저장한 후 맞춤 첨삭을 사용할 수 있습니다.
                                </p>
                            )}
                        </>
                    ) : (
                        <div className="text-center py-4 text-muted-foreground">
                            <Heart className="h-8 w-8 mx-auto mb-2 text-gray-300" />
                            <p className="text-sm">관심 등록한 채용공고가 없습니다.</p>
                            <p className="text-xs mt-1">채용공고 페이지에서 관심 버튼을 눌러 등록해주세요.</p>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* AI 분석 상태 표시 */}
            {analysisStatus && (
                <Card>
                    <CardContent className="py-4">
                        <div className="flex items-center gap-3">
                            {(analysisStatus === "PENDING" || analysisStatus === "PROCESSING") && (
                                <Loader2 className="h-5 w-5 animate-spin text-primary" />
                            )}
                            {analysisStatus === "COMPLETED" && (
                                <CheckCircle2 className="h-5 w-5 text-green-500" />
                            )}
                            {analysisStatus === "FAILED" && (
                                <span className="h-5 w-5 text-red-500">!</span>
                            )}
                            <div>
                                <p className="text-sm font-medium">
                                    {analysisStatus === "PENDING" && "분석 대기 중"}
                                    {analysisStatus === "PROCESSING" && "AI가 분석 중입니다"}
                                    {analysisStatus === "COMPLETED" && "분석 완료"}
                                    {analysisStatus === "FAILED" && "분석 실패"}
                                </p>
                                {statusMessage && (
                                    <p className="text-xs text-muted-foreground">{statusMessage}</p>
                                )}
                            </div>
                            {(analysisStatus === "COMPLETED" || analysisStatus === "FAILED") && (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    className="ml-auto"
                                    onClick={() => {
                                        setAnalysisStatus(null);
                                        setStatusMessage("");
                                    }}
                                >
                                    <RefreshCw className="h-4 w-4" />
                                </Button>
                            )}
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* 분석 결과 */}
            {suggestions.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2 text-lg">
                            <CheckCircle2 className="h-5 w-5 text-green-500" />
                            분석 결과
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                        {suggestions.map((suggestion) => (
                            <div
                                key={suggestion.id}
                                className="p-3 rounded-lg border bg-muted/30 space-y-2"
                            >
                                <div className="flex items-center gap-2">
                                    <span className={getSuggestionColor(suggestion.type)}>
                                        {getSuggestionIcon(suggestion.type)}
                                    </span>
                                    <Badge variant="outline" className="text-xs">
                                        {getSuggestionBadge(suggestion.type)}
                                    </Badge>
                                </div>
                                {suggestion.original && (
                                    <div className="text-sm">
                                        <span className="line-through text-muted-foreground">
                                            {suggestion.original}
                                        </span>
                                        <span className="mx-2">→</span>
                                        <span className="text-primary font-medium">
                                            {suggestion.suggestion}
                                        </span>
                                    </div>
                                )}
                                {!suggestion.original && (
                                    <p className="text-sm font-medium">{suggestion.suggestion}</p>
                                )}
                                <p className="text-xs text-muted-foreground">
                                    {suggestion.explanation}
                                </p>
                            </div>
                        ))}
                    </CardContent>
                </Card>
            )}

            {/* 텍스트 개선 도구 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <Wand2 className="h-5 w-5 text-primary" />
                        문장 개선하기
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-2">
                        <p className="text-sm text-muted-foreground">
                            개선하고 싶은 문장을 입력하세요.
                        </p>
                        <Textarea
                            value={selectedText}
                            onChange={(e) => setSelectedText(e.target.value)}
                            placeholder="예: 저는 3년간 프론트엔드 개발을 하였습니다."
                            className="min-h-[80px]"
                        />
                    </div>
                    <Button
                        onClick={improveText}
                        disabled={isAnalyzing || !selectedText.trim()}
                        variant="outline"
                        className="w-full gap-2"
                    >
                        {isAnalyzing ? (
                            <>
                                <Loader2 className="h-4 w-4 animate-spin" />
                                개선 중...
                            </>
                        ) : (
                            <>
                                <Wand2 className="h-4 w-4" />
                                AI로 문장 개선
                            </>
                        )}
                    </Button>

                    {improvedText && (
                        <div className="p-3 rounded-lg bg-muted border border-border">
                            <p className="text-sm whitespace-pre-wrap">{improvedText}</p>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* 맞춤법 검사 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <SpellCheck className="h-5 w-5 text-primary" />
                        맞춤법 검사
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex items-center gap-3 p-4 rounded-lg bg-primary/10 text-primary">
                        <CheckCircle2 className="h-5 w-5" />
                        <span className="text-sm">맞춤법 검사가 자동으로 적용됩니다.</span>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
