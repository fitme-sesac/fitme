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
    productId: number;
    status: string;
    nextBillingDate: string;
    // Add other fields as needed
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
