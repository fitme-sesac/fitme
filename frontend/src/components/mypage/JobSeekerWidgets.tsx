import { Button } from "@/components/ui/button";
import { X, ChevronRight } from "lucide-react";

export function JobSeekerWidgets() {
    return (
        <div className="space-y-6">
            {/* Counts */}
            <div className="bg-white rounded-2xl border shadow-sm overflow-hidden">
                <div className="flex justify-between items-center p-5 border-b hover:bg-muted/30 cursor-pointer transition-colors group">
                    <span className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">스크랩 공고</span>
                    <div className="flex items-center gap-2">
                        <span className="font-bold text-lg text-[#5A639C]">0</span>
                        <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform" />
                    </div>
                </div>
                <div className="flex justify-between items-center p-5 hover:bg-muted/30 cursor-pointer transition-colors group">
                    <span className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">최근 본 공고</span>
                    <div className="flex items-center gap-2">
                        <span className="font-bold text-lg text-[#5A639C]">0</span>
                        <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:translate-x-1 transition-transform" />
                    </div>
                </div>
            </div>

            {/* Banner */}
            <div className="bg-gradient-to-br from-[#5A639C]/10 to-[#9B86BD]/20 rounded-2xl p-6 text-center cursor-pointer border border-[#5A639C]/20 hover:shadow-md transition-all group relative overflow-hidden">
                <div className="absolute top-2 right-2 p-1 opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/5 rounded-full">
                    <X className="h-4 w-4 text-muted-foreground" />
                </div>
                <h3 className="font-bold text-[#5A639C] text-xl mb-2 leading-tight">
                    합격을<br />부르는<br />강점 찾기
                </h3>
                <div className="mt-4 bg-gradient-to-r from-[#5A639C] to-[#9B86BD] text-white text-xs py-2 px-4 rounded-full inline-flex items-center shadow-lg group-hover:scale-105 transition-transform font-bold">
                    역량테스트 하기 <ChevronRight className="h-3 w-3 ml-1" />
                </div>

                {/* Decorative elements */}
                <div className="mt-6 flex justify-center gap-2 opacity-80">
                    <div className="w-8 h-10 bg-white/50 border border-white/60 rounded-lg shadow-sm transform -rotate-12 backdrop-blur-sm"></div>
                    <div className="w-8 h-10 bg-white/80 border border-white rounded-lg shadow-md transform rotate-0 -mt-2 backdrop-blur-sm z-10"></div>
                    <div className="w-8 h-10 bg-white/50 border border-white/60 rounded-lg shadow-sm transform rotate-12 backdrop-blur-sm"></div>
                </div>
            </div>

            {/* Top button */}
            <Button variant="outline" className="w-full text-xs font-bold rounded-xl h-10 hover:bg-[#5A639C] hover:text-white transition-colors border-dashed" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
                <span className="mr-1">^</span> TOP
            </Button>
        </div>
    );
}
