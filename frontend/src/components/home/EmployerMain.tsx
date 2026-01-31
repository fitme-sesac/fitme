import { EmployerHeroSection } from "./EmployerHeroSection";
import { TalentRecommendationSection } from "./TalentRecommendationSection";

export function EmployerMain() {
    return (
        <div className="flex flex-col gap-0 pb-12">
            <EmployerHeroSection />
            <TalentRecommendationSection />
        </div>
    );
}
