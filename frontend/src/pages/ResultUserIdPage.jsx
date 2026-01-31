import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Copy, ArrowLeft, Loader2 } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { getUserIdResult } from "@/api/auth";

export default function ResultUserIdPage() {
    const navigate = useNavigate();
    const { toast } = useToast();
    const [searchParams] = useSearchParams();

    const initial = useMemo(() => searchParams.get("userid") || "", [searchParams]);
    const [userId, setUserId] = useState(initial);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (userId) return;
        // 쿼리 파라미터가 없으면 서버의 VERIFIED 쿠키에서 조회 시도
        setLoading(true);
        getUserIdResult()
            .then((res) => {
                const id = res?.userId || res?.userid || "";
                if (id) setUserId(id);
            })
            .catch(() => {
                // silent
            })
            .finally(() => setLoading(false));
    }, [userId]);

    const handleCopy = async () => {
        if (!userId) return;
        try {
            await navigator.clipboard.writeText(userId);
            toast({ title: "복사됨", description: "아이디가 클립보드에 복사되었습니다." });
        } catch {
            toast({ title: "복사 실패", description: "복사에 실패했습니다.", variant: "destructive" });
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
            <div className="w-full max-w-md">
                <Card className="bg-white/95 backdrop-blur-md rounded-2xl shadow-xl border border-slate-200">
                    <CardHeader className="space-y-1">
                        <div className="flex items-center gap-2">
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => navigate("/auth/find-id")}
                                className="-ml-3 h-8 w-8 rounded-full"
                            >
                                <ArrowLeft className="h-4 w-4" />
                            </Button>
                            <CardTitle className="text-xl font-bold">아이디 찾기 결과</CardTitle>
                        </div>
                        <CardDescription>인증이 완료된 계정의 아이디를 표시합니다.</CardDescription>
                    </CardHeader>
                    <CardContent className="pt-2">
                        {loading ? (
                            <div className="flex items-center justify-center gap-2 py-10 text-slate-600">
                                <Loader2 className="h-4 w-4 animate-spin" />
                                <span className="text-sm">조회 중...</span>
                            </div>
                        ) : userId ? (
                            <div className="text-center space-y-6 py-6">
                                <div className="space-y-2">
                                    <p className="text-sm text-slate-600">회원님의 아이디는 아래와 같습니다.</p>
                                    <div className="p-4 bg-slate-50 rounded-lg border flex items-center justify-center gap-2">
                                        <span className="text-xl font-bold tracking-wide text-slate-900">{userId}</span>
                                        <Button variant="ghost" size="sm" onClick={handleCopy}>
                                            <Copy className="h-4 w-4" />
                                        </Button>
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <Button
                                        className="w-full btn-gradient-primary h-12 text-lg font-bold shadow-md"
                                        onClick={() => navigate("/auth?tab=login")}
                                    >
                                        로그인하러 가기
                                    </Button>
                                    <Button
                                        variant="outline"
                                        className="w-full h-12"
                                        onClick={() => navigate("/auth/find-password")}
                                    >
                                        비밀번호 찾기
                                    </Button>
                                </div>
                            </div>
                        ) : (
                            <div className="text-center space-y-4 py-8">
                                <p className="text-sm text-slate-600">
                                    조회할 아이디 정보가 없습니다. 다시 인증을 진행해주세요.
                                </p>
                                <Button
                                    className="w-full btn-gradient-primary h-12 text-lg font-bold shadow-md"
                                    onClick={() => navigate("/auth/find-id")}
                                >
                                    아이디 찾기 다시하기
                                </Button>
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
