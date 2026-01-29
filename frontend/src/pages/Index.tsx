import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { HeroSection } from "@/components/home/HeroSection";
import { JobListSection } from "@/components/home/JobListSection";
import { CompanySection } from "@/components/home/CompanySection";
import { FeatureSection } from "@/components/home/FeatureSection";
import { CTASection } from "@/components/home/CTASection";
import { Footer } from "@/components/layout/Footer";
import { EmployerHeroSection } from "@/components/home/EmployerHeroSection";
import { TalentRecommendationSection } from "@/components/home/TalentRecommendationSection";
import { useAuth } from "@/contexts/AuthContext";

const Index = () => {
  const { isCompany, user } = useAuth();

  // Debug mode: Reverting to checks
  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:pl-64 transition-all duration-300">
        <Header />
        <main>
          <HeroSection />
          <JobListSection />
          <CompanySection />
          <FeatureSection />
          <CTASection />
        </main>
        <Footer />
      </div>
    </div>
  );
};

export default Index;
