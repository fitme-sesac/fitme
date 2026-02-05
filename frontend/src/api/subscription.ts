import { http } from "./http";

export interface SubscriptionCreateRequest {
    employerId: number;
    productId: number;
    billingKey?: string;
    authKey?: string;
    customerKey: string;
}

export interface SubscriptionResponse {
    subscriptionId: number;
    employerId: number;
    product: {
        productId: number;
        productCode: string;
        name: string;
        priceAmount: number;
        planTier?: string;
        creditAmount: number;
    };
    nextProduct?: {
        productId: number;
        productCode: string;
        name: string;
        creditAmount: number;
    };
    status: string;
    nextBillingAt: string;
    startedAt: string;
    endedAt?: string;
    cardCompany?: string;
    cardNumber?: string;
}

export async function createSubscription(data: SubscriptionCreateRequest): Promise<SubscriptionResponse> {
    const res = await http.post("/api/subscriptions", data);
    return res.data;
}

export async function getSubscription(subscriptionId: number): Promise<SubscriptionResponse> {
    const res = await http.get(`/api/subscriptions/${subscriptionId}`);
    return res.data;
}

export async function getSubscriptionsByEmployer(employerId: number): Promise<SubscriptionResponse[]> {
    const res = await http.get(`/api/subscriptions/employer/${employerId}`);
    return res.data;
}

export async function cancelSubscription(subscriptionId: number): Promise<void> {
    await http.post(`/api/subscriptions/${subscriptionId}/cancel`);
}

export async function resumeSubscription(subscriptionId: number): Promise<void> {
    await http.post(`/api/subscriptions/${subscriptionId}/resume`);
}

export async function scheduleProductChange(subscriptionId: number, newProductId: number): Promise<void> {
    await http.post(`/api/subscriptions/${subscriptionId}/change-product`, { newProductId });
}

export async function cancelScheduledProductChange(subscriptionId: number): Promise<void> {
    await http.post(`/api/subscriptions/${subscriptionId}/cancel-scheduled-product-change`);
}

export async function updateBillingInfo(subscriptionId: number, data: { authKey: string; customerKey: string }): Promise<void> {
    await http.put(`/api/subscriptions/${subscriptionId}/billing-info`, data);
}
