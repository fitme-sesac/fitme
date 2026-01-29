import { MapPin, Briefcase, GraduationCap, Heart, ArrowRight } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Link } from "react-router-dom";
import { useState } from "react";

export interface TalentCardProps {
  id: number;
  name: string;
  title: string;
  summary?: string;
  experience: string;
  location: string;
  education?: string;
  skills: string[];
  salary: string;
  matchScore?: number;
  avatar?: string | null;
  lastUpdated?: string;
  isNew?: boolean;
}

export function TalentCard({
  id,
  name,
  title,
  summary,
  experience,
  location,
  education,
  skills,
  salary,
  matchScore,
  avatar,
  lastUpdated,
  isNew,
}: TalentCardProps) {
  const [interested, setInterested] = useState(false);

  return (
    <div className="group flex flex-col w-full rounded-2xl bg-white border border-gray-100 p-6 shadow-sm transition-all duration-300 hover:shadow-lg hover:border-primary/20">
      {/* 상단: 프로필 + 매칭률 */}
      <div className="flex items-start justify-between gap-3 mb-4">
        <div className="flex items-center gap-3 min-w-0">
          <Avatar className="h-14 w-14 shrink-0 rounded-full bg-primary/10 border border-gray-100">
            <AvatarImage src={avatar || undefined} />
            <AvatarFallback className="bg-primary/10 text-primary text-lg font-semibold">
              {name.slice(0, 2)}
            </AvatarFallback>
          </Avatar>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="font-bold text-gray-900 truncate">{name}</h3>
              {isNew && (
                <Badge variant="secondary" className="text-[10px] px-1.5 py-0 bg-green-100 text-green-700 border-0">
                  NEW
                </Badge>
              )}
            </div>
            <p className="text-sm text-gray-600 truncate">{title}</p>
          </div>
        </div>
        {matchScore !== undefined && matchScore !== null && (
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-bold text-sm">
            {matchScore}%
          </div>
        )}
      </div>

      {/* 인재 요약 */}
      {summary && (
        <p className="text-sm text-gray-600 line-clamp-3 mb-4 leading-relaxed">
          {summary}
        </p>
      )}

      {/* 경력 · 지역 · 학력 */}
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-gray-500 mb-4">
        <span className="flex items-center gap-1.5">
          <Briefcase className="h-3.5 w-3.5" />
          경력 {experience}
        </span>
        <span className="flex items-center gap-1.5">
          <MapPin className="h-3.5 w-3.5" />
          {location}
        </span>
        {education && (
          <span className="flex items-center gap-1.5">
            <GraduationCap className="h-3.5 w-3.5" />
            {education}
          </span>
        )}
      </div>

      {/* 스킬 태그 */}
      {skills.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-4">
          {skills.slice(0, 6).map((skill, idx) => (
            <Badge
              key={idx}
              variant="secondary"
              className="text-xs px-2.5 py-0.5 rounded-full bg-gray-100 text-gray-700 border-0 font-medium"
            >
              {skill}
            </Badge>
          ))}
          {skills.length > 6 && (
            <Badge variant="secondary" className="text-xs px-2.5 py-0.5 rounded-full bg-gray-100 text-gray-500">
              +{skills.length - 6}
            </Badge>
          )}
        </div>
      )}

      {/* 희망 연봉 */}
      <p className="text-sm text-gray-700 mb-4">
        <span className="font-medium text-gray-500">희망 연봉 </span>
        <span className="font-medium text-gray-900">{salary}</span>
      </p>

      {/* 액션: 관심 + 프로필 보기 */}
      <div className="flex items-center gap-2 mt-auto pt-4 border-t border-gray-100">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className={`gap-1.5 text-gray-500 hover:text-red-500 ${interested ? "text-red-500" : ""}`}
          onClick={() => setInterested((v) => !v)}
        >
          <Heart className={`h-4 w-4 ${interested ? "fill-current" : ""}`} />
          관심
        </Button>
        <Button asChild size="sm" className="ml-auto bg-primary hover:bg-primary/90 text-white gap-1.5">
          <Link to={`/talents/${id}`}>
            프로필 보기
            <ArrowRight className="h-4 w-4" />
          </Link>
        </Button>
      </div>

      {/* 마지막 업데이트 */}
      {lastUpdated && (
        <p className="text-[11px] text-gray-400 text-center mt-3">
          {lastUpdated} 업데이트
        </p>
      )}
    </div>
  );
}
