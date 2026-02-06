import { http } from "./http";


// === Dashboard ===
export const getAdminStats = async () => {
    const response = await http.get("/api/v1/admin/dashboard/stats");
    return response.data;
};

// === Members ===
export const getMembers = async (params: { page: number; size: number; search?: string; status?: string }) => {
    const response = await http.get("/api/v1/admin/members", { params });
    return response.data;
};

export const getMember = async (memberId: number) => {
    const response = await http.get(`/api/v1/admin/members/${memberId}`);
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

export const deleteJobPermanent = async (jobId: number) => {
    const response = await http.delete(`/api/v1/admin/jobs/${jobId}`);
    return response.data;
};

// === Employers ===
export const getEmployers = async (params: { page: number; size: number; search?: string; status?: string }) => {
    const response = await http.get("/api/v1/admin/employers", { params });
    return response.data;
};

export const getEmployer = async (employerId: number) => {
    const response = await http.get(`/api/v1/admin/employers/${employerId}`);
    return response.data;
};

export const verifyEmployer = async (employerId: number, action: 'APPROVE' | 'REJECT') => {
    const response = await http.patch(`/api/v1/admin/employers/${employerId}/verify`, { action });
    return response.data;
};

// === Reports (Using existing API) ===
// Map frontend tab status to backend: PENDING->OPEN, RESOLVED->ACCEPTED, REJECTED->REJECTED
const REPORT_STATUS_TO_BACKEND: Record<string, string> = {
    PENDING: "OPEN",
    RESOLVED: "ACCEPTED",
    REJECTED: "REJECTED",
};

export const getReports = async (status: string, params: { page: number; size: number }) => {
    const backendStatus = REPORT_STATUS_TO_BACKEND[status] ?? status;
    const response = await http.get(`/api/v1/reports/status/${backendStatus}`, { params });
    return response.data;
};

export const processReport = async (
    reportId: number,
    data: { decision: string; violationType?: string; reason?: string }
) => {
    const response = await http.post(`/api/v1/reports/${reportId}/process`, data);
    return response.data;
};

// === Inquiries (FAQ) ===
export const getInquiries = async (params: {
    page: number;
    size: number;
    isPublic?: boolean;
    question?: string; // Add question for searching
}) => {
    const { isPublic, ...rest } = params;
    const url =
        isPublic !== undefined
            ? "/api/v1/faqs/admin/by-public"
            : "/api/v1/faqs/admin/list";
    const response = await http.get(url, {
        params: isPublic !== undefined ? { ...rest, isPublic } : rest,
    });
    return response.data;
};

export const getFAQAdmin = async (faqId: number) => {
    const response = await http.get(`/api/v1/faqs/admin/${faqId}`);
    return response.data;
};

export const updateFAQ = async (
    faqId: number,
    data: { question: string; answer: string; isPublic: boolean; locked?: boolean }
) => {
    const response = await http.put(`/api/v1/faqs/admin/${faqId}`, data);
    return response.data;
};

export const deleteFAQ = async (faqId: number) => {
    const response = await http.delete(`/api/v1/faqs/admin/${faqId}`);
    return response.data;
};

export const getFAQStatistics = async () => {
    const response = await http.get("/api/v1/faqs/admin/statistics");
    return response.data;
};

// === Subscriptions ===
export const getAdminSubscriptions = async () => {
    const response = await http.get("/api/v1/admin/subscriptions");
    return response.data;
};

// === Notices ===
export const createNotice = async (data: { title: string; body: string; noticeType: string; isPublic: boolean; status: string }) => {
    const response = await http.post("/api/v1/notices/admin", data);
    return response.data;
};

export const getAdminNotices = async (params: { page: number; size: number; noticeType?: string; title?: string }) => {
    const response = await http.get("/api/v1/notices/admin/list", { params });
    return response.data;
};

export const getNoticeAdmin = async (noticeId: number) => {
    const response = await http.get(`/api/v1/notices/admin/${noticeId}`);
    return response.data;
};

export const updateNotice = async (
    noticeId: number,
    data: { title: string; body: string; noticeType: string; isPublic: boolean; status: string }
) => {
    const response = await http.put(`/api/v1/notices/admin/${noticeId}`, data);
    return response.data;
};

export const deleteNotice = async (noticeId: number) => {
    const response = await http.delete(`/api/v1/notices/admin/${noticeId}`);
    return response.data;
};

// === Notifications ===
export const getAdminNotifications = async () => {
    const response = await http.get("/api/v1/admin/notifications");
    return response.data;
};
