import { MapPin, Building2, Clock, Heart, ArrowRight } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Link } from "react-router-dom";
import { useState } from "react";

interface JobCardProps {
  id: number;
  company: string;
  logo: string;
  title: string;
  location: string;
  salary: string;
  skills?: string[] | string | null;
  postedAt: string;
  isAd?: boolean;
  matchScore?: number;
  requiredExperience?: number;
  companySummary?: string;
  employmentType?: string;
  createdAt?: string;
}

function formatPostedAt(createdAt: string | undefined, postedAt: string): string {
  if (createdAt) {
    const diff = Math.floor((Date.now() - new Date(createdAt).getTime()) / (1000 * 60 * 60 * 24));
    if (diff === 0) return "오늘";
    if (diff === 1) return "1일 전";
    if (diff < 7) return `${diff}일 전`;
  }
  return postedAt;
}

export function JobCard({
  id,
  company,
  logo,
  title,
  location,
  salary,
  skills: skillsProp,
  postedAt,
  isAd = false,
  matchScore,
  requiredExperience,
  companySummary,
  employmentType = "정규직",
  createdAt,
}: JobCardProps) {
  const [interested, setInterested] = useState(false);
  const skills = Array.isArray(skillsProp)
    ? skillsProp
    : skillsProp != null && typeof skillsProp === "string"
      ? [skillsProp]
      : [];

  const postedLabel = formatPostedAt(createdAt, postedAt);

  return (
    <div
      className={cn(
        "group flex flex-col w-full rounded-2xl bg-white border border-gray-100 p-6 shadow-sm transition-all duration-300 hover:shadow-lg hover:border-primary/20",
        isAd && "ring-1 ring-primary/20"
      )}
    >
      {/* 상단: 로고 + 매칭률 배지 */}
      <div className="flex items-start justify-between gap-3 mb-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-gray-100 text-lg font-bold text-gray-600 overflow-hidden">
          {logo.length > 5 ? (
            <img src={logo} alt={company} className="w-full h-full object-cover" />
          ) : (
            <span className="text-xl">{logo}</span>
          )}
        </div>
        {matchScore !== undefined && matchScore !== null && (
          <span className="inline-flex items-center justify-center min-w-[48px] h-8 px-2.5 rounded-full text-sm font-bold text-primary bg-primary/10">
            {matchScore}%
          </span>
        )}
      </div>

      {/* 직무 타이틀 + 회사명 */}
      <Link to={`/jobs/${id}`} className="block mb-2">
        <h3 className="text-lg font-bold text-gray-900 leading-snug line-clamp-2 group-hover:text-primary transition-colors">
          {title}
        </h3>
        <p className="text-sm text-gray-500 mt-0.5">{company}</p>
      </Link>

      {/* 기업/포지션 요약 */}
      {companySummary && (
        <p className="text-sm text-gray-600 line-clamp-2 mb-4 leading-relaxed">
          {companySummary}
        </p>
      )}

      {/* 근무지 · 고용형태 · 등록일 */}
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500 mb-4">
        {location && (
          <span className="flex items-center gap-1">
            <MapPin className="h-3.5 w-3.5" />
            {location.split(" ")[0] || location}
          </span>
        )}
        <span className="flex items-center gap-1">
          <Building2 className="h-3.5 w-3.5" />
          {employmentType}
        </span>
        <span className="flex items-center gap-1">
          <Clock className="h-3.5 w-3.5" />
          {postedLabel}
        </span>
      </div>

      {/* 연봉 */}
      <p className="text-sm text-gray-700 mb-4">
        <span className="font-medium text-gray-500">연봉 </span>
        <span className="font-medium text-gray-900">{salary}</span>
      </p>

      {/* 스택 태그 */}
      {skills.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-5">
          {skills.slice(0, 4).map((skill, idx) => (
            <Badge
              key={idx}
              variant="secondary"
              className="text-xs px-2.5 py-0.5 rounded-full bg-gray-100 text-gray-700 border-0 font-medium"
            >
              {skill}
            </Badge>
          ))}
          {skills.length > 4 && (
            <Badge variant="secondary" className="text-xs px-2.5 py-0.5 rounded-full bg-gray-100 text-gray-500">
              +{skills.length - 4}
            </Badge>
          )}
        </div>
      )}

      {/* 액션: 관심 + 지원하기 */}
      <div className="flex items-center gap-2 mt-auto pt-4 border-t border-gray-100">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className={cn(
            "gap-1.5 text-gray-500 hover:text-red-500",
            interested && "text-red-500"
          )}
          onClick={(e) => {
            e.preventDefault();
            setInterested((v) => !v);
          }}
        >
          <Heart className={cn("h-4 w-4", interested && "fill-current")} />
          관심
        </Button>
        <Button asChild size="sm" className="ml-auto bg-primary hover:bg-primary/90 text-white gap-1.5">
          <Link to={`/jobs/${id}`}>
            지원하기
            <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </div>
    </div>
  );
}
