import { Sidebar } from "@/components/layout/Sidebar";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export default function Talents() {
    return (
        <div className="min-h-screen bg-background">
            <Sidebar />
            <div className="lg:pl-64 transition-all duration-300">
                <Header />
                <main className="container max-w-5xl mx-auto py-12 px-4 md:px-8 min-h-[calc(100vh-200px)]">
                    <h1 className="text-3xl font-bold mb-4">인재풀</h1>
                    <p className="text-muted-foreground">기업 회원을 위한 인재 검색 페이지입니다.</p>
                </main>
                <Footer />
            </div>
        </div>
    );
}
