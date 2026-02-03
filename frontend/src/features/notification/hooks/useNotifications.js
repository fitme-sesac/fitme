import { useState, useEffect, useCallback, useRef } from "react";
import { notificationApi } from "../api/notificationApi";

/**
 * 알림 관리 커스텀 훅
 */
export function useNotifications() {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [hasNext, setHasNext] = useState(false);
    const [page, setPage] = useState(0);

    // 폴링 인터벌 ref
    const pollingRef = useRef(null);

    /**
     * 읽지 않은 알림 수 조회
     */
    const fetchUnreadCount = useCallback(async () => {
        try {
            const response = await notificationApi.getUnreadCount();
            // 백엔드: { success, data: { unreadCount } }
            const count = response?.data?.unreadCount ?? response?.unreadCount ?? 0;
            setUnreadCount(Number(count) || 0);
        } catch (err) {
            // 로그인하지 않은 경우, API 미구현(500), 또는 Not Found(404) 에러 무시
            const status = err.response?.status;
            if (status !== 401 && status !== 403 && status !== 404 && status !== 500) {
                console.error("알림 수 조회 실패:", err);
            }
            // API가 없는 경우 unreadCount를 0으로 유지
            setUnreadCount(0);
        }
    }, []);

    /**
     * 최근 알림 조회 (드롭다운용)
     */
    const fetchRecentNotifications = useCallback(async (limit = 10) => {
        setLoading(true);
        setError(null);
        try {
            const response = await notificationApi.getRecentNotifications(limit);
            // 백엔드: { success, data: notifications[] }
            const list = Array.isArray(response?.data) ? response.data : (response || []);
            setNotifications(list);
        } catch (err) {
            const status = err.response?.status;
            // 로그인하지 않은 경우, API 미구현(500), 또는 Not Found(404) 에러 무시
            if (status !== 401 && status !== 403 && status !== 404 && status !== 500) {
                setError("알림을 불러오는데 실패했습니다.");
                console.error("알림 조회 실패:", err);
            }
            // API가 없는 경우 빈 배열로 설정
            setNotifications([]);
        } finally {
            setLoading(false);
        }
    }, []);

    /**
     * 알림 목록 조회 (페이징)
     */
    const fetchNotifications = useCallback(async (pageNum = 0, size = 20) => {
        setLoading(true);
        setError(null);
        try {
            const response = await notificationApi.getNotifications(pageNum, size);
            // 백엔드: { success, data: content[], page: { hasNext, ... } }
            const list = Array.isArray(response?.data) ? response.data : [];
            const hasMore = response?.page?.hasNext ?? false;
            if (pageNum === 0) {
                setNotifications(list);
            } else {
                setNotifications(prev => [...prev, ...list]);
            }
            setHasNext(hasMore);
            if (response?.page?.totalElements !== undefined && pageNum === 0) {
                // unreadCount는 별도 API로만 제공되므로 유지
            }
            setPage(pageNum);
        } catch (err) {
            const status = err.response?.status;
            // 로그인하지 않은 경우, API 미구현(500), 또는 Not Found(404) 에러 무시
            if (status !== 401 && status !== 403 && status !== 404 && status !== 500) {
                setError("알림을 불러오는데 실패했습니다.");
                console.error("알림 조회 실패:", err);
            }
            // API가 없는 경우 빈 배열로 설정
            if (pageNum === 0) {
                setNotifications([]);
            }
        } finally {
            setLoading(false);
        }
    }, []);

    /**
     * 더 많은 알림 로드
     */
    const loadMore = useCallback(() => {
        if (!loading && hasNext) {
            fetchNotifications(page + 1);
        }
    }, [loading, hasNext, page, fetchNotifications]);

    /**
     * 특정 알림 읽음 처리 (403/500 시에도 UI만 낙관적 업데이트)
     */
    const markAsRead = useCallback(async (notificationId) => {
        const status = err => err?.response?.status;
        setNotifications(prev =>
            prev.map(n =>
                n.id === notificationId ? { ...n, isRead: true, status: "READ" } : n
            )
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
        try {
            await notificationApi.markAsRead(notificationId);
        } catch (err) {
            if (status(err) !== 403 && status(err) !== 404 && status(err) !== 500) {
                console.error("알림 읽음 처리 실패:", err);
            }
        }
    }, []);

    /**
     * 모든 알림 읽음 처리 (403/500 시에도 UI만 낙관적 업데이트)
     */
    const markAllAsRead = useCallback(async () => {
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true, status: "READ" })));
        setUnreadCount(0);
        try {
            await notificationApi.markAllAsRead();
        } catch (err) {
            const code = err?.response?.status;
            if (code !== 403 && code !== 404 && code !== 500) {
                console.error("전체 읽음 처리 실패:", err);
            }
        }
    }, []);

    /**
     * 알림 삭제
     */
    const deleteNotification = useCallback(async (notificationId) => {
        try {
            await notificationApi.deleteNotification(notificationId);
            
            // 로컬 상태에서 제거
            setNotifications(prev => prev.filter(n => n.id !== notificationId));
        } catch (err) {
            console.error("알림 삭제 실패:", err);
        }
    }, []);

    /**
     * 새로고침
     */
    const refresh = useCallback(() => {
        fetchUnreadCount();
        fetchRecentNotifications();
    }, [fetchUnreadCount, fetchRecentNotifications]);

    /**
     * 폴링 시작 (30초마다 읽지 않은 알림 수만 확인, 한 번만 설정)
     */
    const startPolling = useCallback((interval = 30000) => {
        if (pollingRef.current) return;
        fetchUnreadCount();
        pollingRef.current = setInterval(() => {
            fetchUnreadCount();
        }, interval);
    }, [fetchUnreadCount]);

    /**
     * 폴링 중지
     */
    const stopPolling = useCallback(() => {
        if (pollingRef.current) {
            clearInterval(pollingRef.current);
            pollingRef.current = null;
        }
    }, []);

    // 컴포넌트 언마운트 시 폴링 중지
    useEffect(() => {
        return () => {
            stopPolling();
        };
    }, [stopPolling]);

    return {
        notifications,
        unreadCount,
        loading,
        error,
        hasNext,
        fetchUnreadCount,
        fetchRecentNotifications,
        fetchNotifications,
        loadMore,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        refresh,
        startPolling,
        stopPolling
    };
}

export default useNotifications;
