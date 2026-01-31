import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import {
    Bot,
    Sparkles,
    CheckCircle2,
    Wand2,
    SpellCheck,
    FileEdit,
    Lightbulb,
    Loader2
} from "lucide-react";

interface AIProofreadingPanelProps {
    content: string;
    onApplySuggestion: (newContent: string) => void;
}

interface Suggestion {
    id: string;
    type: "spelling" | "grammar" | "improvement" | "tip";
    original?: string;
    suggestion: string;
    explanation: string;
}

export function AIProofreadingPanel({ content, onApplySuggestion }: AIProofreadingPanelProps) {
    const [isAnalyzing, setIsAnalyzing] = useState(false);
    const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
    const [selectedText, setSelectedText] = useState("");
    const [improvedText, setImprovedText] = useState("");

    // Mock AI analysis - in real implementation, this would call an AI API
    const analyzeContent = async () => {
        setIsAnalyzing(true);

        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 1500));

        // Mock suggestions
        const mockSuggestions: Suggestion[] = [
            {
                id: "1",
                type: "spelling",
                original: "개발자",
                suggestion: "개발자",
                explanation: "맞춤법 검사 완료 - 오류 없음",
            },
            {
                id: "2",
                type: "improvement",
                suggestion: "성과 중심으로 경력을 기술하세요",
                explanation: "구체적인 수치나 성과를 포함하면 더 설득력 있는 이력서가 됩니다.",
            },
            {
                id: "3",
                type: "tip",
                suggestion: "핵심 역량을 상단에 배치하세요",
                explanation: "채용 담당자가 가장 먼저 보는 부분이므로 핵심 역량을 강조하세요.",
            },
            {
                id: "4",
                type: "grammar",
                original: "하였습니다",
                suggestion: "했습니다",
                explanation: "간결한 문체가 이력서에 더 적합합니다.",
            },
        ];

        setSuggestions(mockSuggestions);
        setIsAnalyzing(false);
    };

    const improveText = async () => {
        if (!selectedText.trim()) return;

        setIsAnalyzing(true);
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Mock improvement
        setImprovedText(
            `${selectedText}\n\n[AI 첨삭 결과]\n- 더 구체적인 수치를 추가해보세요\n- 성과 중심으로 작성하면 좋겠습니다\n- 능동적인 동사를 사용해보세요`
        );
        setIsAnalyzing(false);
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
                        disabled={isAnalyzing}
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
