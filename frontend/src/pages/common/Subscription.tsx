import { useState, useEffect } from "react";
import { CreditCard, Building2, Shield, ArrowRight, Loader2 } from "lucide-react";
import { getPlanDetails } from "@/utils/subscriptionUtils";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
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
import { fetchProducts, Product } from "@/api/product";
import { useQuery } from "@tanstack/react-query";
import { SubscriptionPlan } from "@/types/subscription";
import { scheduleProductChange } from "@/api/subscription";
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

    // --- Real Product Data Fetching ---
    const { data: productPage, isLoading: validProductsLoading } = useQuery({
        queryKey: ["products", "SUBSCRIPTION"],
        queryFn: () => fetchProducts("SUBSCRIPTION"),
    });

    // DB 상품 데이터를 UI 포맷으로 변환
    const subscriptionPlans: SubscriptionPlan[] = (productPage?.content || [])
        .filter((product) => {
            const code = product.productCode;
            return (
                product.productType === 'SUBSCRIPTION' &&
                (code.includes('BASIC') || code.includes('STANDARD') || code.includes('PRO'))
            );
        })
        .map((product) => {
            // 상품 코드나 이름에 따라 UI 속성 매핑 (이 부분은 하드코딩 필요할 수 있음)
            const { name, description, benefits, highlighted, isPopular } = getPlanDetails(product.productCode, product.creditAmount);

            return {
                id: product.productCode,
                productId: product.productId,
                name: name,
                description: description,
                price: product.priceAmount,
                billingCycle: "monthly" as const,
                features: benefits,
                highlightedFeatures: highlighted,
                isPopular: isPopular,
                ctaText: "시작하기",
            };
        }).sort((a, b) => a.price - b.price); // 가격 오름차순 정렬


    const handleSelectPlan = async (plan: SubscriptionPlan) => {
        // 이미 구독 중인 경우 플랜 변경 로직 수행 (Free 포함 모든 플랜)
        if (isCompany && activeSubscription) {

            if (activeSubscription.product?.productId === plan.productId) {
                return; // 이미 해당 플랜 사용 중
            }

            // 변경 확인 메시지
            const confirmMsg = `${plan.name}로 플랜을 변경하시겠습니까?\n변경 사항은 다음 결제일부터 적용됩니다.`;

            if (confirm(confirmMsg)) {
                const toastId = toast.loading("플랜 변경 예약 중...");
                try {
                    await scheduleProductChange(activeSubscription.subscriptionId, plan.productId);
                    toast.success("플랜 변경이 예약되었습니다. 다음 결제일에 반영됩니다.", { id: toastId });
                } catch (error) {
                    console.error(error);
                    toast.error("플랜 변경 예약에 실패했습니다.", { id: toastId });
                }
            }
            return;
        }

        // 신규 구독 (또는 비로그인)
        // Pro plan -> Open modal
        setSelectedPlan(plan);
        setShowCheckoutModal(true);
    };

    // Helper to check if a plan is the current one
    const checkIsCurrentPlan = (plan: SubscriptionPlan) => {
        if (!activeProductId) return false;
        return activeProductId === plan.productId;
    };

    if (validProductsLoading) {
        return (
            <div className="min-h-screen bg-background flex flex-col">
                <Sidebar />
                <div className="lg:pl-64 flex-1 flex flex-col">
                    <Header />
                    <div className="flex-1 flex items-center justify-center">
                        <Loader2 className="h-10 w-10 animate-spin text-primary" />
                    </div>
                </div>
            </div>
        )
    }

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

                    {/* Billing Toggle (Real data is monthly only for now, but keeping UI) */}
                    {!isProductIntro && (
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
                                disabled={true} // DB data is monthly based for now
                            />
                            <Label
                                htmlFor="billing-toggle"
                                className={isYearly ? "text-foreground font-medium" : "text-muted-foreground"}
                            >
                                연간 결제
                                <span className="ml-2 text-xs text-accent font-semibold bg-accent/10 px-2 py-1 rounded-full">
                                    준비중
                                </span>
                            </Label>
                        </div>
                    )}

                    {/* Pricing Cards */}
                    <section className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-4 items-start">
                        {subscriptionPlans.map((plan) => (
                            <PricingCard
                                key={plan.id}
                                plan={plan}
                                isYearly={isYearly}
                                onSelect={isProductIntro ? undefined : () => handleSelectPlan(plan)}
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
