import { MapPin, Clock, Banknote, Sparkles, Building2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Link } from "react-router-dom";

interface JobCardProps {
  id: number;
  company: string;
  logo: string;
  title: string;
  location: string;
  salary: string;
  skills: string[];
  postedAt: string;
  isAd?: boolean;
  matchScore?: number;
}

export function JobCard({
  id,
  company,
  logo,
  title,
  location,
  salary,
  skills,
  postedAt,
  isAd = false,
  matchScore,
}: JobCardProps) {
  /* Warm Earthy Style */
  return (
    <div className={cn(
      "group flex flex-col justify-between h-[280px] w-full rounded-[1.25rem] bg-white border border-[#D9CFC7]/60 p-6 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-primary/20 hover:shadow-xl relative overflow-hidden",
      isAd && "ring-1 ring-[#D9CFC7]"
    )}>
      <Link to={`/jobs/${id}`} className="flex flex-col h-full justify-between">
        <div>
          {/* Header: Icon & Company */}
          <div className="flex items-center gap-3 mb-5">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#EFE9E3] text-xl font-bold text-foreground transition-colors group-hover:bg-primary group-hover:text-white border border-transparent">
              {logo.length > 5 ? <img src={logo} alt={company} className="w-full h-full object-cover rounded-xl" /> : logo}
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-[#8A8A8A]">{company}</span>
              {matchScore && (
                <span className="inline-flex items-center gap-1 text-xs font-bold text-[#4E56C0] bg-[#F5F3FF] px-2 py-0.5 rounded-md mt-0.5">
                  <Sparkles className="w-2.5 h-2.5" /> {matchScore}% 적합
                </span>
              )}
            </div>
          </div>

          {/* Title */}
          <h3 className="text-lg font-bold text-[#333] leading-snug line-clamp-3 group-hover:text-primary transition-colors">
            {title}
          </h3>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-[#EFE9E3] pt-4 mt-auto">
          <span className="inline-flex items-center rounded-full bg-[#F9F8F6] px-2.5 py-1 text-xs font-medium text-[#A0A0A0]">
            {location?.split(' ')[0] || "서울"}
          </span>
          <span className="text-sm font-medium text-[#C9B59C]">
            {postedAt}
          </span>
        </div>
      </Link>
    </div>
  );
}
