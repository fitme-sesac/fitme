import { useAuth } from "@/contexts/AuthContext";
import { Navigate, useLocation } from "react-router-dom";
import { Loader2 } from "lucide-react";

interface PrivateRouteProps {
    children: React.ReactNode;
    /** 단일 역할: 이 역할만 허용 */
    requiredRole?: string;
    /** 여러 역할: 이 중 하나면 허용 (requiredRole보다 우선) */
    allowedRoles?: string[];
}

export default function PrivateRoute({ children, requiredRole, allowedRoles }: PrivateRouteProps) {
    const { user, loading } = useAuth() as any;
    const location = useLocation();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    if (!user) {
        return <Navigate to="/auth" state={{ from: location }} replace />;
    }

    if (allowedRoles?.length) {
        if (!user?.role || !allowedRoles.includes(user.role)) {
            return <Navigate to="/" replace />;
        }
    } else if (requiredRole && user?.role && user.role !== requiredRole) {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
}
