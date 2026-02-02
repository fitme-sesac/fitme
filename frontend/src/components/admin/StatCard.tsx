import { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface StatCardProps {
    title: string;
    value: string | number;
    change?: string;
    changeType?: "positive" | "negative" | "neutral";
    icon: LucideIcon;
    iconColor?: string;
}

export const StatCard = ({
    title,
    value,
    change,
    changeType = "neutral",
    icon: Icon,
    iconColor = "bg-primary/10 text-primary",
}: StatCardProps) => {
    const changeColors = {
        positive: "text-green-600",
        negative: "text-red-600",
        neutral: "text-muted-foreground",
    };

    return (
        <Card>
            <CardContent className="p-6">
                <div className="flex items-start justify-between">
                    <div className="space-y-1">
                        <p className="text-sm font-medium text-muted-foreground">{title}</p>
                        <p className="text-3xl font-bold text-foreground">{value}</p>
                    </div>
                    <div className={cn("p-3 rounded-xl", iconColor)}>
                        <Icon className="w-6 h-6" />
                    </div>
                </div>
                {change && (
                    <p className={cn("mt-3 text-sm", changeColors[changeType])}>
                        {change}
                    </p>
                )}
            </CardContent>
        </Card>
    );
};

export default StatCard;
