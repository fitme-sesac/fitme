import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Bookmark, MapPin, Briefcase } from "lucide-react";
import { Progress } from "@/components/ui/progress";

export interface AIJobProps {
    id: string;
    company: string;
    logo: string; // URL or Char
    position: string;
    experience: string;
    location: string;
    matchScore: number;
    skills: string[];
    salary: string;
    isNew?: boolean;
    activeTime: string;
    jobCount?: number; // "채용공고 3개"
}

export function AIJobCard({ job }: { job: AIJobProps }) {
    return (
        <Card className="hover:shadow-lg transition-all duration-300 border-border group overflow-hidden h-full flex flex-col">
            <CardContent className="p-6 flex-1 flex flex-col">
                {/* Header: Company Info */}
                <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                        {/* Logo Avatar */}
                        <div className={`h-12 w-12 rounded-full flex items-center justify-center text-lg font-bold text-white shadow-sm 
                ${job.matchScore >= 90 ? 'bg-emerald-500' :
                                job.matchScore >= 80 ? 'bg-blue-500' : 'bg-slate-500'}`}>
                            {job.logo || job.company[0]}
                        </div>
                        <div>
                            <div className="flex items-center gap-2">
                                <h3 className="font-bold text-lg text-slate-900 dark:text-slate-100">{job.company}</h3>
                                {job.isNew && <Badge variant="secondary" className="bg-orange-100 text-orange-600 text-[10px] h-5 px-1.5 pointer-events-none">NEW</Badge>}
                            </div>
                            <p className="text-sm font-medium text-slate-700 mt-0.5 line-clamp-1">{job.position}</p>
                            <div className="flex items-center gap-2 text-xs text-slate-500 mt-1">
                                <span className="flex items-center gap-1"><Briefcase className="w-3 h-3" /> {job.experience}</span>
                                <span className="flex items-center gap-1"><MapPin className="w-3 h-3" /> {job.location}</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Match Score */}
                <div className="mb-4 space-y-1.5">
                    <div className="flex items-center justify-between text-sm">
                        <span className="font-medium text-slate-500">AI 매칭률</span>
                        <span className={`font-bold ${job.matchScore >= 90 ? 'text-emerald-500' : 'text-blue-500'}`}>{job.matchScore}%</span>
                    </div>
                    <Progress value={job.matchScore} className={`h-2 ${job.matchScore >= 90 ? 'bg-emerald-100' : 'bg-blue-100'}`} indicatorClassName={job.matchScore >= 90 ? 'bg-emerald-500' : 'bg-blue-500'} />
                </div>

                {/* Skills - Fixed min height to prevent jumping, OR just let flex handle it */}
                <div className="flex flex-wrap gap-1.5 mb-6">
                    {job.skills.slice(0, 4).map((skill, i) => (
                        <Badge key={i} variant="secondary" className="bg-slate-100 text-slate-600 hover:bg-slate-200 font-normal">
                            {skill}
                        </Badge>
                    ))}
                    {job.skills.length > 4 && (
                        <Badge variant="secondary" className="bg-slate-100 text-slate-400 font-normal">+{job.skills.length - 4}</Badge>
                    )}
                </div>

                {/* Push to bottom */}
                <div className="mt-auto">
                    {/* Salary */}
                    <div className="text-sm font-semibold text-slate-700 mb-4">
                        희망 연봉: {job.salary}
                    </div>

                    {/* Actions */}
                    <div className="flex gap-2">
                        <Button className={`flex-1 ${job.matchScore >= 90 ? 'bg-emerald-500 hover:bg-emerald-600' : 'bg-blue-600 hover:bg-blue-700'} text-white border-0 shadow-md`}>
                            지원하기
                        </Button>
                        <Button variant="outline" size="icon" className="border-slate-200 text-slate-400 hover:text-red-500 hover:border-red-200 hover:bg-red-50">
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
