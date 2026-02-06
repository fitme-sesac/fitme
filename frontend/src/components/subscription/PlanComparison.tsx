import { Check, X } from "lucide-react";
const planComparison = [
    {
        feature: "인재 프로필 열람",
        free: "월 5명",
        pro: "월 50명",
        enterprise: "무제한",
    },
    {
        feature: "검색 필터",
        free: "기본",
        pro: "고급",
        enterprise: "전체",
    },
    {
        feature: "연락처 열람",
        free: false,
        pro: true,
        enterprise: true,
    },
    {
        feature: "인재 스크랩",
        free: false,
        pro: "100명",
        enterprise: "무제한",
    },
    {
        feature: "면접 제안",
        free: false,
        pro: true,
        enterprise: true,
    },
    {
        feature: "채용 분석 리포트",
        free: false,
        pro: false,
        enterprise: true,
    },
    {
        feature: "전담 매니저",
        free: false,
        pro: false,
        enterprise: true,
    },
    {
        feature: "API 연동",
        free: false,
        pro: false,
        enterprise: true,
    },
];
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";

export function PlanComparison() {
    const renderValue = (value: string | boolean) => {
        if (typeof value === "boolean") {
            return value ? (
                <Check className="w-5 h-5 text-accent mx-auto" />
            ) : (
                <X className="w-5 h-5 text-muted-foreground/50 mx-auto" />
            );
        }
        return <span className="text-foreground">{value}</span>;
    };

    return (
        <div className="bg-card rounded-xl border overflow-hidden">
            <Table>
                <TableHeader>
                    <TableRow className="bg-muted/30">
                        <TableHead className="w-[200px] font-semibold text-foreground">기능</TableHead>
                        <TableHead className="text-center font-semibold text-foreground">Free</TableHead>
                        <TableHead className="text-center font-semibold text-accent">Pro</TableHead>
                        <TableHead className="text-center font-semibold text-foreground">Enterprise</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {planComparison.map((row) => (
                        <TableRow key={row.feature} className="hover:bg-muted/20">
                            <TableCell className="font-medium text-muted-foreground">
                                {row.feature}
                            </TableCell>
                            <TableCell className="text-center">{renderValue(row.free)}</TableCell>
                            <TableCell className="text-center bg-accent/5">{renderValue(row.pro)}</TableCell>
                            <TableCell className="text-center">{renderValue(row.enterprise)}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}
