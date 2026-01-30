import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Sidebar } from "@/components/layout/Sidebar";
import { JobPostingsTab } from "@/components/company/JobPostingsTab";
import { ApplicantsTab } from "@/components/company/ApplicantsTab";
import { AIMatchingTab } from "@/components/company/AIMatchingTab";
import { AdAnalyticsTab } from "@/components/company/AdAnalyticsTab";
import { DashboardHeader } from "@/components/company/DashboardHeader";
import { AdBanner } from "@/components/company/AdBanner";
import { CompanyCommunityManagement } from "@/components/company/CompanyCommunityManagement";

import { useSearchParams } from "react-router-dom";

export default function CompanyDashboard() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") || "jobs";
  const [activeTab, setActiveTab] = useState(initialTab);

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
      <main className="lg:ml-64">
        <div className="p-6 lg:p-8">
          {/* 상단 광고 배너 */}
          <AdBanner position="top" />

          <DashboardHeader />

          <Tabs value={activeTab} onValueChange={handleTabChange} className="mt-6">
            <TabsList className="grid w-full grid-cols-4 lg:w-auto lg:inline-flex">
              <TabsTrigger value="jobs">채용공고 관리</TabsTrigger>
              <TabsTrigger value="applicants">지원자 현황</TabsTrigger>
              <TabsTrigger value="ai-matching">AI 인재 추천</TabsTrigger>
              <TabsTrigger value="community">커뮤니티 관리</TabsTrigger>
              <TabsTrigger value="ads">광고 성과</TabsTrigger>
            </TabsList>

            <div className="mt-6 grid grid-cols-1 xl:grid-cols-[1fr_300px] gap-6">
              <div>
                <TabsContent value="jobs" className="mt-0">
                  <JobPostingsTab />
                </TabsContent>

                <TabsContent value="applicants" className="mt-0">
                  <ApplicantsTab />
                </TabsContent>

                <TabsContent value="ai-matching" className="mt-0">
                  <AIMatchingTab />
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
          </Tabs>
        </div>
      </main>
    </div>
  );
}
