import { Megaphone, X } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";

interface AdBannerProps {
  position: "top" | "side";
}

export function AdBanner({ position }: AdBannerProps) {
  const [isVisible, setIsVisible] = useState(true);

  if (!isVisible) return null;

  if (position === "top") {
    return (
      <div className="relative mb-6 rounded-xl bg-gradient-to-r from-primary/10 via-info/10 to-accent/10 border border-primary/20 p-4">
        <button
          onClick={() => setIsVisible(false)}
          className="absolute right-2 top-2 p-1 rounded-full hover:bg-muted transition-colors"
        >
          <X className="h-4 w-4 text-muted-foreground" />
        </button>
        <div className="flex items-center gap-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/20">
            <Megaphone className="h-5 w-5 text-primary" />
          </div>
          <div className="flex-1">
            <p className="font-medium text-foreground">
              🎯 프리미엄 채용광고로 더 많은 인재를 만나보세요!
            </p>
            <p className="text-sm text-muted-foreground">
              상위 노출 광고 이용 시 지원률이 평균 3배 증가합니다.
            </p>
          </div>
          <button className="btn-gradient-primary px-4 py-2 rounded-lg text-sm hidden sm:block">
            광고 시작하기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="sticky top-6 space-y-4">
      {/* 사이드 배너 1 */}
      <div className="rounded-xl border border-border bg-card p-4">
        <div className="aspect-[4/5] rounded-lg bg-gradient-to-br from-primary/20 via-info/20 to-accent/20 flex flex-col items-center justify-center p-4 text-center">
          <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-primary/20">
            <Megaphone className="h-6 w-6 text-primary" />
          </div>
          <h3 className="font-bold text-foreground mb-2">광고 영역</h3>
          <p className="text-sm text-muted-foreground mb-4">
            여기에 프리미엄 광고가 표시됩니다
          </p>
          <span className="text-xs px-2 py-1 rounded-full bg-muted text-muted-foreground">
            AD
          </span>
        </div>
      </div>

      {/* 사이드 배너 2 */}
      <div className="rounded-xl border border-accent/30 bg-gradient-to-br from-accent/5 to-warning/5 p-4">
        <p className="font-semibold text-foreground mb-2">🔥 핫딜 광고</p>
        <p className="text-sm text-muted-foreground mb-3">
          이번 주 채용광고 50% 할인 중!
        </p>
        <button className="w-full py-2 rounded-lg bg-accent text-accent-foreground text-sm font-medium hover:opacity-90 transition-opacity">
          자세히 보기
        </button>
      </div>
    </div>
  );
}
