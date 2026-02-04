import { createContext, useContext, ReactNode } from "react";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { useAuth } from "@/contexts/AuthContext";
import { useEffect } from "react";

const NotificationContext = createContext<ReturnType<typeof useNotifications> | null>(null);

export function NotificationProvider({ children }: { children: ReactNode }) {
    const { user } = useAuth();
    const value = useNotifications();
    const { startPolling, stopPolling } = value;

    useEffect(() => {
        if (user) {
            startPolling(30000);
        }
        return () => stopPolling();
    }, [user, startPolling, stopPolling]);

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotificationContext() {
    const ctx = useContext(NotificationContext);
    return ctx;
}
