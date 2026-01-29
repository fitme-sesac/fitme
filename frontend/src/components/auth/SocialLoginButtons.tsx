import { useState } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";

export function SocialLoginButtons() {
  const [googleLoading, setGoogleLoading] = useState(false);
  const [kakaoLoading, setKakaoLoading] = useState(false);
  const [naverLoading, setNaverLoading] = useState(false);
  const { signInWithGoogle, signInWithKakao, signInWithNaver } = useAuth();

  const handleGoogleLogin = () => {
    setGoogleLoading(true);
    signInWithGoogle();
  };

  const handleKakaoLogin = () => {
    setKakaoLoading(true);
    signInWithKakao();
  };

  const handleNaverLogin = () => {
    setNaverLoading(true);
    signInWithNaver();
  };

  const isAnyLoading = googleLoading || kakaoLoading || naverLoading;

  return (
    <div className="flex flex-col gap-3 mt-3 w-full">
      {/* Google */}
      <Button
        type="button"
        variant="outline"
        className="w-full relative h-12 rounded-lg border-gray-200 hover:bg-gray-50 bg-white"
        onClick={handleGoogleLogin}
        disabled={isAnyLoading}
      >
        <div className="absolute left-4 flex items-center justify-center">
          {googleLoading ? (
            <Loader2 className="h-5 w-5 animate-spin text-gray-600" />
          ) : (
            <svg className="h-5 w-5" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              />
            </svg>
          )}
        </div>
        <span className="text-base font-medium text-gray-700">Google로 계속하기</span>
      </Button>

      {/* Kakao */}
      <Button
        type="button"
        className="w-full relative h-12 rounded-lg bg-[#FEE500] text-[#191919] hover:bg-[#FEE500]/90 border border-[#FEE500]"
        onClick={handleKakaoLogin}
        disabled={isAnyLoading}
      >
        <div className="absolute left-4 flex items-center justify-center">
          {kakaoLoading ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 3C6.48 3 2 6.48 2 10.5c0 2.55 1.55 4.8 4 6.18V21l4.5-2.5c.5.05 1 .08 1.5.08 5.52 0 10-3.48 10-7.58S17.52 3 12 3z" />
            </svg>
          )}
        </div>
        <span className="text-base font-medium">카카오로 계속하기</span>
      </Button>

      {/* Naver */}
      <Button
        type="button"
        className="w-full relative h-12 rounded-lg bg-[#03C75A] text-white hover:bg-[#03C75A]/90 border border-[#03C75A]"
        onClick={handleNaverLogin}
        disabled={isAnyLoading}
      >
        <div className="absolute left-4 flex items-center justify-center">
          {naverLoading ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
              <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727z" />
            </svg>
          )}
        </div>
        <span className="text-base font-medium">네이버로 계속하기</span>
      </Button>
    </div>
  );
}
