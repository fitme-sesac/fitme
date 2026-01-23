import { http } from "../../../api/http";

/**
 * 알림 API
 */
export const notificationApi = {
    /**
     * 알림 목록 조회 (페이징)
     */
    getNotifications: async (page = 0, size = 20) => {
        const response = await http.get("/api/notifications", {
            params: { page, size }
        });
        return response.data;
    },

    /**
     * 최근 알림 조회 (드롭다운용)
     */
    getRecentNotifications: async (limit = 10) => {
        const response = await http.get("/api/notifications/recent", {
            params: { limit }
        });
        return response.data;
    },

    /**
     * 읽지 않은 알림 수 조회
     */
    getUnreadCount: async () => {
        const response = await http.get("/api/notifications/unread-count");
        return response.data;
    },

    /**
     * 특정 알림 읽음 처리
     */
    markAsRead: async (notificationId) => {
        const response = await http.patch(`/api/notifications/${notificationId}/read`);
        return response.data;
    },

    /**
     * 모든 알림 읽음 처리
     */
    markAllAsRead: async () => {
        const response = await http.patch("/api/notifications/read-all");
        return response.data;
    },

    /**
     * 알림 삭제
     */
    deleteNotification: async (notificationId) => {
        const response = await http.delete(`/api/notifications/${notificationId}`);
        return response.data;
    }
};

export default notificationApi;
