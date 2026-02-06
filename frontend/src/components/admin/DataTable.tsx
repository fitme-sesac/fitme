import React from "react";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { MoreHorizontal, ChevronLeft, ChevronRight } from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export interface Column<T> {
    key: keyof T | string;
    label: string;
    render?: (item: T) => React.ReactNode;
}

interface ActionItem<T> {
    label: string;
    onClick: (item: T) => void;
    disabled?: (item: T) => boolean;
}

interface DataTableProps<T> {
    columns: Column<T>[];
    data: T[];
    actions?: ActionItem<T>[];
    totalItems?: number;
    currentPage?: number;
    pageSize?: number;
    onPageChange?: (page: number) => void;
}

export function DataTable<T extends { id?: string | number }>({
    columns,
    data,
    actions,
    totalItems = 0,
    currentPage = 0,
    pageSize = 20,
    onPageChange,
}: DataTableProps<T>) {
    const totalPages = Math.ceil(totalItems / pageSize);

    return (
        <div className="bg-card rounded-xl border border-border overflow-hidden">
            <Table>
                <TableHeader>
                    <TableRow className="bg-muted/50 hover:bg-muted/50">
                        {columns.map((column) => (
                            <TableHead key={String(column.key)} className="font-semibold">
                                {column.label}
                            </TableHead>
                        ))}
                        {actions && actions.length > 0 && (
                            <TableHead className="w-12"></TableHead>
                        )}
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {data.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={columns.length + (actions ? 1 : 0)} className="text-center py-8 text-muted-foreground">
                                데이터가 없습니다.
                            </TableCell>
                        </TableRow>
                    ) : (
                        data.map((item, index) => (
                            <TableRow key={item.id ?? index} className="hover:bg-muted/30">
                                {columns.map((column) => (
                                    <TableCell key={String(column.key)}>
                                        {column.render
                                            ? column.render(item)
                                            : String((item as any)[column.key] ?? "")}
                                    </TableCell>
                                ))}
                                {actions && actions.length > 0 && (
                                    <TableCell>
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button variant="ghost" size="icon" className="h-8 w-8">
                                                    <MoreHorizontal className="w-4 h-4" />
                                                </Button>
                                            </DropdownMenuTrigger>
                                            <DropdownMenuContent align="end">
                                                {actions.map((action) => (
                                                    <DropdownMenuItem
                                                        key={action.label}
                                                        onClick={() => action.onClick(item)}
                                                        disabled={action.disabled ? action.disabled(item) : false}
                                                    >
                                                        {action.label}
                                                    </DropdownMenuItem>
                                                ))}
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </TableCell>
                                )}
                            </TableRow>
                        ))
                    )}
                </TableBody>
            </Table>

            {onPageChange && totalItems > pageSize && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-border">
                    <p className="text-sm text-muted-foreground">
                        총 {totalItems}개 항목
                    </p>
                    <div className="flex items-center gap-2">
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onPageChange(currentPage - 1)}
                            disabled={currentPage === 0}
                        >
                            <ChevronLeft className="w-4 h-4 mr-1" />
                            이전
                        </Button>
                        <span className="text-sm">
                            {currentPage + 1} / {totalPages}
                        </span>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onPageChange(currentPage + 1)}
                            disabled={currentPage + 1 >= totalPages}
                        >
                            다음
                            <ChevronRight className="w-4 h-4 ml-1" />
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}

export default DataTable;
