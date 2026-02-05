import { Link } from "react-router-dom";
import { AlertTriangle, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AccountSuspendedPage() {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 px-4">
            <div className="max-w-md w-full text-center space-y-6">
                <div className="flex justify-center">
                    <div className="rounded-full bg-amber-100 p-4">
                        <AlertTriangle className="h-12 w-12 text-amber-600" />
                    </div>
                </div>
                <h1 className="text-2xl font-bold text-gray-900">
                    계정이 정지되었습니다
                </h1>
                <p className="text-gray-600">
                    이용 약관 또는 서비스 정책 위반으로 인해 계정 이용이 제한되었습니다.
                    문의 사항이 있으시면 고객센터로 연락해 주세요.
                </p>
                <div className="rounded-lg bg-gray-100 p-4 text-sm text-gray-700 text-left">
                    <p className="font-medium mb-1">고객센터</p>
                    <p>이메일: support@fitme.example.com</p>
                    <p>운영 시간: 평일 09:00 - 18:00</p>
                </div>
                <Button asChild variant="outline" className="w-full">
                    <Link to="/auth" className="inline-flex items-center gap-2">
                        <ArrowLeft className="h-4 w-4" />
                        로그인 페이지로 돌아가기
                    </Link>
                </Button>
            </div>
        </div>
    );
}
