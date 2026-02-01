import { MapPin, Briefcase, GraduationCap, DollarSign, CheckCircle, Heart, User, Clock } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Link } from "react-router-dom";
import { useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";

interface TalentCardProps {
    id: number;
    name: string;
    title: string;
    summary?: string;
    experience: string;
    location: string;
    education?: string;
    skills: string[];
    salary: string;
    matchScore: number;
    avatar?: string | null;
    lastUpdated?: string;
    isNew?: boolean;
}

export function WideTalentCard({
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
    isNew = false,
}: TalentCardProps) {
    const [interested, setInterested] = useState(false);

    // Dynamic gradient based on match score
    const matchColor = matchScore >= 90 ? "text-blue-600 bg-blue-50 border-blue-200" :
        matchScore >= 80 ? "text-green-600 bg-green-50 border-green-200" :
            "text-yellow-600 bg-yellow-50 border-yellow-200";

    return (
        <div className="group relative w-full rounded-2xl bg-white border border-gray-100 p-6 shadow-sm transition-all duration-300 hover:shadow-lg hover:border-primary/20 hover:-translate-y-0.5">
            {/* Top Accent for High Match */}
            {matchScore >= 90 && (
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-500 to-indigo-500 rounded-t-2xl" />
            )}

            <div className="flex flex-col sm:flex-row gap-6">
                {/* Left: Avatar & Basic Info */}
                <div className="flex-shrink-0 flex flex-col items-center sm:items-start gap-3">
                    <div className="relative">
                        <Avatar className="h-20 w-20 sm:h-24 sm:w-24 border-2 border-white shadow-md">
                            <AvatarImage src={avatar || undefined} alt={name} />
                            <AvatarFallback className="text-2xl font-bold bg-slate-100 text-slate-500">
                                {name.charAt(0)}
                            </AvatarFallback>
                        </Avatar>
                        {isNew && (
                            <Badge className="absolute -top-2 -right-2 bg-orange-500 text-white border-2 border-white shadow-sm pointer-events-none">
                                NEW
                            </Badge>
                        )}
                        <div className={cn("absolute -bottom-2 right-1/2 translate-x-1/2 sm:right-0 sm:translate-x-0 badge border font-bold text-xs px-2 py-0.5 rounded-full flex items-center gap-1 min-w-max", matchColor)}>
                            <CheckCircle className="w-3 h-3" />
                            {matchScore}% 매칭
                        </div>
                    </div>
                </div>

                {/* Center: Details */}
                <div className="flex-1 space-y-4 text-center sm:text-left">
                    <div>
                        <div className="flex flex-col sm:flex-row sm:items-center gap-2 mb-1 justify-center sm:justify-start">
                            <h3 className="text-xl font-bold text-gray-900 flex items-center gap-2 justify-center sm:justify-start">
                                {name}
                                <span className="text-sm font-normal text-muted-foreground">({experience})</span>
                            </h3>
                            <Badge variant="outline" className="w-fit mx-auto sm:mx-0 text-slate-600 bg-slate-50 border-slate-200">
                                {title}
                            </Badge>
                        </div>
                        <div className="flex flex-wrap items-center justify-center sm:justify-start gap-x-4 gap-y-1 text-sm text-gray-500">
                            <span className="flex items-center gap-1.5">
                                <Briefcase className="w-3.5 h-3.5 text-gray-400" />
                                {experience}
                            </span>
                            <span className="flex items-center gap-1.5">
                                <MapPin className="w-3.5 h-3.5 text-gray-400" />
                                {location}
                            </span>
                            {education && (
                                <span className="flex items-center gap-1.5">
                                    <GraduationCap className="w-3.5 h-3.5 text-gray-400" />
                                    {education}
                                </span>
                            )}
                        </div>
                    </div>

                    {summary && (
                        <p className="text-sm text-gray-600 leading-relaxed bg-slate-50 p-3 rounded-lg border border-slate-100 italic">
                            "{summary}"
                        </p>
                    )}

                    <div className="space-y-2">
                        <div className="flex flex-wrap gap-1.5 justify-center sm:justify-start">
                            {skills.slice(0, 8).map((skill, i) => (
                                <Badge key={i} variant="secondary" className="px-2 py-0.5 text-xs font-medium bg-white border border-gray-200 text-gray-700 hover:bg-gray-50">
                                    {skill}
                                </Badge>
                            ))}
                            {skills.length > 8 && (
                                <Badge variant="secondary" className="px-2 py-0.5 text-xs text-gray-500 bg-gray-50">+{skills.length - 8}</Badge>
                            )}
                        </div>
                    </div>
                </div>

                {/* Right: Actions & Salary */}
                <div className="flex flex-col items-center sm:items-end gap-3 min-w-[140px] pt-2 border-t sm:border-t-0 sm:border-l sm:pl-6 border-gray-100 mt-2 sm:mt-0">
                    <div className="text-center sm:text-right mb-1">
                        <p className="text-xs text-muted-foreground mb-0.5 flex items-center justify-center sm:justify-end gap-1">
                            <DollarSign className="w-3 h-3" /> 희망 연봉
                        </p>
                        <p className="font-bold text-gray-900">{salary}</p>
                    </div>

                    <div className="mt-auto w-full space-y-2">
                        <Button
                            className="w-full font-bold shadow-md hover:shadow-lg transition-all"
                            style={{ background: 'linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)' }}
                            asChild
                        >
                            <Link to={`/talents/${id}`}>
                                제안하기
                            </Link>
                        </Button>
                        <Button
                            variant="outline"
                            className={cn(
                                "w-full border-gray-200 text-gray-500 hover:text-red-500 hover:border-red-200 hover:bg-red-50 gap-2 transition-all",
                                interested && "text-red-500 border-red-200 bg-red-50"
                            )}
                            onClick={() => setInterested(!interested)}
                        >
                            <Heart className={cn("w-4 h-4", interested && "fill-current")} />
                            <span>관심</span>
                        </Button>
                    </div>
                    {lastUpdated && (
                        <p className="text-xs text-gray-400 flex items-center gap-1 mt-1">
                            <Clock className="w-3 h-3" /> {lastUpdated} 업데이트
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}
