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

/**
 * 결제 취소
 * @param {string} orderId - 주문 ID
 * @param {string} cancelReason - 취소 사유
 * @returns {Promise<void>}
 */
export async function cancelPayment(orderId, cancelReason) {
    const response = await http.post(`/api/v1/payments/${orderId}/cancel`, {
        cancelReason
    });
    return response.data;
}
