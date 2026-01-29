import { MapPin, Building2, Clock, Heart, Bookmark, ChevronRight } from "lucide-react";
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
    if (diff === 0) return "방금 전";
    if (diff === 1) return "1일 전";
    if (diff < 7) return `${diff}일 전`;
  }
  return postedAt;
}

// Brand color mapping
const BRAND_COLORS: Record<string, string> = {
  "Toss": "#0064FF", // Toss Blue
  "Danggeun": "#FF6F0F", // Carrot Market Orange
  "Line": "#06C755", // Line Green
  "Woowa Bros": "#2AC1BC", // Woowa Mint
  "Naver": "#03C75A", // Naver Green
  "Kakao": "#FEE500", // Kakao Yellow
  "Samsung": "#1428A0", // Samsung Blue
  "Hyundai": "#002C5F", // Hyundai Blue
  "LG": "#A50034", // LG Red
  "SK": "#EA002C", // SK Red
  "Coupang": "#E41B23", // Coupang Red
};

const DEFAULT_COLOR = "#000000"; // Fallback

function getBrandColor(company: string): string {
  // Simple partial match or exact match
  const key = Object.keys(BRAND_COLORS).find(k => company.toLowerCase().includes(k.toLowerCase()));
  return key ? BRAND_COLORS[key] : DEFAULT_COLOR;
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
  const brandColor = getBrandColor(company);

  // Specific gradient from the requested design (Blue to Teal)
  const mainGradient = "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)";

  const displayLocation = location.split(" ").slice(0, 2).join(" ");
  const experienceLabel = requiredExperience ? `경력 ${requiredExperience}년+` : "신입/경력";

  return (
    <div
      className={cn(
        "group flex flex-col w-full h-full rounded-3xl bg-white border border-gray-100 p-6 shadow-sm transition-all duration-300 hover:shadow-xl hover:-translate-y-1 overflow-hidden",
        isAd && "ring-2 ring-primary/20"
      )}
    >
      {/* Header: Logo & Company Name - Fixed Height to align next sections */}
      <div className="flex items-start gap-4 mb-4 min-h-[90px]">
        <div
          className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full text-white text-xl font-bold shadow-sm mt-1"
          style={{ backgroundColor: brandColor }}
        >
          {logo.length > 5 ? (
            <img src={logo} alt={company} className="w-full h-full object-cover rounded-full" />
          ) : (
            <span>{company.charAt(0)}</span>
          )}
        </div>
        <div className="flex flex-col w-full">
          <div className="flex items-center gap-2 mb-0.5">
            <span className="font-bold text-lg text-gray-900 line-clamp-1">{company}</span>
            {/* Optional 'NEW' badge if needed */}
            {postedLabel === "방금 전" && (
              <span className="shrink-0 px-1.5 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-orange-600">NEW</span>
            )}
          </div>
          <h3 className="text-base font-bold text-gray-800 leading-snug line-clamp-2 min-h-[44px] group-hover:text-primary transition-colors">
            {title}
          </h3>
          <p className="text-xs text-gray-500 mt-1.5 flex items-center gap-2">
            <Building2 className="w-3 h-3 shrink-0" />
            <span className="shrink-0">{experienceLabel}</span>
            <span className="w-px h-2.5 bg-gray-300 shrink-0"></span>
            <MapPin className="w-3 h-3 shrink-0" />
            <span className="truncate">{displayLocation}</span>
          </p>
        </div>
      </div>

      {/* AI Match Rate Bar */}
      <div className="mb-5">
        <div className="flex justify-between items-end mb-2">
          <span className="text-sm font-bold text-gray-700">AI 매칭률</span>
          {matchScore !== undefined && matchScore !== null && (
            <span
              className="text-lg font-black text-transparent bg-clip-text"
              style={{ backgroundImage: mainGradient }}
            >
              {matchScore}%
            </span>
          )}
        </div>
        <div className="w-full h-2.5 bg-gray-100 rounded-full overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-1000 ease-out"
            style={{
              width: `${matchScore || 0}%`,
              background: mainGradient
            }}
          />
        </div>
      </div>

      {/* Skills - Fixed Height */}
      <div className="flex flex-wrap gap-2 mb-5 h-[52px] content-start overflow-hidden">
        {skills.slice(0, 3).map((skill, idx) => (
          <span
            key={idx}
            className="px-3 py-1.5 rounded-xl bg-gray-50 text-gray-600 text-xs font-semibold border border-gray-100 whitespace-nowrap"
          >
            {skill}
          </span>
        ))}
        {skills.length > 3 && (
          <span className="px-3 py-1.5 rounded-xl bg-gray-50 text-gray-400 text-xs font-semibold border border-gray-100 whitespace-nowrap">
            +{skills.length - 3}
          </span>
        )}
      </div>

      {/* Salary & Footer Info */}
      <div className="mt-auto">
        <p className="text-sm font-bold text-gray-800 mb-4 truncate">
          희망 연봉: {salary}
        </p>

        <div className="flex items-center gap-3">
          <Button
            asChild
            className="flex-1 h-12 rounded-xl text-base font-bold text-white shadow-md hover:shadow-lg transition-all hover:scale-[1.02] active:scale-[0.98] border-0"
            style={{ background: mainGradient }}
          >
            <Link to={`/jobs/${id}`}>
              지원하기
            </Link>
          </Button>
          <button
            type="button"
            className={cn(
              "h-12 w-12 flex items-center justify-center rounded-xl border border-gray-200 text-gray-400 hover:border-gray-300 hover:text-gray-600 transition-colors bg-white",
              interested && "text-red-500 border-red-200 bg-red-50 hover:border-red-300 hover:text-red-600"
            )}
            onClick={(e) => {
              e.preventDefault();
              setInterested(v => !v);
            }}
          >
            <Bookmark className={cn("w-6 h-6", interested && "fill-current")} />
          </button>
        </div>

        <div className="flex items-center justify-between mt-4 text-[11px] text-gray-400 font-medium">
          <span>마지막 활동: {postedLabel}</span>
          <span>채용공고 1개</span>
        </div>
      </div>
    </div>
  );
}
