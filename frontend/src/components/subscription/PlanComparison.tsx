import { Check, X } from "lucide-react";
import { planComparison } from "@/data/subscriptionPlans";
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
