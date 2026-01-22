import { useEffect, useState } from "react";
import { useNotifications } from "../hooks/useNotifications";
import { getNotificationIcon, getNotificationBgColor, getRelativeTime } from "../types/notification";
import "./NotificationPage.css";

/**
 * 알림 전체 목록 페이지
 */
export default function NotificationPage() {
    const {
        notifications,
        unreadCount,
        loading,
        error,
        hasNext,
        fetchNotifications,
        loadMore,
        markAsRead,
        markAllAsRead,
        deleteNotification
    } = useNotifications();

    const [selectedFilter, setSelectedFilter] = useState("all"); // all, unread

    // 초기 알림 로드
    useEffect(() => {
        fetchNotifications(0, 20);
    }, [fetchNotifications]);

    // 알림 클릭 핸들러
    const handleNotificationClick = async (notification) => {
        if (!notification.isRead) {
            await markAsRead(notification.id);
        }
        // 링크가 있으면 해당 페이지로 이동
        if (notification.linkUrl) {
            window.location.href = notification.linkUrl;
        }
    };

    // 알림 읽음만 처리 (페이지 이동 없이)
    const handleMarkAsReadOnly = async (e, notification) => {
        e.stopPropagation();
        if (!notification.isRead) {
            await markAsRead(notification.id);
        }
    };

    // 알림 삭제 핸들러
    const handleDelete = async (e, notificationId) => {
        e.stopPropagation();
        if (window.confirm("이 알림을 삭제하시겠습니까?")) {
            await deleteNotification(notificationId);
        }
    };

    // 필터링된 알림
    const filteredNotifications = selectedFilter === "unread"
        ? notifications.filter(n => !n.isRead)
        : notifications;

    return (
        <div className="notification-page">
            <div className="container py-4">
                {/* 페이지 헤더 */}
                <div className="notification-page-header">
                    <h1>알림</h1>
                    <div className="notification-page-actions">
                        {unreadCount > 0 && (
                            <button 
                                className="btn btn-outline-primary btn-sm"
                                onClick={markAllAsRead}
                            >
                                모두 읽음 처리
                            </button>
                        )}
                    </div>
                </div>

                {/* 필터 탭 */}
                <div className="notification-filter-tabs">
                    <button 
                        className={`filter-tab ${selectedFilter === "all" ? "active" : ""}`}
                        onClick={() => setSelectedFilter("all")}
                    >
                        전체 ({notifications.length})
                    </button>
                    <button 
                        className={`filter-tab ${selectedFilter === "unread" ? "active" : ""}`}
                        onClick={() => setSelectedFilter("unread")}
                    >
                        읽지 않음 ({unreadCount})
                    </button>
                </div>

                {/* 에러 메시지 */}
                {error && (
                    <div className="alert alert-danger" role="alert">
                        {error}
                    </div>
                )}

                {/* 알림 목록 */}
                <div className="notification-list-container">
                    {loading && notifications.length === 0 ? (
                        <div className="notification-loading-full">
                            <div className="spinner-border text-primary" role="status">
                                <span className="visually-hidden">로딩중...</span>
                            </div>
                        </div>
                    ) : filteredNotifications.length === 0 ? (
                        <div className="notification-empty-full">
                            <i className="bi bi-bell-slash"></i>
                            <h3>알림이 없습니다</h3>
                            <p>새로운 알림이 오면 여기에 표시됩니다.</p>
                        </div>
                    ) : (
                        <>
                            {filteredNotifications.map(notification => (
                                <NotificationListItem
                                    key={notification.id}
                                    notification={notification}
                                    onClick={() => handleNotificationClick(notification)}
                                    onMarkAsRead={(e) => handleMarkAsReadOnly(e, notification)}
                                    onDelete={(e) => handleDelete(e, notification.id)}
                                />
                            ))}
                            
                            {/* 더보기 버튼 */}
                            {hasNext && selectedFilter === "all" && (
                                <div className="notification-load-more">
                                    <button 
                                        className="btn btn-outline-secondary"
                                        onClick={loadMore}
                                        disabled={loading}
                                    >
                                        {loading ? (
                                            <>
                                                <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                                                로딩중...
                                            </>
                                        ) : (
                                            "더 보기"
                                        )}
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

/**
 * 알림 목록 항목 컴포넌트
 */
function NotificationListItem({ notification, onClick, onMarkAsRead, onDelete }) {
    const iconClass = getNotificationIcon(notification.eventType);
    const bgColorClass = getNotificationBgColor(notification.eventType);
    const timeAgo = getRelativeTime(notification.createdAt);

    return (
        <div 
            className={`notification-list-item ${notification.isRead ? "read" : "unread"}`}
            onClick={onClick}
            role="button"
            tabIndex={0}
        >
            {/* 아이콘 */}
            <div className={`notification-list-icon ${bgColorClass}`}>
                <i className={iconClass}></i>
            </div>

            {/* 내용 */}
            <div className="notification-list-content">
                <div className="notification-list-header">
                    <span className="notification-list-title">{notification.title}</span>
                    <span className="notification-list-time">{timeAgo}</span>
                </div>
                <p className="notification-list-message">{notification.message}</p>
            </div>

            {/* 액션 버튼 */}
            <div className="notification-list-actions">
                {!notification.isRead && (
                    <button 
                        className="notification-read-btn"
                        onClick={onMarkAsRead}
                        title="읽음 처리"
                    >
                        <i className="bi bi-check2"></i>
                    </button>
                )}
                <button 
                    className="notification-delete-btn"
                    onClick={onDelete}
                    title="삭제"
                >
                    <i className="bi bi-trash"></i>
                </button>
            </div>
        </div>
    );
}
