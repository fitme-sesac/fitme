import { useState, useEffect } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { JobPostingsTab } from "@/components/company/JobPostingsTab";
import { ApplicantsTab } from "@/components/company/ApplicantsTab";
import { AIMatchingTab } from "@/components/company/AIMatchingTab";
import { AdAnalyticsTab } from "@/components/company/AdAnalyticsTab";
import { SentProposalsTab } from "@/components/company/SentProposalsTab";
import { DashboardHeader } from "@/components/company/DashboardHeader";
import { AdBanner } from "@/components/company/AdBanner";
import { CompanyCommunityManagement } from "@/components/company/CompanyCommunityManagement";
import { InterviewsTab } from "@/components/company/InterviewsTab";
import { getEmployerDashboard, getEmployerProfile } from "@/api/employers";
import { useAuth } from "@/contexts/AuthContext";

import { useSearchParams } from "react-router-dom";

export default function CompanyDashboard() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") || "jobs";
  const [activeTab, setActiveTab] = useState(initialTab);
  const [jobsRefetchKey, setJobsRefetchKey] = useState(0);
  const [editJobUid, setEditJobUid] = useState<string | null>(null);
  const [dashboardInfo, setDashboardInfo] = useState<{ companyName?: string; memberName?: string } | null>(null);
  const { user } = useAuth();

  useEffect(() => {
    const u = user as { name?: string; user_metadata?: { name?: string } } | null;
    const memberName = u?.name ?? u?.user_metadata?.name ?? undefined;
    if (!user) {
      setDashboardInfo(null);
      return;
    }
    setDashboardInfo((prev) => ({ ...prev, memberName }));
    let cancelled = false;
    getEmployerDashboard()
      .then((res: { profile?: { name?: string }; recentJobs?: unknown[] }) => {
        if (cancelled) return;
        const name = res.profile?.name ?? undefined;
        if (name) {
          setDashboardInfo((prev) => ({ ...prev, companyName: name }));
          return;
        }
        return getEmployerProfile().then((profile: { name?: string }) => {
          if (cancelled) return;
          setDashboardInfo((prev) => ({ ...prev, companyName: profile?.name ?? undefined }));
        });
      })
      .catch(() => {
        if (!cancelled) {
          getEmployerProfile()
            .then((profile: { name?: string }) => {
              if (!cancelled) setDashboardInfo((prev) => ({ ...prev, companyName: profile?.name ?? undefined }));
            })
            .catch(() => {
              if (!cancelled) setDashboardInfo((prev) => ({ ...prev, companyName: undefined }));
            });
        }
      });
    return () => { cancelled = true; };
  }, [user]);

  // Sync URL when tab changes
  const handleTabChange = (val: string) => {
    setActiveTab(val);
    setSearchParams(prev => {
      prev.set('tab', val);
      return prev;
    });
  };

  // Sync state if URL changes externally (e.g. back button)
  // Although not strictly necessary if we only use one way binding, bidirectional is safer
  // Actually, let's just use the setter wrapper.


  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:ml-64 flex flex-col min-h-screen">
        <Header />
        <main className="flex-1">
          <div className="p-6 lg:p-8">
            {/* 상단 광고 배너 */}
            <AdBanner position="top" />

            <DashboardHeader
              onJobCreated={() => setJobsRefetchKey((k) => k + 1)}
              editJobUid={editJobUid}
              onClearEdit={() => setEditJobUid(null)}
              companyName={dashboardInfo?.companyName ?? null}
              memberName={dashboardInfo?.memberName ?? (user as { name?: string })?.name ?? null}
            />

            <Tabs value={activeTab} onValueChange={handleTabChange} className="mt-6">
              <TabsList className="grid w-full grid-cols-3 sm:grid-cols-4 lg:grid-cols-7 lg:inline-flex flex-wrap justify-start">
                <TabsTrigger value="jobs">채용공고 관리</TabsTrigger>
                <TabsTrigger value="applicants">지원자 현황</TabsTrigger>
                <TabsTrigger value="interviews">면접 일정</TabsTrigger>
                <TabsTrigger value="ai-matching">AI 인재 추천</TabsTrigger>
                <TabsTrigger value="sent-proposals">제안한 인재</TabsTrigger>
                <TabsTrigger value="community">커뮤니티 관리</TabsTrigger>
                <TabsTrigger value="ads">광고 성과</TabsTrigger>
              </TabsList>

              <div className="mt-6 grid grid-cols-1 xl:grid-cols-[1fr_300px] gap-6">
                <div>
                  <TabsContent value="jobs" className="mt-0">
                    <JobPostingsTab
                      refetchKey={jobsRefetchKey}
                      onEditJob={(jobUid) => setEditJobUid(jobUid)}
                    />
                  </TabsContent>

                  <TabsContent value="applicants" className="mt-0">
                    <ApplicantsTab />
                  </TabsContent>

                  <TabsContent value="ai-matching" className="mt-0">
                    <AIMatchingTab />
                  </TabsContent>

                  <TabsContent value="sent-proposals" className="mt-0">
                    <SentProposalsTab />
                  </TabsContent>

                  <TabsContent value="ads" className="mt-0">
                    <AdAnalyticsTab />
                  </TabsContent>

                  <TabsContent value="community" className="mt-0">
                    <CompanyCommunityManagement />
                  </TabsContent>
                </div>

                {/* 사이드 광고 배너 */}
                <div className="hidden xl:block">
                  <AdBanner position="side" />
                </div>
              </div>

              {/* 면접 일정 탭 - 별도 레이아웃 사용 */}
              <TabsContent value="interviews" className="mt-6">
                <InterviewsTab />
              </TabsContent>

            </Tabs>
          </div>
        </main>
      </div>
    </div>
  );
}
