import { useAuth } from "@/contexts/AuthContext";
import { Navigate, useLocation } from "react-router-dom";
import { Loader2 } from "lucide-react";

interface PrivateRouteProps {
    children: React.ReactNode;
	requiredRole?: string;
}

export default function PrivateRoute({ children, requiredRole }: PrivateRouteProps) {
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
        // Redirect to login page but save the attempted location
        return <Navigate to="/auth" state={{ from: location }} replace />;
    }

    if (requiredRole && user?.role && user.role !== requiredRole) {
        return <Navigate to="/" replace />;
    }

    return <>{children}</>;
}
