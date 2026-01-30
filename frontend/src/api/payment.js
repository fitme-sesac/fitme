import { http } from "./http";

/**
 * Payment API
 */

export async function createPayment(data) {
    const response = await http.post("/api/v1/payments/request", data);
    return response.data;
}

export async function confirmPayment(data) {
    const response = await http.post("/api/v1/payments/confirm", data);
    return response.data;
}

export async function getMyPayments(roleType = "CANDIDATE") {
    const response = await http.get("/api/v1/payments/me", {
        params: { roleType }
    });
    return response.data;
}

export async function getPaymentDetail(orderId) {
    const response = await http.get(`/api/v1/payments/${orderId}`);
    return response.data;
}
