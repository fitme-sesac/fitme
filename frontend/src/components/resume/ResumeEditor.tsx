import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
    User,
    Briefcase,
    GraduationCap,
    Award,
    Plus,
    X,
    Save,
    Loader2,
    Sparkles
} from "lucide-react";

export interface ResumeData {
    personalInfo: {
        name: string;
        email: string;
        phone: string;
        address: string;
        birthDate: string;
    };
    summary: string;
    experiences: {
        id: string;
        company: string;
        position: string;
        startDate: string;
        endDate: string;
        description: string;
    }[];
    education: {
        id: string;
        school: string;
        major: string;
        degree: string;
        startDate: string;
        endDate: string;
    }[];
    skills: string[];
    aiSummary?: string;
}

interface ResumeEditorProps {
    resumeData: ResumeData;
    onUpdateResume: (data: ResumeData) => void;
    onSave: () => void;
    saving?: boolean;
}

export function ResumeEditor({ resumeData, onUpdateResume, onSave, saving }: ResumeEditorProps) {
    const [newSkill, setNewSkill] = useState("");
    const updatePersonalInfo = (field: string, value: string) => {
        onUpdateResume({
            ...resumeData,
            personalInfo: { ...resumeData.personalInfo, [field]: value },
        });
    };

    const addExperience = () => {
        const newExp = {
            id: Date.now().toString(),
            company: "",
            position: "",
            startDate: "",
            endDate: "",
            description: "",
        };
        onUpdateResume({
            ...resumeData,
            experiences: [...resumeData.experiences, newExp],
        });
    };

    const updateExperience = (id: string, field: string, value: string) => {
        onUpdateResume({
            ...resumeData,
            experiences: resumeData.experiences.map((exp) =>
                exp.id === id ? { ...exp, [field]: value } : exp
            ),
        });
    };

    const removeExperience = (id: string) => {
        onUpdateResume({
            ...resumeData,
            experiences: resumeData.experiences.filter((exp) => exp.id !== id),
        });
    };

    const addEducation = () => {
        const newEdu = {
            id: Date.now().toString(),
            school: "",
            major: "",
            degree: "",
            startDate: "",
            endDate: "",
        };
        onUpdateResume({
            ...resumeData,
            education: [...resumeData.education, newEdu],
        });
    };

    const updateEducation = (id: string, field: string, value: string) => {
        onUpdateResume({
            ...resumeData,
            education: resumeData.education.map((edu) =>
                edu.id === id ? { ...edu, [field]: value } : edu
            ),
        });
    };

    const removeEducation = (id: string) => {
        onUpdateResume({
            ...resumeData,
            education: resumeData.education.filter((edu) => edu.id !== id),
        });
    };

    const addSkill = () => {
        if (newSkill.trim() && !resumeData.skills.includes(newSkill.trim())) {
            onUpdateResume({
                ...resumeData,
                skills: [...resumeData.skills, newSkill.trim()],
            });
            setNewSkill("");
        }
    };

    const removeSkill = (skill: string) => {
        onUpdateResume({
            ...resumeData,
            skills: resumeData.skills.filter((s) => s !== skill),
        });
    };

    return (
        <div className="space-y-6">
            {/* 기본 정보 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <User className="h-5 w-5 text-primary" />
                        기본 정보
                    </CardTitle>
                </CardHeader>
                <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                        <Label htmlFor="name">이름</Label>
                        <Input
                            id="name"
                            value={resumeData.personalInfo.name}
                            onChange={(e) => updatePersonalInfo("name", e.target.value)}
                            placeholder="홍길동"
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="email">이메일</Label>
                        <Input
                            id="email"
                            type="email"
                            value={resumeData.personalInfo.email}
                            onChange={(e) => updatePersonalInfo("email", e.target.value)}
                            placeholder="example@email.com"
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="phone">연락처</Label>
                        <Input
                            id="phone"
                            value={resumeData.personalInfo.phone}
                            onChange={(e) => updatePersonalInfo("phone", e.target.value)}
                            placeholder="010-1234-5678"
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="birthDate">생년월일</Label>
                        <Input
                            id="birthDate"
                            type="date"
                            value={resumeData.personalInfo.birthDate}
                            onChange={(e) => updatePersonalInfo("birthDate", e.target.value)}
                        />
                    </div>
                    <div className="space-y-2 md:col-span-2">
                        <Label htmlFor="address">주소</Label>
                        <Input
                            id="address"
                            value={resumeData.personalInfo.address}
                            onChange={(e) => updatePersonalInfo("address", e.target.value)}
                            placeholder="서울특별시 강남구"
                        />
                    </div>
                </CardContent>
            </Card>

            {/* 자기소개 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <Award className="h-5 w-5 text-primary" />
                        자기소개
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    {resumeData.aiSummary && resumeData.aiSummary !== resumeData.summary && (
                        <div className="bg-sky-50 border border-sky-100 rounded-lg p-4 mb-4">
                            <div className="flex justify-between items-start mb-2">
                                <h4 className="font-semibold text-sky-800 flex items-center gap-2">
                                    <Sparkles className="h-4 w-4" />
                                    AI 요약 제안
                                </h4>
                                <Button
                                    size="sm"
                                    variant="outline"
                                    className="bg-white hover:bg-sky-100 text-sky-600 border-sky-200"
                                    onClick={() => onUpdateResume({ ...resumeData, summary: resumeData.aiSummary! })}
                                >
                                    적용하기
                                </Button>
                            </div>
                            <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
                                {resumeData.aiSummary}
                            </p>
                        </div>
                    )}
                    <Textarea
                        value={resumeData.summary}
                        onChange={(e) =>
                            onUpdateResume({ ...resumeData, summary: e.target.value })
                        }
                        placeholder="간단한 자기소개를 작성해주세요..."
                        className="min-h-[150px]"
                    />
                </CardContent>
            </Card>

            {/* 경력 */}
            <Card>
                <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <Briefcase className="h-5 w-5 text-primary" />
                        경력
                    </CardTitle>
                    <Button size="sm" variant="outline" onClick={addExperience}>
                        <Plus className="h-4 w-4 mr-1" />
                        추가
                    </Button>
                </CardHeader>
                <CardContent className="space-y-6">
                    {resumeData.experiences.length === 0 ? (
                        <p className="text-muted-foreground text-center py-4">
                            경력 정보를 추가해주세요.
                        </p>
                    ) : (
                        resumeData.experiences.map((exp, index) => (
                            <div key={exp.id} className="relative border rounded-lg p-4">
                                <Button
                                    size="icon"
                                    variant="ghost"
                                    className="absolute top-2 right-2 h-6 w-6"
                                    onClick={() => removeExperience(exp.id)}
                                >
                                    <X className="h-4 w-4" />
                                </Button>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label>회사명</Label>
                                        <Input
                                            value={exp.company}
                                            onChange={(e) =>
                                                updateExperience(exp.id, "company", e.target.value)
                                            }
                                            placeholder="회사명"
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label>직책</Label>
                                        <Input
                                            value={exp.position}
                                            onChange={(e) =>
                                                updateExperience(exp.id, "position", e.target.value)
                                            }
                                            placeholder="직책/직무"
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label>시작일</Label>
                                        <Input
                                            type="month"
                                            value={exp.startDate}
                                            onChange={(e) =>
                                                updateExperience(exp.id, "startDate", e.target.value)
                                            }
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label>종료일</Label>
                                        <Input
                                            type="month"
                                            value={exp.endDate}
                                            onChange={(e) =>
                                                updateExperience(exp.id, "endDate", e.target.value)
                                            }
                                            placeholder="재직중"
                                        />
                                    </div>
                                    <div className="space-y-2 md:col-span-2">
                                        <Label>업무 내용</Label>
                                        <Textarea
                                            value={exp.description}
                                            onChange={(e) =>
                                                updateExperience(exp.id, "description", e.target.value)
                                            }
                                            placeholder="주요 업무 내용을 작성해주세요..."
                                        />
                                    </div>
                                </div>
                            </div>
                        ))
                    )}
                </CardContent>
            </Card>

            {/* 학력 */}
            <Card>
                <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <GraduationCap className="h-5 w-5 text-primary" />
                        학력
                    </CardTitle>
                    <Button size="sm" variant="outline" onClick={addEducation}>
                        <Plus className="h-4 w-4 mr-1" />
                        추가
                    </Button>
                </CardHeader>
                <CardContent className="space-y-6">
                    {resumeData.education.length === 0 ? (
                        <p className="text-muted-foreground text-center py-4">
                            학력 정보를 추가해주세요.
                        </p>
                    ) : (
                        resumeData.education.map((edu) => (
                            <div key={edu.id} className="relative border rounded-lg p-4">
                                <Button
                                    size="icon"
                                    variant="ghost"
                                    className="absolute top-2 right-2 h-6 w-6"
                                    onClick={() => removeEducation(edu.id)}
                                >
                                    <X className="h-4 w-4" />
                                </Button>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label>학교명</Label>
                                        <Input
                                            value={edu.school}
                                            onChange={(e) =>
                                                updateEducation(edu.id, "school", e.target.value)
                                            }
                                            placeholder="학교명"
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label>전공</Label>
                                        <Input
                                            value={edu.major}
                                            onChange={(e) =>
                                                updateEducation(edu.id, "major", e.target.value)
                                            }
                                            placeholder="전공"
                                        />
                                    </div>
                                    <div className="space-y-2">
                                        <Label>학위</Label>
                                        <Input
                                            value={edu.degree}
                                            onChange={(e) =>
                                                updateEducation(edu.id, "degree", e.target.value)
                                            }
                                            placeholder="학사/석사/박사"
                                        />
                                    </div>
                                    <div className="space-y-2 flex gap-2">
                                        <div className="flex-1 space-y-2">
                                            <Label>시작일</Label>
                                            <Input
                                                type="month"
                                                value={edu.startDate}
                                                onChange={(e) =>
                                                    updateEducation(edu.id, "startDate", e.target.value)
                                                }
                                            />
                                        </div>
                                        <div className="flex-1 space-y-2">
                                            <Label>졸업일</Label>
                                            <Input
                                                type="month"
                                                value={edu.endDate}
                                                onChange={(e) =>
                                                    updateEducation(edu.id, "endDate", e.target.value)
                                                }
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))
                    )}
                </CardContent>
            </Card>

            {/* 기술 스택 */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <Award className="h-5 w-5 text-primary" />
                        기술 스택
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex flex-wrap gap-2 mb-4">
                        {resumeData.skills.map((skill) => (
                            <Badge
                                key={skill}
                                variant="secondary"
                                className="cursor-pointer hover:bg-destructive hover:text-destructive-foreground"
                                onClick={() => removeSkill(skill)}
                            >
                                {skill}
                                <X className="h-3 w-3 ml-1" />
                            </Badge>
                        ))}
                    </div>
                    <div className="flex gap-2">
                        <Input
                            value={newSkill}
                            onChange={(e) => setNewSkill(e.target.value)}
                            placeholder="기술 스택 추가 (Enter)"
                            onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addSkill())}
                        />
                        <Button onClick={addSkill} variant="outline">
                            <Plus className="h-4 w-4" />
                        </Button>
                    </div>
                </CardContent>
            </Card>

            {/* 저장 버튼 */}
            <div className="flex justify-end">
                <Button onClick={onSave} size="lg" className="gap-2" disabled={saving}>
                    {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                    {saving ? "저장 중..." : "이력서 저장"}
                </Button>
            </div>
        </div>
    );
}
