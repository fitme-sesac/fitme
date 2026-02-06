import { cn } from "@/lib/utils";

type StatusType = "active" | "pending" | "rejected" | "resolved" | "new" | "deleted" | "suspended" | "open" | "closed";

interface StatusBadgeProps {
    status: StatusType | string;
    label?: string;
}

const statusStyles: Record<string, string> = {
    active: "bg-green-500/10 text-green-600",
    open: "bg-green-500/10 text-green-600",
    pending: "bg-yellow-500/10 text-yellow-600",
    rejected: "bg-red-500/10 text-red-600",
    deleted: "bg-red-500/10 text-red-600",
    suspended: "bg-red-500/10 text-red-600",
    closed: "bg-gray-500/10 text-gray-600",
    resolved: "bg-gray-500/10 text-gray-600",
    new: "bg-primary/10 text-primary",
    // 구독 상태 추가
    canceled: "bg-gray-500/10 text-gray-600",
    payment_failed: "bg-red-500/10 text-red-600",
};

const statusLabels: Record<string, string> = {
    active: "활성",
    open: "게시중",
    pending: "대기중",
    rejected: "거절됨",
    deleted: "삭제됨",
    suspended: "정지",
    closed: "마감",
    resolved: "해결됨",
    new: "신규",
    // 구독 상태 라벨 추가
    canceled: "취소됨",
    payment_failed: "결제 실패",
};

export const StatusBadge = ({ status, label }: StatusBadgeProps) => {
    const normalizedStatus = status.toLowerCase();
    const style = statusStyles[normalizedStatus] || "bg-muted text-muted-foreground";
    const displayLabel = label || statusLabels[normalizedStatus] || status;

    return (
        <span className={cn(
            "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium",
            style
        )}>
            {displayLabel}
        </span>
    );
};

export default StatusBadge;
