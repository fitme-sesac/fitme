import { usePublicJobs } from "@/hooks/useJobs";
import { Link } from "react-router-dom";
import { ChevronRight, Building2, MapPin } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";

export function SideJobList() {
    const { data, isLoading } = usePublicJobs({ size: 5, sort: "created_at,desc" });
    const jobs = data?.jobs || [];

    return (
        <div className="bg-card rounded-2xl border border-border p-5">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-lg">새로운 채용 공고</h3>
                <Link to="/jobs" className="text-xs text-muted-foreground hover:text-primary flex items-center">
                    더보기 <ChevronRight className="h-3 w-3" />
                </Link>
            </div>

            <div className="space-y-4">
                {isLoading ? (
                    Array.from({ length: 5 }).map((_, i) => (
                        <div key={i} className="flex gap-3">
                            <Skeleton className="h-10 w-10 rounded-lg" />
                            <div className="space-y-2 flex-1">
                                <Skeleton className="h-4 w-3/4" />
                                <Skeleton className="h-3 w-1/2" />
                            </div>
                        </div>
                    ))
                ) : jobs.length > 0 ? (
                    jobs.map((job: any) => (
                        <Link
                            key={job.jobId}
                            to={`/jobs/${job.jobId}`}
                            className="flex gap-3 group items-start hover:bg-accent/5 p-2 rounded-lg transition-colors -mx-2"
                        >
                            <div className="h-10 w-10 rounded-lg bg-secondary flex items-center justify-center shrink-0 overflow-hidden font-bold text-secondary-foreground">
                                {job.companyLogoUrl ? (
                                    <img src={job.companyLogoUrl} alt={job.companyName} className="h-full w-full object-cover" />
                                ) : (
                                    job.companyName.charAt(0)
                                )}
                            </div>
                            <div className="min-w-0">
                                <h4 className="text-sm font-semibold truncate group-hover:text-primary transition-colors">
                                    {job.title}
                                </h4>
                                <div className="flex items-center text-xs text-muted-foreground mt-1 gap-2">
                                    <span className="flex items-center gap-0.5 truncate max-w-[80px]">
                                        <Building2 className="h-3 w-3" /> {job.companyName}
                                    </span>
                                    {job.location && (
                                        <span className="flex items-center gap-0.5 truncate">
                                            <MapPin className="h-3 w-3" /> {job.location.split(' ')[0]}
                                        </span>
                                    )}
                                </div>
                            </div>
                        </Link>
                    ))
                ) : (
                    <p className="text-sm text-muted-foreground py-4 text-center">
                        새로운 공고가 없습니다.
                    </p>
                )}
            </div>

            <div className="mt-6 pt-4 border-t border-border">
                <div className="rounded-xl bg-gradient-to-r from-primary/10 to-accent/10 p-4 text-center">
                    <p className="text-sm font-bold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                        나에게 딱 맞는<br />AI 포지션 추천받기
                    </p>
                    <Link to="/resume" className="mt-2 text-xs text-muted-foreground hover:underline block">
                        이력서 등록하러 가기 &rarr;
                    </Link>
                </div>
            </div>
        </div>
    );
}
