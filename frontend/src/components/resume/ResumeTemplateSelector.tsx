import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { FileText, Briefcase, GraduationCap, Palette } from "lucide-react";

interface ResumeTemplate {
    id: string;
    name: string;
    description: string;
    icon: React.ElementType;
    color: string;
    popular?: boolean;
}

const templates: ResumeTemplate[] = [
    {
        id: "basic",
        name: "기본형",
        description: "깔끔하고 정돈된 기본 이력서",
        icon: FileText,
        color: "bg-blue-500",
        popular: true,
    },
    {
        id: "professional",
        name: "전문가형",
        description: "경력직을 위한 상세한 양식",
        icon: Briefcase,
        color: "bg-primary",
    },
    {
        id: "academic",
        name: "학술형",
        description: "연구/학술 분야 맞춤 양식",
        icon: GraduationCap,
        color: "bg-green-500",
    },
    {
        id: "creative",
        name: "크리에이티브",
        description: "디자인/창작 분야 맞춤",
        icon: Palette,
        color: "bg-purple-500",
    },
];

interface ResumeTemplateSelectorProps {
    selectedTemplate: string;
    onSelectTemplate: (templateId: string) => void;
}

export function ResumeTemplateSelector({
    selectedTemplate,
    onSelectTemplate,
}: ResumeTemplateSelectorProps) {
    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {templates.map((template) => {
                const Icon = template.icon;
                const isSelected = selectedTemplate === template.id;

                return (
                    <Card
                        key={template.id}
                        className={cn(
                            "cursor-pointer transition-all hover:shadow-md",
                            isSelected && "ring-2 ring-primary border-primary"
                        )}
                        onClick={() => onSelectTemplate(template.id)}
                    >
                        <CardContent className="p-4 text-center">
                            <div
                                className={cn(
                                    "h-12 w-12 rounded-xl mx-auto mb-3 flex items-center justify-center",
                                    template.color
                                )}
                            >
                                <Icon className="h-6 w-6 text-white" />
                            </div>
                            <div className="flex items-center justify-center gap-2 mb-1">
                                <h3 className="font-medium">{template.name}</h3>
                                {template.popular && (
                                    <Badge variant="secondary" className="text-xs">
                                        인기
                                    </Badge>
                                )}
                            </div>
                            <p className="text-xs text-muted-foreground">
                                {template.description}
                            </p>
                        </CardContent>
                    </Card>
                );
            })}
        </div>
    );
}
