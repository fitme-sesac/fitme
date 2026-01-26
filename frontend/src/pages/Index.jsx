import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { HeroSection } from "@/components/home/HeroSection";
import { JobListSection } from "@/components/home/JobListSection";
import { CompanySection } from "@/components/home/CompanySection";
import { FeatureSection } from "@/components/home/FeatureSection";
import { CTASection } from "@/components/home/CTASection";
import { Footer } from "@/components/layout/Footer";

const Index = () => {
  return (
    <div className="min-h-screen bg-background">
      {/* 사이드바 - 데스크탑 */}
      <Sidebar />
      
      {/* 메인 콘텐츠 영역 */}
      <div className="lg:pl-64">
        {/* 헤더 */}
        <Header />
        
        {/* 메인 콘텐츠 */}
        <main>
          <HeroSection />
          <JobListSection />
          <CompanySection />
          <FeatureSection />
          <CTASection />
        </main>
        
        {/* 푸터 */}
        <Footer />
      </div>
    </div>
  );
};

export default Index;
