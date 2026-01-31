import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { GuestMain } from "@/components/home/GuestMain";
import { JobSeekerMain } from "@/components/home/JobSeekerMain";
import { EmployerMain } from "@/components/home/EmployerMain";
import { Navigate } from "react-router-dom";
import { Footer } from "@/components/layout/Footer";
import { useAuth } from "@/contexts/AuthContext";

const Index = () => {
  const { isCompany, isAdmin, user } = useAuth();

  // Debug: Log auth state to understand what's happening
  console.log("Index Page Auth State:", { isCompany, isServiceAdmin: isAdmin, user });

  if (isAdmin) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  // isCompany Redirect Removed: Now rendering EmployerMain

  return (
    <div className="min-h-screen bg-background">
      <Sidebar />
      <div className="lg:pl-64 transition-all duration-300">
        <Header />
        <main>
          {user ? (
            isCompany ? (
              // 기업 회원 홈
              <EmployerMain />
            ) : (
              // 구직자 회원 홈
              <JobSeekerMain />
            )
          ) : (
            // 비로그인 (게스트)
            <GuestMain />
          )}
        </main>

        <Footer />
      </div>
    </div>
  );
};

export default Index;
