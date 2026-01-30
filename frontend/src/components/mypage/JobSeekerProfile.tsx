import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/contexts/AuthContext";
import { Camera, ChevronDown, Laptop, X, Briefcase, MapPin, Building2 } from "lucide-react";
import { Link } from "react-router-dom";

interface ProfileData {
    name: string;
    lastCompany: string;
    totalExperience: string;
    recentInfo: string;
    recentPosition: string;
    skills: string[];
}

export function JobSeekerProfile({ stats, recommendations = [], profileData }: { stats: any, recommendations?: any[], profileData?: ProfileData }) {
    const { user, profile } = useAuth();

    // 기본 더미 데이터 (만약 prop이 없을 경우 대비)
    const data = profileData || {
        name: "양**",
        lastCompany: "(주)에스씨케이컴퍼니",
        totalExperience: "1년 8개월",
        recentInfo: "1년 2개월 근무",
        recentPosition: "바리스타",
        skills: ["풀스택", "JAVA", "CSS", "React"]
    };

    return (
        <div className="space-y-6">
            {/* 프로필 카드 */}
            <Card className="rounded-2xl shadow-sm border overflow-hidden">
                <CardContent className="p-0">
                    {/* 상단 프로필 영역 */}
                    <div className="p-8 relative">
                        {/* 말풍선 */}
                        <div className="absolute top-6 left-36 bg-white border border-[#5A639C]/30 text-[#5A639C] px-3 py-1.5 rounded-xl text-sm shadow-sm animate-pulse z-10 hidden md:flex items-center gap-2">
                            <span>나를 위해 AI에게 이력서를 첨삭 맡겨보자!</span>
                            <button className="text-gray-400 hover:text-gray-600"><X className="h-3 w-3" /></button>
                        </div>

                        <div className="flex flex-col md:flex-row gap-6 mt-6">
                            <div className="relative mx-auto md:mx-0">
                                <Avatar className="h-24 w-24 border-4 border-white shadow-lg cursor-pointer group">
                                    <AvatarImage src={user?.user_metadata?.avatar_url} />
                                    <AvatarFallback className="text-2xl bg-[#5A639C]/10 text-[#6d77bd] font-bold">
                                        {data.name}
                                    </AvatarFallback>
                                    <div className="absolute inset-0 bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                        <Camera className="h-6 w-6 text-white" />
                                    </div>
                                </Avatar>
                                <div className="absolute bottom-0 right-0 bg-white rounded-full p-1 border shadow-sm">
                                    <div className="bg-gray-100 rounded-full h-6 w-6 flex items-center justify-center">
                                        <Camera className="h-3 w-3 text-gray-500" />
                                    </div>
                                </div>
                            </div>

                            <div className="flex-1 pt-2 text-center md:text-left">
                                <div className="flex items-center justify-center md:justify-start gap-2 mb-2">
                                    <Badge variant="outline" className="text-xs font-normal text-muted-foreground bg-gray-50 border-gray-200">
                                        업데이트 3달전
                                    </Badge>
                                    <span className="text-xs text-[#5A639C] font-bold cursor-pointer hover:underline">포지션 제안받는 중</span>
                                </div>

                                <h1 className="text-2xl font-bold flex items-center justify-center md:justify-start gap-2 mb-4 text-foreground">
                                    {data.lastCompany}에서 일했어요
                                    <Laptop className="h-6 w-6 text-[#9B86BD]" />
                                </h1>

                                <div className="bg-[#5A639C]/5 rounded-xl p-5 border border-[#5A639C]/10 text-left">
                                    <div className="flex items-center gap-2 mb-2">
                                        <span className="font-bold text-foreground">총 경력 {data.totalExperience}</span>
                                        <ChevronDown className="h-4 w-4 text-muted-foreground" />
                                    </div>
                                    <div className="space-y-1 text-sm text-muted-foreground">
                                        <div className="flex items-center gap-2">
                                            <span className="font-medium text-foreground">{data.lastCompany}</span>
                                            <span className="w-px h-3 bg-gray-300 mx-1"></span>
                                            <span>{data.recentInfo}</span>
                                        </div>
                                        <div>{data.recentPosition}</div>
                                    </div>
                                </div>

                                <div className="mt-4 text-xs text-muted-foreground underline cursor-pointer hover:text-[#5A639C]">
                                    이력서 90% 완성 &gt;
                                </div>
                            </div>
                        </div>

                        <div className="mt-8 flex flex-col gap-4">
                            <div className="flex flex-col md:flex-row items-start md:items-center gap-2 md:gap-4 text-sm">
                                <span className="font-bold min-w-[60px]">보유스킬</span>
                                <div className="flex flex-wrap gap-2">
                                    {data.skills.map(skill => (
                                        <Badge key={skill} variant="secondary" className="hover:bg-gradient-to-r hover:from-[#5A639C] hover:to-[#9B86BD] hover:text-white cursor-pointer bg-white border border-gray-100 text-gray-600 font-normal shadow-sm transition-all md:text-xs">
                                            {skill}
                                        </Badge>
                                    ))}
                                    <ChevronDown className="h-4 w-4 text-gray-400 cursor-pointer" />
                                </div>
                            </div>
                            <div className="flex flex-col md:flex-row items-start md:items-center gap-2 md:gap-4 text-sm">
                                <span className="font-bold min-w-[60px]">구직 선호도</span>
                                <div className="flex items-center gap-2 text-[#5A639C] cursor-pointer hover:underline font-medium">
                                    <span>조직문화 vs 근무환경 무엇이 중요한가요? &gt;</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* 하단 통계 탭 */}
                    <div className="grid grid-cols-3 divide-x border-t bg-gray-50/30">
                        {[
                            { label: "기업 제안", value: stats.proposals, highlight: false },
                            { label: "관심 공고", value: stats.saved, highlight: false },
                            { label: "커뮤니티", value: stats.community, highlight: false }
                        ].map((item, idx) => (
                            <Button
                                key={idx}
                                variant="ghost"
                                className="flex flex-col items-center justify-center py-8 h-auto hover:bg-[#5A639C]/5 rounded-none transition-colors group"
                            >
                                <div className="text-sm text-muted-foreground mb-1 group-hover:text-[#5A639C] transition-colors">{item.label}</div>
                                <div className={`text-xl font-bold ${item.highlight ? 'text-[#5A639C]' : 'text-foreground'}`}>{item.value}</div>
                            </Button>
                        ))}
                    </div>
                </CardContent>
            </Card>

            {/* 하단 추천 공고 배너 (원픽 공고) */}
            <div className="mt-8">
                <h3 className="text-lg font-bold flex items-center gap-2 mb-4">
                    <span className="text-[#5A639C] text-xl">●</span> AI가 추천하는 원픽 공고
                    <span className="text-xs font-normal text-muted-foreground ml-auto cursor-pointer hover:underline">더보기 &gt;</span>
                </h3>

                {recommendations.length > 0 ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {recommendations.map((job) => (
                            <Link to={`/jobs/${job.id}`} key={job.id} className="block group">
                                <div className="aspect-[4/3] bg-white rounded-2xl border shadow-sm p-5 flex flex-col justify-between group-hover:border-[#5A639C]/50 group-hover:shadow-md transition-all cursor-pointer h-full relative overflow-hidden">
                                    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-[#5A639C] to-[#9B86BD] opacity-0 group-hover:opacity-100 transition-opacity"></div>
                                    <div className="flex justify-between items-start">
                                        <div className="w-12 h-12 bg-gray-50 rounded-xl border flex items-center justify-center overflow-hidden">
                                            <Building2 className="h-6 w-6 text-gray-300" />
                                        </div>
                                        <Briefcase className="h-4 w-4 text-muted-foreground" />
                                    </div>
                                    <div className="space-y-2">
                                        <h4 className="font-bold text-foreground line-clamp-2 leading-tight group-hover:text-[#5A639C] transition-colors">{job.title}</h4>
                                        <p className="text-sm text-muted-foreground line-clamp-1">{job.companyName}</p>

                                        <div className="flex items-center gap-2 text-xs text-muted-foreground pt-2">
                                            <MapPin className="h-3 w-3" />
                                            <span>{job.location}</span>
                                        </div>
                                    </div>
                                </div>
                            </Link>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-12 bg-gray-50 rounded-2xl border border-dashed">
                        <p className="text-muted-foreground">추천 공고를 불러오는 중입니다...</p>
                    </div>
                )}
            </div>
        </div>
    );
}
