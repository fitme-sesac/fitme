import { useEffect, useState } from "react";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Loader2, User } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { getMyApplications } from "@/api/applications";
import { getPublicJobs, getMyScrapedJobs } from "@/api/jobs"; // Modified import
import { getMyPosts } from "@/api/community";
import { JobSeekerSidebar } from "@/components/mypage/JobSeekerSidebar";
import { JobSeekerProfile } from "@/components/mypage/JobSeekerProfile";
import { JobSeekerWidgets } from "@/components/mypage/JobSeekerWidgets";

import { getMyProfileSummary } from "@/api/resumes"; // Import getMyProfileSummary

// ... existing imports

const JobSeekerMyPage = () => {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    proposals: 0,
    saved: 0,
    community: 0
  });
  const [recommendations, setRecommendations] = useState<any[]>([]);
  const [profileData, setProfileData] = useState<any>(null); // State for profile data

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Scraps
        const scrapsResponse = await getMyScrapedJobs();
        const scrapsData = Array.isArray(scrapsResponse) ? scrapsResponse : scrapsResponse?.content || [];

        // Community Posts (to get count)
        const postsResponse = await getMyPosts(0, 1);
        const communityCount = postsResponse?.totalElements || (Array.isArray(postsResponse) ? postsResponse.length : 0);

        // Fetch recommended jobs
        const jobsResponse = await getPublicJobs({ page: 0, size: 6 });
        const jobsData = jobsResponse?.content || [];
        setRecommendations(jobsData);

        // Fetch Profile Summary
        const profileSummary = await getMyProfileSummary();
        setProfileData(profileSummary);

        setStats({
          proposals: 0,
          saved: scrapsData.length,
          community: communityCount
        });
      } catch (error) {
        console.error("Failed to fetch data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-[#5A639C]" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />

      <div className="lg:pl-64 transition-all duration-300">
        <Header />

        <main className="container max-w-7xl mx-auto py-12 px-4 md:px-8">
          {/* Page Title Area */}
          <div className="flex items-end justify-between mb-8 border-b pb-4">
            <div className="flex items-center gap-3">
              <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-[#5A639C] text-white">
                <User className="h-6 w-6" />
              </div>
              <h1 className="text-2xl font-bold">마이페이지</h1>
            </div>
            <div className="text-sm text-muted-foreground pb-1">
              나의 채용 활동 현황을 한눈에 확인하세요.
            </div>
          </div>

          <div className="grid grid-cols-12 gap-8">
            {/* Left Sidebar Menu */}
            <div className="col-span-12 md:col-span-3 lg:col-span-2 hidden md:block">
              <div className="sticky top-24">
                <JobSeekerSidebar />
              </div>
            </div>

            {/* Main Content */}
            <div className="col-span-12 md:col-span-9 lg:col-span-7">
              <JobSeekerProfile stats={stats} recommendations={recommendations} profileData={profileData} />
            </div>

            {/* Right Widgets */}
            <div className="col-span-12 md:col-span-3 lg:col-span-3 hidden lg:block">
              <div className="sticky top-24">
                <JobSeekerWidgets />
              </div>
            </div>
          </div>
        </main>

        <Footer />
      </div>
    </div>
  );
};

export default JobSeekerMyPage;
