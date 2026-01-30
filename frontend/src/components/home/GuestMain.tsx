import { HeroSection } from "@/components/home/HeroSection";
import { JobListSection } from "@/components/home/JobListSection";
import { CompanySection } from "@/components/home/CompanySection";
import { FeatureSection } from "@/components/home/FeatureSection";
import { CTASection } from "@/components/home/CTASection";

export function GuestMain() {
    return (
        <>
            <HeroSection />
            <JobListSection />
            <CompanySection />
            <FeatureSection />
            <CTASection />
        </>
    );
}
