import { Check, X, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { SubscriptionPlan } from "@/data/subscriptionPlans";

interface PricingCardProps {
    plan: SubscriptionPlan;
    isYearly?: boolean;
    onSelect?: (planId: string) => void;
    isCurrentPlan?: boolean;
}

export function PricingCard({ plan, isYearly, onSelect, isCurrentPlan }: PricingCardProps) {
    const displayPrice = isYearly ? Math.floor(plan.price * 0.8) : plan.price;
    const yearlyTotal = displayPrice * 12;

    return (
        <div
            className={cn(
                "relative flex flex-col rounded-2xl border bg-card p-6 transition-all duration-300",
                plan.isPopular
                    ? "border-accent shadow-lg shadow-accent/10 scale-105 z-10"
                    : "border-border/50 hover:border-accent/50 hover:shadow-md",
                isCurrentPlan && "ring-2 ring-emerald-500 border-emerald-500 shadow-lg shadow-emerald-500/10"
            )}
        >
            {plan.isPopular && !isCurrentPlan && (
                <Badge className="absolute -top-3 left-1/2 -translate-x-1/2 bg-accent text-accent-foreground px-4 py-1">
                    <Sparkles className="w-3 h-3 mr-1" />
                    추천
                </Badge>
            )}

            {isCurrentPlan && (
                <Badge className="absolute -top-3 left-1/2 -translate-x-1/2 bg-emerald-500 text-white px-4 py-1">
                    <Check className="w-3 h-3 mr-1" />
                    구독중
                </Badge>
            )}

            <div className="mb-6">
                <h3 className="text-xl font-bold text-foreground">{plan.name}</h3>
                <p className="text-sm text-muted-foreground mt-1">{plan.description}</p>
            </div>

            <div className="mb-6">
                {plan.price === 0 ? (
                    <div className="flex items-baseline">
                        <span className="text-4xl font-bold text-foreground">무료</span>
                    </div>
                ) : (
                    <div className="flex items-baseline">
                        <span className="text-4xl font-bold text-foreground">
                            ₩{displayPrice.toLocaleString()}
                        </span>
                        <span className="text-muted-foreground ml-2">/월</span>
                    </div>
                )}
                {isYearly && plan.price > 0 && (
                    <p className="text-sm text-accent mt-1">
                        연간 결제 시 ₩{yearlyTotal.toLocaleString()} (20% 할인)
                    </p>
                )}
            </div>

            {!onSelect ? null : (
                <Button
                    onClick={() => !isCurrentPlan && onSelect(plan.id)}
                    disabled={isCurrentPlan}
                    className={cn(
                        "w-full mb-6",
                        isCurrentPlan
                            ? "bg-emerald-500 hover:bg-emerald-600 text-white opacity-100"
                            : plan.isPopular
                                ? "bg-accent hover:bg-accent-hover text-accent-foreground"
                                : "bg-muted hover:bg-muted/80 text-foreground"
                    )}
                    size="lg"
                >
                    {isCurrentPlan ? "구독중" : plan.ctaText}
                </Button>
            )}

            <div className="space-y-3 flex-1">
                {plan.features.map((feature) => (
                    <div key={feature} className="flex items-start gap-3">
                        <div className={cn(
                            "flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center mt-0.5",
                            plan.highlightedFeatures?.includes(feature)
                                ? "bg-accent/20 text-accent"
                                : "bg-muted text-muted-foreground"
                        )}>
                            <Check className="w-3 h-3" />
                        </div>
                        <span className={cn(
                            "text-sm",
                            plan.highlightedFeatures?.includes(feature)
                                ? "text-foreground font-medium"
                                : "text-muted-foreground"
                        )}>
                            {feature}
                        </span>
                    </div>
                ))}

                {plan.limitations?.map((limitation) => (
                    <div key={limitation} className="flex items-start gap-3">
                        <div className="flex-shrink-0 w-5 h-5 rounded-full flex items-center justify-center mt-0.5 bg-destructive/10 text-destructive">
                            <X className="w-3 h-3" />
                        </div>
                        <span className="text-sm text-muted-foreground">{limitation}</span>
                    </div>
                ))}
            </div>
        </div>
    );
}
