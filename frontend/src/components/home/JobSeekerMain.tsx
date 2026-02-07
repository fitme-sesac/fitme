import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Link } from "react-router-dom";
import { Briefcase, FileText, Bookmark, Users, Sparkles, ChevronRight, Search, Bell } from "lucide-react";
import { JobListSection } from "@/components/home/JobListSection";

import { JobSeekerHeroSection } from "@/components/home/JobSeekerHeroSection";
import { SurfitSection } from "@/components/home/SurfitSection";
import { usePublicJobs, useAiRecommendations } from "@/hooks/useJobs";
import { Job } from "@/pages/common/Jobs";
import { useQuery } from "@tanstack/react-query";
import { getMyResumes } from "@/api/resumes";

// Mock User Stats
const USER_STATS = [
    { label: "지원 완료", count: 5, icon: FileText, color: "text-blue-500", bg: "bg-blue-500/10" },
    { label: "서류 통과", count: 2, icon: Briefcase, color: "text-green-500", bg: "bg-green-500/10" },
    { label: "스크랩", count: 12, icon: Bookmark, color: "text-purple-500", bg: "bg-purple-500/10" },
    { label: "제안받음", count: 3, icon: Bell, color: "text-amber-500", bg: "bg-amber-500/10" },
];

export function JobSeekerMain() {
    const { user, profile } = useAuth();
    const displayName = profile?.display_name || user?.user_metadata?.display_name || "지원자";

    // 1. Fetch AI Recommendations (for logged in users) - Using refined matching for consistency
    // 1. Fetch AI Recommendations (for logged in users) - Using refined matching for consistency
    const { data: rawAiData, isLoading: aiLoading } = usePublicJobs({
        memberId: user?.id,
        sortBy: "match",
        size: 12, // Fetch enough candidates (like Jobs page) to find high-score matches even if vector rank is lower
        enabled: !!user
    });
    // Display top 4 from the sorted list
    const aiJobs = ((rawAiData as any)?.jobs || []).slice(0, 4);

    // 2. Fallback to general jobs if needed - Still pass memberId for real match calculation
    const { data: rawPublicData, isLoading: publicLoading } = usePublicJobs({
        memberId: user?.id,
        page: 0,
        size: 4,
        enabled: !user || aiJobs.length === 0
    });

    const publicJobs = (rawPublicData as any)?.jobs as Job[] || [];

    // Final jobs for display
    const recommendedJobs = aiJobs.length > 0 ? aiJobs : publicJobs;

    // Fetch resumes to check status
    const { data: resumes = [] } = useQuery<any[]>({
        queryKey: ["myResumes"],
        queryFn: getMyResumes,
        enabled: !!user,
        // AI 요약 중일 때는 3초마다 갱신하여 완료 시 자동으로 배너가 사라지게 함
        refetchInterval: (query) => {
            const data = query.state.data as any[];
            const hasProcessing = data?.some((r: any) =>
                r.primary && (r.summaryStatus === "PROCESSING" || r.summaryStatus === "PENDING")
            );
            return hasProcessing ? 3000 : false;
        }
    });

    const primaryResume = (resumes as any[]).find((r: any) => r.primary);
    const resumeStatus = !primaryResume
        ? "NO_PRIMARY"
        : (primaryResume.summaryStatus === "PROCESSING" || primaryResume.summaryStatus === "PENDING")
            ? "PROCESSING"
            : "COMPLETED";

    return (
        <div className="space-y-6 pb-12">
            {/* 1. Hero Carousel Section */}
            <JobSeekerHeroSection />

            {/* 2. Surfit-style Layout Section (Now Full Width AI Grid) */}
            <SurfitSection
                displayName={displayName}
                jobs={recommendedJobs}
                resumeStatus={resumeStatus}
            />
        </div>
    );
}
