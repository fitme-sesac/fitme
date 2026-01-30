import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Link } from "react-router-dom";
import { Briefcase, FileText, Bookmark, Users, Sparkles, ChevronRight, Search, Bell } from "lucide-react";
import { JobListSection } from "@/components/home/JobListSection";

import { JobSeekerHeroSection } from "@/components/home/JobSeekerHeroSection";
import { SurfitSection } from "@/components/home/SurfitSection";
import { usePublicJobs } from "@/hooks/useJobs";
import { Job } from "@/pages/Jobs";

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

    // Fetch jobs for AI recommendations
    const { data: rawData, isLoading } = usePublicJobs({
        page: 0,
        size: 20, // Fetch enough to sort
        keyword: ""
    });

    const jobs = (rawData as any)?.jobs as Job[] || [];

    // Sort by match rate descending
    const recommendedJobs = [...jobs].sort((a, b) => {
        const aRate = a.matchInfo?.overallMatchRate ?? a.matchInfo?.matchRate ?? 0;
        const bRate = b.matchInfo?.overallMatchRate ?? b.matchInfo?.matchRate ?? 0;
        return bRate - aRate;
    });

    return (
        <div className="space-y-6 pb-12">
            {/* 1. Hero Carousel Section */}
            <JobSeekerHeroSection />

            {/* 2. Surfit-style Layout Section (Now Full Width AI Grid) */}
            <SurfitSection displayName={displayName} jobs={recommendedJobs} />
        </div>
    );
}
