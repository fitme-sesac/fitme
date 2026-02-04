import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
    createSubscription,
    getSubscription,
    getSubscriptionsByEmployer,
    SubscriptionCreateRequest
} from "@/api/subscription";

// 구독 생성 Hook
export function useCreateSubscription() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (data: SubscriptionCreateRequest) => createSubscription(data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["subscriptions"] });
            queryClient.invalidateQueries({ queryKey: ["my-subscription"] });
        },
    });
}

// 내 구독 상세 조회 Hook
export function useSubscription(subscriptionId: number) {
    return useQuery({
        queryKey: ["subscription", subscriptionId],
        queryFn: () => getSubscription(subscriptionId),
        enabled: !!subscriptionId,
    });
}

// 기업의 구독 목록 조회 Hook
export function useEmployerSubscriptions(employerId: number) {
    return useQuery({
        queryKey: ["employer-subscriptions", employerId],
        queryFn: () => getSubscriptionsByEmployer(employerId),
        enabled: !!employerId,
    });
}
