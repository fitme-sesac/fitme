import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Heart, Bookmark } from "lucide-react";

interface SurfitContentProps {
    id: string;
    title: string;
    description: string;
    author: string;
    date: string;
    imageUrl: string;
    category: string;
    logo: string;
}

export function SurfitContentCard({ content }: { content: SurfitContentProps }) {
    return (
        <Card className="overflow-hidden hover:shadow-lg transition-all duration-300 border-border group h-full flex flex-col">
            {/* Thumbnail Image */}
            <div className="relative aspect-video overflow-hidden bg-muted">
                <img
                    src={content.imageUrl}
                    alt={content.title}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
                <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                    <div className="flex gap-2">
                        <button className="bg-white/90 p-2 rounded-full hover:bg-white text-slate-400 hover:text-red-500 shadow-sm transition-colors">
                            <Heart className="w-4 h-4" />
                        </button>
                        <button className="bg-white/90 p-2 rounded-full hover:bg-white text-slate-400 hover:text-sky-500 shadow-sm transition-colors">
                            <Bookmark className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>

            <CardContent className="p-4 flex-1 flex flex-col">
                {/* Author Info */}
                <div className="flex items-center gap-2 mb-3">
                    <Avatar className="h-6 w-6">
                        <AvatarImage src={content.logo} />
                        <AvatarFallback className="text-[10px]">{content.author[0]}</AvatarFallback>
                    </Avatar>
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-300">
                        {content.author}
                    </span>
                    <span className="text-xs text-slate-400">|</span>
                    <span className="text-xs text-slate-400">{content.date}</span>
                </div>

                {/* Title */}
                <h3 className="font-bold text-lg leading-tight mb-2 group-hover:text-sky-600 transition-colors line-clamp-2">
                    {content.title}
                </h3>

                {/* Description */}
                <p className="text-sm text-slate-500 dark:text-slate-400 line-clamp-2 mb-4 flex-1">
                    {content.description}
                </p>

                {/* Footer: Category */}
                <div className="mt-auto pt-3 border-t border-border/50 flex items-center justify-between">
                    <Badge variant="secondary" className="text-xs font-normal bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400">
                        {content.category}
                    </Badge>
                </div>
            </CardContent>
        </Card>
    );
}
