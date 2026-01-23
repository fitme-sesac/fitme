import { getNotificationIcon, getNotificationBgColor, getRelativeTime } from "../types/notification";
import "./NotificationDropdown.css";

/**
 * 알림 드롭다운 컴포넌트
 */
export default function NotificationDropdown({
    notifications,
    loading,
    unreadCount,
    onNotificationClick,
    onMarkAllAsRead,
    onViewAll,
    onClose
}) {
    return (
        <div className="notification-dropdown">
            {/* 헤더 */}
            <div className="notification-dropdown-header">
                <span className="notification-dropdown-title">
                    알림
                    {unreadCount > 0 && (
                        <span className="notification-unread-badge">{unreadCount}</span>
                    )}
                </span>
                {unreadCount > 0 && (
                    <button 
                        className="notification-mark-all-read"
                        onClick={onMarkAllAsRead}
                    >
                        모두 읽음
                    </button>
                )}
            </div>

            {/* 알림 목록 */}
            <div className="notification-dropdown-list">
                {loading ? (
                    <div className="notification-loading">
                        <div className="spinner-border spinner-border-sm text-primary" role="status">
                            <span className="visually-hidden">로딩중...</span>
                        </div>
                    </div>
                ) : notifications.length === 0 ? (
                    <div className="notification-empty">
                        <i className="bi bi-bell-slash"></i>
                        <p>알림이 없습니다</p>
                    </div>
                ) : (
                    notifications.map((notification) => (
                        <NotificationItem
                            key={notification.id}
                            notification={notification}
                            onClick={() => onNotificationClick(notification)}
                        />
                    ))
                )}
            </div>

            {/* 푸터 */}
            <div className="notification-dropdown-footer">
                <button 
                    className="notification-view-all"
                    onClick={onViewAll}
                >
                    모든 알림 보기
                </button>
            </div>
        </div>
    );
}

/**
 * 개별 알림 항목 컴포넌트
 */
function NotificationItem({ notification, onClick }) {
    const iconClass = getNotificationIcon(notification.eventType);
    const bgColorClass = getNotificationBgColor(notification.eventType);
    const timeAgo = getRelativeTime(notification.createdAt);

    return (
        <div 
            className={`notification-item ${notification.isRead ? "read" : "unread"}`}
            onClick={onClick}
            role="button"
            tabIndex={0}
            onKeyPress={(e) => e.key === "Enter" && onClick()}
        >
            {/* 아이콘 */}
            <div className={`notification-icon ${bgColorClass}`}>
                <i className={iconClass}></i>
            </div>

            {/* 내용 */}
            <div className="notification-content">
                <div className="notification-title">{notification.title}</div>
                <div className="notification-message">{notification.message}</div>
                <div className="notification-time">{timeAgo}</div>
            </div>

            {/* 읽지 않음 표시 */}
            {!notification.isRead && (
                <div className="notification-unread-dot"></div>
            )}
        </div>
    );
}
