import { useState, useEffect, useRef } from "react";
import { useNotifications } from "../hooks/useNotifications";
import NotificationDropdown from "./NotificationDropdown";
import "./NotificationBell.css";

/**
 * 알림 벨 아이콘 컴포넌트
 * - 읽지 않은 알림 수 배지 표시
 * - 클릭 시 드롭다운 표시
 */
export default function NotificationBell({ isAuthenticated }) {
    const [isOpen, setIsOpen] = useState(false);
    const bellRef = useRef(null);
    const dropdownRef = useRef(null);
    
    const {
        notifications,
        unreadCount,
        loading,
        fetchRecentNotifications,
        fetchUnreadCount,
        markAsRead,
        markAllAsRead,
        startPolling,
        stopPolling
    } = useNotifications();

    // 인증된 사용자만 폴링 시작
    useEffect(() => {
        if (isAuthenticated) {
            startPolling(30000); // 30초마다 폴링
            return () => stopPolling();
        }
    }, [isAuthenticated, startPolling, stopPolling]);

    // 드롭다운 열 때 최근 알림 로드
    useEffect(() => {
        if (isOpen && isAuthenticated) {
            fetchRecentNotifications(10);
        }
    }, [isOpen, isAuthenticated, fetchRecentNotifications]);

    // 외부 클릭 감지하여 드롭다운 닫기
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (
                isOpen &&
                bellRef.current &&
                !bellRef.current.contains(event.target) &&
                dropdownRef.current &&
                !dropdownRef.current.contains(event.target)
            ) {
                setIsOpen(false);
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [isOpen]);

    // 벨 아이콘 클릭 핸들러
    const handleBellClick = (e) => {
        e.preventDefault();
        e.stopPropagation();
        
        if (!isAuthenticated) {
            // 로그인 페이지로 이동
            window.location.href = "/Login";
            return;
        }
        
        setIsOpen(!isOpen);
    };

    // 알림 항목 클릭 핸들러
    const handleNotificationClick = async (notification) => {
        // 읽지 않은 알림이면 읽음 처리
        if (!notification.isRead) {
            await markAsRead(notification.id);
            // 읽음 처리 후 unread count 갱신
            await fetchUnreadCount();
        }
        
        // 링크가 있으면 해당 페이지로 이동
        if (notification.linkUrl) {
            setIsOpen(false);
            window.location.href = notification.linkUrl;
        } else {
            // 링크가 없으면 드롭다운만 닫기
            setIsOpen(false);
        }
    };

    // 모두 읽음 처리 핸들러
    const handleMarkAllAsRead = async () => {
        await markAllAsRead();
        fetchUnreadCount();
    };

    // 더보기 클릭 핸들러
    const handleViewAll = () => {
        window.location.href = "/notifications";
        setIsOpen(false);
    };

    if (!isAuthenticated) {
        return null;
    }

    return (
        <div className="notification-bell-container" ref={bellRef}>
            <button
                className="notification-bell-button"
                onClick={handleBellClick}
                aria-label="알림"
                title="알림"
            >
                <i className="bi bi-bell"></i>
                {unreadCount > 0 && (
                    <span className="notification-badge">
                        {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                )}
            </button>

            {isOpen && (
                <div ref={dropdownRef}>
                    <NotificationDropdown
                        notifications={notifications}
                        loading={loading}
                        unreadCount={unreadCount}
                        onNotificationClick={handleNotificationClick}
                        onMarkAllAsRead={handleMarkAllAsRead}
                        onViewAll={handleViewAll}
                        onClose={() => setIsOpen(false)}
                    />
                </div>
            )}
        </div>
    );
}
