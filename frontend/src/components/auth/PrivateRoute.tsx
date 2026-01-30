import { useAuth } from "@/contexts/AuthContext";
import { Navigate, useLocation } from "react-router-dom";
import { Loader2 } from "lucide-react";

interface PrivateRouteProps {
    children: React.ReactNode;
}

export default function PrivateRoute({ children }: PrivateRouteProps) {
    const { user, loading } = useAuth() as any;
    const location = useLocation();

    console.log("PrivateRoute Check:", {
        path: location.pathname,
        user: user?.email,
        loading
    });

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    if (!user) {
        console.log("PrivateRoute: No user, redirecting relative to", location.pathname);
        // Redirect to login page but save the attempted location
        return <Navigate to="/auth" state={{ from: location }} replace />;
    }

    return <>{children}</>;
}
