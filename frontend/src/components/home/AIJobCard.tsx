import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Bookmark, MapPin, Briefcase, Sparkles } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { Link } from "react-router-dom";

export interface AIJobProps {
    id: string;
    company: string;
    logo: string; // URL or Char
    position: string;
    experience: string;
    location: string;
    matchScore: number;
    skills: string[];
    competitionRate?: string; // Changed from salary
    isNew?: boolean;
    activeTime: string;
    jobCount?: number; // "채용공고 3개"
}

const BRAND_COLORS: Record<string, string> = {
    "Toss": "#0064FF",
    "Danggeun": "#FF6F0F",
    "Line": "#06C755",
    "Woowa Bros": "#2AC1BC",
    "Naver": "#03C75A",
    "Kakao": "#FEE500",
    "Samsung": "#1428A0",
    "Hyundai": "#002C5F",
    "LG": "#A50034",
    "SK": "#EA002C",
    "Coupang": "#E41B23",
};

const DEFAULT_COLOR = "#000000";

function getBrandColor(company: string): string {
    const key = Object.keys(BRAND_COLORS).find(k => company.toLowerCase().includes(k.toLowerCase()));
    return key ? BRAND_COLORS[key] : DEFAULT_COLOR;
}

export function AIJobCard({ job }: { job: AIJobProps }) {
    const brandColor = getBrandColor(job.company);
    const mainGradient = "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)";

    // Match score from refined API
    const matchScore = job.matchScore;

    return (
        <Card className="hover:shadow-lg transition-all duration-300 border-border group overflow-hidden h-full flex flex-col">
            <CardContent className="p-6 flex-1 flex flex-col">
                {/* Header: Company Info */}
                <div className="flex items-start justify-between mb-5">
                    <div className="flex items-start gap-4 w-full">
                        {/* Logo Avatar */}
                        <div
                            className="h-14 w-14 rounded-2xl flex-shrink-0 flex items-center justify-center text-xl font-bold text-white shadow-sm overflow-hidden"
                            style={{ backgroundColor: brandColor }}
                        >
                            {job.logo && job.logo.length > 1 ? (
                                <img src={job.logo} alt={job.company} className="w-full h-full object-cover" />
                            ) : (
                                job.logo || job.company[0]
                            )}
                        </div>
                        <div className="flex flex-col h-[90px] justify-between flex-1 min-w-0">
                            <div className="flex items-center gap-2 h-6">
                                {job.isNew && (
                                    <Badge variant="secondary" className="bg-orange-50 text-orange-600 text-[10px] h-5 px-1.5 border-orange-100 pointer-events-none flex-shrink-0">
                                        NEW
                                    </Badge>
                                )}
                                <h3 className="font-bold text-base text-slate-900 dark:text-slate-100 leading-tight truncate">{job.company}</h3>
                            </div>
                            {/* Enforce 2 lines height for position */}
                            <p className="text-sm font-semibold text-slate-800 line-clamp-2 leading-snug h-10 flex items-start">
                                {job.position}
                            </p>
                            <div className="flex items-center gap-2 text-xs text-slate-500 h-5">
                                <span className="flex items-center gap-1"><Briefcase className="w-3 h-3" /> {job.experience}</span>
                                <span className="text-slate-300">|</span>
                                <span className="flex items-center gap-1 truncate"><MapPin className="w-3 h-3" /> {job.location}</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Standardized Match Score UI */}
                <div className="mb-4">
                    {matchScore !== undefined && matchScore !== null ? (
                        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/5 border border-primary/20 shadow-sm mb-2">
                            <Sparkles className="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
                            <span className="text-xs font-black text-primary">
                                AI 매칭률 {Math.round(matchScore)}%
                            </span>
                        </div>
                    ) : (
                        <div className="h-[26px] mb-2" /> // Placeholder to maintain height
                    )}

                    {/* Gradient Progress Bar */}
                    <div className="h-2 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                        <div
                            className="h-full rounded-full transition-all duration-1000"
                            style={{
                                width: `${matchScore ?? 0}%`,
                                background: mainGradient
                            }}
                        />
                    </div>
                </div>

                {/* Skills - Enforce 1 line height or consistent height */}
                <div className="flex flex-wrap gap-1.5 mb-6 h-[26px] overflow-hidden">
                    {job.skills.slice(0, 4).map((skill, i) => (
                        <Badge key={i} variant="secondary" className="bg-slate-50 border border-slate-200 text-slate-600 hover:bg-slate-100 font-medium whitespace-nowrap">
                            {skill}
                        </Badge>
                    ))}
                    {job.skills.length > 4 && (
                        <Badge variant="secondary" className="bg-slate-50 border border-slate-200 text-slate-400 font-medium whitespace-nowrap">+{job.skills.length - 4}</Badge>
                    )}
                </div>

                {/* Push to bottom */}
                <div className="mt-auto">
                    {/* Competition Rate (Previously Salary) */}
                    <div className="text-sm font-semibold text-slate-700 mb-4 h-5 flex items-center">
                        경쟁률: {job.competitionRate || "집계중"}
                    </div>

                    {/* Actions */}
                    <div className="flex gap-2">
                        <Link to={`/jobs/${job.id}`} className="flex-1 block">
                            <Button
                                className="w-full text-white border-0 shadow-md font-bold text-base transition-all hover:opacity-90 active:scale-[0.98]"
                                style={{ background: mainGradient }}
                            >
                                지원하기
                            </Button>
                        </Link>
                        <Button variant="outline" size="icon" className="border-slate-200 text-slate-400 hover:text-red-500 hover:border-red-200 hover:bg-red-50 shrink-0">
                            <Bookmark className="w-4 h-4" />
                        </Button>
                    </div>
                </div>
            </CardContent>

            {/* Footer Meta */}
            <CardFooter className="px-6 py-3 bg-slate-50 dark:bg-slate-900/50 border-t border-slate-100 text-xs text-slate-400 flex justify-between items-center">
                <span>마지막 활동: {job.activeTime}</span>
                {job.jobCount && <span className="text-slate-500 font-medium">채용공고 {job.jobCount}개</span>}
            </CardFooter>
        </Card>
    );
}
