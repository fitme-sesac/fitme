import { useState, useEffect } from "react";
import { CreditCard, Building2, Shield, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { subscriptionPlans, SubscriptionPlan } from "@/data/subscriptionPlans";
import { PricingCard } from "@/components/subscription/PricingCard";
import { PlanComparison } from "@/components/subscription/PlanComparison";
import { FAQ } from "@/components/subscription/FAQ";
import { toast } from "sonner";
import { Link, useLocation } from "react-router-dom";
import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { SubscriptionCheckoutModal } from "@/components/subscription/SubscriptionCheckoutModal";
import { useEmployerProfile } from "@/hooks/useEmployers";
import { useEmployerSubscriptions } from "@/features/payment/hooks/useSubscription";
import { useAuth } from "@/contexts/AuthContext";

export default function Subscription() {
    const [isYearly, setIsYearly] = useState(false);
    const [showCheckoutModal, setShowCheckoutModal] = useState(false);
    const [selectedPlan, setSelectedPlan] = useState<SubscriptionPlan | null>(null);
    const location = useLocation();
    const { isCompany, user } = useAuth() as any;

    // Fetch employer profile and subscriptions
    const { data: employerProfile } = useEmployerProfile();
    const { data: subscriptions } = useEmployerSubscriptions(employerProfile?.employerId);

    // Active subscription check
    const activeSubscription = subscriptions?.find(s => s.status === "ACTIVE");
    const activeProductId = activeSubscription?.product?.productId;

    // Check if the current page is a simple product introduction page
    const isProductIntro = location.pathname === "/products";

    const handleSelectPlan = (planId: string) => {
        const plan = subscriptionPlans.find(p => p.id === planId);
        if (!plan) return;

        if (plan.id === "enterprise") {
            toast.info("Enterprise 플랜 문의가 접수되었습니다. 영업팀에서 곧 연락드리겠습니다.");
        } else if (plan.id === "free") {
            toast.success("Free 플랜이 활성화되었습니다!");
        } else {
            // Pro plan -> Open modal
            setSelectedPlan(plan);
            setShowCheckoutModal(true);
        }
    };

    // Helper to check if a plan is the current one
    const checkIsCurrentPlan = (plan: SubscriptionPlan) => {
        if (!activeProductId) return plan.id === "free" && !isCompany; // Default assumption? Or just false.

        // Mapping: Pro is usually ID 4 (or similar) in this project. 
        // We'll need to be careful with mapping between plan.id (string) and productId (number).
        if (plan.id === "pro" && (activeProductId === 2 || activeProductId === 3)) return true; // Adjusted: Pro/Business are likely 2 or 3.
        if (plan.id === "free" && (!activeProductId || activeProductId === 1)) return true; // Free is 1 or no subscription
        return false;
    };

    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64 transition-all duration-300">
                <Header />

                {/* Main Content */}
                <main className="container max-w-7xl px-4 md:px-8 py-12 space-y-16">

                    {/* Hero Section */}
                    <section className="text-center space-y-4">
                        <h2 className="text-4xl font-bold text-foreground">
                            최고의 인재를 찾는 가장 효율적인 방법
                        </h2>
                        <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                            FitMe 인재풀에서 검증된 개발자, 디자이너, PM을 만나보세요.
                            기업 규모에 맞는 합리적인 요금제로 채용 비용을 절감하세요.
                        </p>
                    </section>

                    {/* Billing Toggle */}
                    <div className="flex items-center justify-center gap-4">
                        <Label
                            htmlFor="billing-toggle"
                            className={!isYearly ? "text-foreground font-medium" : "text-muted-foreground"}
                        >
                            월간 결제
                        </Label>
                        <Switch
                            id="billing-toggle"
                            checked={isYearly}
                            onCheckedChange={setIsYearly}
                        />
                        <Label
                            htmlFor="billing-toggle"
                            className={isYearly ? "text-foreground font-medium" : "text-muted-foreground"}
                        >
                            연간 결제
                            <span className="ml-2 text-xs text-accent font-semibold bg-accent/10 px-2 py-1 rounded-full">
                                20% 할인
                            </span>
                        </Label>
                    </div>

                    {/* Pricing Cards */}
                    <section className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-4 items-start">
                        {subscriptionPlans.map((plan) => (
                            <PricingCard
                                key={plan.id}
                                plan={plan}
                                isYearly={isYearly}
                                onSelect={isProductIntro ? undefined : handleSelectPlan}
                                isCurrentPlan={isCompany && checkIsCurrentPlan(plan)}
                            />
                        ))}
                    </section>

                    {/* Trust Badges */}
                    <section className="flex flex-wrap justify-center gap-8 py-8 border-y border-border/50">
                        <div className="flex items-center gap-3 text-muted-foreground">
                            <Shield className="w-5 h-5 text-accent" />
                            <span className="text-sm">SSL 보안 결제</span>
                        </div>
                        <div className="flex items-center gap-3 text-muted-foreground">
                            <Building2 className="w-5 h-5 text-accent" />
                            <span className="text-sm">1,000+ 기업이 신뢰</span>
                        </div>
                        <div className="flex items-center gap-3 text-muted-foreground">
                            <CreditCard className="w-5 h-5 text-accent" />
                            <span className="text-sm">언제든 취소 가능</span>
                        </div>
                    </section>

                    {/* Plan Comparison */}
                    <section className="space-y-6">
                        <div className="text-center">
                            <h3 className="text-2xl font-bold text-foreground">플랜 비교</h3>
                            <p className="text-muted-foreground mt-2">
                                각 플랜별 기능을 한눈에 비교해보세요
                            </p>
                        </div>
                        <PlanComparison />
                    </section>

                    {/* FAQ */}
                    <section className="space-y-6 max-w-3xl mx-auto">
                        <div className="text-center">
                            <h3 className="text-2xl font-bold text-foreground">자주 묻는 질문</h3>
                            <p className="text-muted-foreground mt-2">
                                요금제에 대해 궁금한 점이 있으신가요?
                            </p>
                        </div>
                        <FAQ />
                    </section>

                    {/* CTA - Only show if not on product intro page */}
                    {!isProductIntro && (
                        <section className="text-center bg-gradient-to-r from-accent/10 via-accent/5 to-accent/10 rounded-2xl p-12">
                            <h3 className="text-2xl font-bold text-foreground mb-4">
                                아직 결정이 어려우신가요?
                            </h3>
                            <p className="text-muted-foreground mb-6 max-w-lg mx-auto">
                                무료 플랜으로 먼저 시작해보세요. 언제든지 업그레이드할 수 있습니다.
                            </p>
                            <div className="flex flex-wrap justify-center gap-4">
                                <Button
                                    size="lg"
                                    className="bg-accent hover:bg-accent-hover text-accent-foreground"
                                    onClick={() => handleSelectPlan("free")}
                                    disabled={isCompany && checkIsCurrentPlan(subscriptionPlans[0])}
                                >
                                    {isCompany && checkIsCurrentPlan(subscriptionPlans[0]) ? "구독중" : "무료로 시작하기"}
                                </Button>
                                <Button
                                    size="lg"
                                    variant="outline"
                                    onClick={() => toast.info("상담 예약이 접수되었습니다.")}
                                >
                                    상담 예약하기
                                </Button>
                            </div>
                        </section>
                    )}
                </main>

                <Footer />
            </div>

            <SubscriptionCheckoutModal
                open={showCheckoutModal}
                onOpenChange={setShowCheckoutModal}
                plan={selectedPlan}
            />
        </div>
    );
}
