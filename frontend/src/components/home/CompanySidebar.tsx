import { Link } from "react-router-dom";
import { PlusCircle, FileText, Users, ChevronRight, Briefcase } from "lucide-react";
import { Button } from "@/components/ui/button";

export function CompanySidebar() {
    return (
        <div className="space-y-6">
            {/* Quick Action Card */}
            <div className="bg-card rounded-2xl border border-border p-5 shadow-sm">
                <div className="text-center space-y-4">
                    <div className="h-12 w-12 bg-primary/10 rounded-full flex items-center justify-center mx-auto text-primary">
                        <PlusCircle className="h-6 w-6" />
                    </div>
                    <div>
                        <h3 className="font-bold text-lg">새로운 공고 등록</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                            우수한 인재를 찾고 계신가요?<br />
                            지금 바로 채용 공고를 등록해보세요.
                        </p>
                    </div>
                    <Button className="w-full font-bold" size="lg" asChild>
                        <Link to="/company/dashboard?tab=jobs">
                            공고 등록하기
                        </Link>
                    </Button>
                </div>
            </div>

            {/* Management Links */}
            <div className="bg-card rounded-2xl border border-border overflow-hidden">
                <div className="p-4 border-b border-border bg-muted/30">
                    <h3 className="font-bold text-gray-900 flex items-center gap-2">
                        <Briefcase className="h-4 w-4 text-primary" />
                        채용 관리 바로가기
                    </h3>
                </div>
                <div className="divide-y divide-border">
                    <Link to="/company/dashboard" className="flex items-center justify-between p-4 hover:bg-muted/50 transition-colors group">
                        <div className="flex items-center gap-3">
                            <div className="h-8 w-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center group-hover:bg-blue-100 transition-colors">
                                <FileText className="h-4 w-4" />
                            </div>
                            <div>
                                <p className="font-medium text-sm text-gray-900">내 공고 관리</p>
                                <p className="text-xs text-gray-500">등록한 공고 수정/마감</p>
                            </div>
                        </div>
                        <ChevronRight className="h-4 w-4 text-gray-400 group-hover:text-primary transition-colors" />
                    </Link>

                    <Link to="/company/dashboard?tab=applicants" className="flex items-center justify-between p-4 hover:bg-muted/50 transition-colors group">
                        <div className="flex items-center gap-3">
                            <div className="h-8 w-8 rounded-lg bg-green-50 text-green-600 flex items-center justify-center group-hover:bg-green-100 transition-colors">
                                <Users className="h-4 w-4" />
                            </div>
                            <div>
                                <p className="font-medium text-sm text-gray-900">지원자 관리</p>
                                <p className="text-xs text-gray-500">서류 검토 및 면접 제안</p>
                            </div>
                        </div>
                        <ChevronRight className="h-4 w-4 text-gray-400 group-hover:text-primary transition-colors" />
                    </Link>
                </div>
            </div>

            {/* Talent Pool Promo */}
            <div className="bg-gradient-primary rounded-2xl p-5 text-white shadow-md relative overflow-hidden group">
                <div className="absolute top-0 right-0 p-3 opacity-10 group-hover:opacity-20 transition-opacity">
                    <Users className="h-24 w-24 transform rotate-12" />
                </div>
                <h3 className="font-bold text-lg mb-2 relative z-10">인재 풀 탐색</h3>
                <p className="text-indigo-100 text-sm mb-4 relative z-10">
                    공고를 기다리지 말고<br />
                    직접 인재에게 제안해보세요!
                </p>
                <Button variant="secondary" className="w-full text-indigo-700 font-bold hover:bg-white/90" size="sm" asChild>
                    <Link to="/talents">
                        인재 보러가기
                    </Link>
                </Button>
            </div>
        </div>
    );
}
