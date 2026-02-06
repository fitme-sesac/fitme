import { http } from "./http";


// === Dashboard ===
export const getAdminStats = async () => {
    const response = await http.get("/api/v1/admin/dashboard/stats");
    return response.data;
};

// === Members ===
export const getMembers = async (params: { page: number; size: number; search?: string }) => {
    const response = await http.get("/api/v1/admin/members", { params });
    return response.data;
};

export const updateMemberStatus = async (memberId: number, status: string) => {
    const response = await http.patch(`/api/v1/admin/members/${memberId}/status`, { status });
    return response.data;
};

// === Jobs ===
export const getAdminJobs = async (params: { page: number; size: number; status?: string; search?: string }) => {
    const response = await http.get("/api/v1/admin/jobs", { params });
    return response.data;
};

export const updateJobStatus = async (jobId: number, status: string) => {
    const response = await http.patch(`/api/v1/admin/jobs/${jobId}/status`, { status });
    return response.data;
};

// === Employers ===
export const getEmployers = async (params: { page: number; size: number; search?: string }) => {
    const response = await http.get("/api/v1/admin/employers", { params });
    return response.data;
};

export const verifyEmployer = async (employerId: number, action: 'APPROVE' | 'REJECT') => {
    const response = await http.patch(`/api/v1/admin/employers/${employerId}/verify`, { action });
    return response.data;
};

// === Reports (Using existing API) ===
export const getReports = async (status: string, params: { page: number; size: number }) => {
    // UI(프론트) 상태값 <-> 백엔드 표준 상태값 매핑
    // - PENDING  -> OPEN
    // - RESOLVED -> ACCEPTED
    const s = (status || "").toUpperCase();
    const normalized = s === "PENDING" ? "OPEN" : (s === "RESOLVED" ? "ACCEPTED" : s);
    const response = await http.get(`/api/v1/reports/status/${normalized}`, { params });
    return response.data;
};

export const processReport = async (
    reportId: number,
    data: { decision: 'ACCEPT' | 'REJECT'; violationType?: string; restrictDays?: number; reason?: string }
) => {
    const response = await http.post(`/api/v1/reports/${reportId}/process`, data);
    return response.data;
};

// === Inquiries (FAQ) ===
export const getInquiries = async (params: { page: number; size: number }) => {
    const response = await http.get("/api/v1/faqs/admin/list", { params });
    return response.data;
};

export const getFAQStatistics = async () => {
    const response = await http.get("/api/v1/faqs/admin/statistics");
    return response.data;
};

// === Subscriptions ===
export const getAdminSubscriptions = async () => {
    // 백엔드 AdminSubscriptionController 기준
    const response = await http.get("/api/v1/admin/subscriptions");
    return response.data;
};

// === Notices ===
export const createNotice = async (data: { title: string; body: string; noticeType: string; isPublic: boolean; status: string }) => {
    const response = await http.post("/api/v1/notices/admin", data);
    return response.data;
};

export const getAdminNotices = async (params: { page: number; size: number }) => {
    const response = await http.get("/api/v1/notices/admin/list", { params });
    return response.data;
};

// === Notifications ===
export const getAdminNotifications = async () => {
    const response = await http.get("/api/v1/admin/notifications");
    return response.data;
};

