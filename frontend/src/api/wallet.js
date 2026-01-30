import { http } from "./http";

/**
 * Wallet API
 */

export async function getMyWallet(roleType = "CANDIDATE") {
    const response = await http.get("/api/v1/wallets/me", {
        params: { roleType }
    });
    return response.data;
}

export async function getMyLedgers(roleType = "CANDIDATE") {
    const response = await http.get("/api/v1/wallets/me/ledgers", {
        params: { roleType }
    });
    return response.data;
}

export async function getMyMonthlyLedgers(year, month, roleType = "CANDIDATE") {
    const response = await http.get("/api/v1/wallets/me/ledgers/monthly", {
        params: { year, month, roleType }
    });
    return response.data;
}
