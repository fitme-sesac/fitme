import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { CheckCircle2, XCircle, Loader2 } from "lucide-react";

export function PaymentResultModal() {
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const [result, setResult] = useState<"success" | "fail" | null>(null);

    useEffect(() => {
        const isSuccess = searchParams.get("payment_success") === "true";
        const isFail = searchParams.get("payment_fail") === "true";

        if (isSuccess) {
            setResult("success");
            setOpen(true);
        } else if (isFail) {
            setResult("fail");
            setOpen(true);
        } else {
            setOpen(false);
        }
    }, [searchParams]);

    const handleClose = () => {
        setOpen(false);
        // URL 파라미터 제거
        const newParams = new URLSearchParams(searchParams);
        newParams.delete("payment_success");
        newParams.delete("payment_fail");
        newParams.delete("orderId");
        newParams.delete("amount");
        newParams.delete("code");
        newParams.delete("message");
        navigate({ search: newParams.toString() }, { replace: true });
    };

    if (!result) return null;

    return (
        <Dialog open={open} onOpenChange={handleClose}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle className="text-center">
                        {result === "success" ? "결제 완료" : "결제 실패"}
                    </DialogTitle>
                </DialogHeader>

                <div className="flex flex-col items-center justify-center py-6 space-y-4">
                    {result === "success" ? (
                        <>
                            <div className="h-16 w-16 bg-emerald-100 rounded-full flex items-center justify-center">
                                <CheckCircle2 className="h-8 w-8 text-emerald-600" />
                            </div>
                            <div className="text-center space-y-1">
                                <p className="text-lg font-medium">크레딧 충전이 완료되었습니다!</p>
                                <p className="text-sm text-slate-500">
                                    주문번호: {searchParams.get("orderId")}<br />
                                    결제금액: {Number(searchParams.get("amount") || 0).toLocaleString()}원
                                </p>
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="h-16 w-16 bg-red-100 rounded-full flex items-center justify-center">
                                <XCircle className="h-8 w-8 text-red-600" />
                            </div>
                            <div className="text-center space-y-1">
                                <p className="text-lg font-medium text-red-600">결제 처리에 실패했습니다</p>
                                <p className="text-sm text-slate-500">
                                    {searchParams.get("message") || "잠시 후 다시 시도해주세요."}
                                </p>
                            </div>
                        </>
                    )}
                </div>

                <DialogFooter className="sm:justify-center">
                    <Button onClick={handleClose} className="w-full sm:w-auto min-w-[120px]">
                        확인
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
