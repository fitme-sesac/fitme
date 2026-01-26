import { MapPin, Clock, Banknote, Heart, Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Link } from "react-router-dom";

/**
 * 채용공고 카드 컴포넌트
 * @param {Object} props - 채용공고 정보
 * @param {number} props.jobId - 채용공고 ID
 * @param {string} props.companyName - 회사명
 * @param {string} props.companyLogoUrl - 회사 로고 URL
 * @param {string} props.title - 공고 제목
 * @param {string} props.location - 근무지
 * @param {string} props.salaryDisplay - 연봉 표시
 * @param {string} props.stack - 기술 스택 (쉼표 구분 문자열 또는 배열)
 * @param {string} props.createdAt - 등록일
 * @param {boolean} props.isAd - 광고 여부
 * @param {Object} props.matchInfo - AI 매칭 정보
 */
export function JobCard({
  jobId,
  companyName,
  companyLogoUrl,
  title,
  location,
  salaryDisplay,
  stack,
  createdAt,
  isAd = false,
  matchInfo,
}) {
  // 스킬 배열 처리 (문자열 또는 배열 지원)
  const skills = Array.isArray(stack) 
    ? stack 
    : (stack?.split(",").map(s => s.trim()).filter(Boolean) || []);

  // 등록일 포맷팅
  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return "오늘";
    if (diffDays === 1) return "1일 전";
    if (diffDays < 7) return `${diffDays}일 전`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
    return `${Math.floor(diffDays / 30)}개월 전`;
  };

  // 로고 표시 (URL이 없으면 회사명 첫 글자)
  const logoDisplay = companyLogoUrl ? (
    <img 
      src={companyLogoUrl} 
      alt={companyName} 
      className="h-12 w-12 rounded-xl object-cover"
    />
  ) : (
    <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-lg font-bold text-primary">
      {companyName?.charAt(0) || "?"}
    </div>
  );

  return (
    <Link to={`/jobs/${jobId}`}>
      <div className={cn(
        "group relative rounded-2xl bg-card p-5 card-hover border cursor-pointer",
        isAd && "ring-2 ring-accent/30"
      )}>
        {/* 광고 배지 */}
        {isAd && (
          <div className="absolute -top-2 -right-2 rounded-full bg-accent px-2 py-0.5 text-[10px] font-bold text-accent-foreground">
            AD
          </div>
        )}

        {/* 상단: 회사 정보 */}
        <div className="mb-4 flex items-start justify-between">
          <div className="flex items-center gap-3">
            {logoDisplay}
            <div>
              <p className="font-medium text-sm text-muted-foreground">{companyName}</p>
              <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-1">
                {title}
              </h3>
            </div>
          </div>
          <Button 
            variant="ghost" 
            size="icon" 
            className="h-8 w-8 text-muted-foreground hover:text-destructive"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              // TODO: 스크랩 기능 구현
            }}
          >
            <Heart className="h-4 w-4" />
          </Button>
        </div>

        {/* 중간: 스킬 태그 */}
        {skills.length > 0 && (
          <div className="mb-4 flex flex-wrap gap-1.5">
            {skills.slice(0, 4).map((skill) => (
              <Badge key={skill} variant="secondary" className="text-xs font-normal">
                {skill}
              </Badge>
            ))}
            {skills.length > 4 && (
              <Badge variant="secondary" className="text-xs font-normal">
                +{skills.length - 4}
              </Badge>
            )}
          </div>
        )}

        {/* 하단: 조건 정보 */}
        <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
          {location && (
            <div className="flex items-center gap-1">
              <MapPin className="h-3.5 w-3.5" />
              <span>{location}</span>
            </div>
          )}
          {salaryDisplay && (
            <div className="flex items-center gap-1">
              <Banknote className="h-3.5 w-3.5" />
              <span>{salaryDisplay}</span>
            </div>
          )}
          {createdAt && (
            <div className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              <span>{formatDate(createdAt)}</span>
            </div>
          )}
        </div>

        {/* AI 매칭 스코어 */}
        {matchInfo?.matchScore && (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-primary/5 px-3 py-2">
            <Sparkles className="h-4 w-4 text-primary" />
            <span className="text-sm font-medium text-primary">
              AI 매칭률 {matchInfo.matchScore}%
            </span>
          </div>
        )}
      </div>
    </Link>
  );
}
