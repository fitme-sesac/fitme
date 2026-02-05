export interface SubscriptionPlan {
    id: string; // "SUB_BASIC", "SUB_STANDARD", "SUB_PRO" etc.
    productId: number; // Real DB ID
    name: string;
    description: string;
    price: number;
    billingCycle: "monthly" | "yearly";
    features: string[];
    highlightedFeatures?: string[];
    limitations?: string[];
    isPopular?: boolean;
    ctaText: string;
}
