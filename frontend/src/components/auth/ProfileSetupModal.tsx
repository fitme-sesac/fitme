import { useState, useEffect, useRef } from "react";
import { useAuth } from "@/contexts/AuthContext";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Camera, Check } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";

// 유효성 검사 스키마
const profileSchema = z.object({
    name: z.string().min(2, "이름은 2글자 이상이어야 합니다."),
    handle: z
        .string()
        .min(3, "핸들은 3글자 이상이어야 합니다.")
        .max(30, "핸들은 30글자 이하여야 합니다.")
        .regex(/^[a-z0-9-]+$/, "영문 소문자, 숫자, 하이픈(-)만 사용할 수 있습니다."),
    agreeHandle: z.boolean().refine((val) => val === true, {
        message: "필수 동의 항목입니다.",
    }),
    agreePersonal: z.boolean().refine((val) => val === true, {
        message: "필수 동의 항목입니다.",
    }),
});

export function ProfileSetupModal() {
    const { user, updateProfile, checkSession } = useAuth();
    const [open, setOpen] = useState(false);
    const [previewImage, setPreviewImage] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // 폼 설정
    const {
        register,
        handleSubmit,
        setValue,
        watch,
        formState: { errors, isValid, isSubmitting },
    } = useForm({
        resolver: zodResolver(profileSchema),
        defaultValues: {
            name: "",
            handle: "",
            agreeHandle: false,
            agreePersonal: false,
        },
        mode: "onChange",
    });

    // 초기 상태 로드 및 모달 트리거 조건 체크
    useEffect(() => {
        // 이미 로컬에서 완료 처리했다면 띄우지 않음
        if (localStorage.getItem("profile_setup_completed") === "true") {
            setOpen(false);
            return;
        }

        if (user) {
            // user_metadata에 handle이 없으면 모달을 띄움
            // 주의: user_metadata 구조는 백엔드에 따라 다를 수 있음.
            // 여기서는 handle이 없거나 null일 때 띄우는 로직으로 구현
            const handle = user.user_metadata?.handle;
            const displayName = user.user_metadata?.display_name;

            if (!handle) {
                setOpen(true);
                if (displayName) {
                    setValue("name", displayName);
                }
                if (user.user_metadata?.avatar_url) {
                    setPreviewImage(user.user_metadata.avatar_url);
                }
            } else {
                setOpen(false);
            }
        }
    }, [user, setValue]);

    const handleImageClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = () => {
                setPreviewImage(reader.result as string);
            };
            reader.readAsDataURL(file);
        }
    };

    const onSubmit = async (data: z.infer<typeof profileSchema>) => {
        try {
            const formData = new FormData();
            formData.append("name", data.name);
            formData.append("handle", data.handle);

            const file = fileInputRef.current?.files?.[0];
            if (file) {
                formData.append("profileImage", file);
            }

            // API 호출
            // @ts-ignore
            await updateProfile(formData);

            // 세션 갱신 (핸들 정보 가져오기 위함)
            await checkSession();

            toast.success("프로필 설정이 완료되었습니다.");
            setOpen(false);
        } catch (error: any) {
            console.error("Profile update error:", error);

            // 백엔드 API가 없거나 에러 발생 시 (500, 404 등)
            // UI 테스트를 위해 로컬 스토리지에 저장하고 넘어가는 Fallback 로직
            if (error.response?.status === 500 || error.response?.status === 404) {
                toast.error("서버 연결 실패 (API 미구현). 로컬에 임시 저장하고 진행합니다.");

                // 임시로 완료 처리
                localStorage.setItem("profile_setup_completed", "true");
                setOpen(false);
                return;
            }

            toast.error(error.message || "프로필 업데이트에 실패했습니다.");
        }
    };

    const userName = watch("name") || user?.email?.split("@")[0] || "사용자";

    return (
        <Dialog open={open} onOpenChange={(val) => !val && setOpen(val)}> {/* 강제로 닫기 방지 로직 필요하면 수정 가능 */}
            {/* onPointerDownOutside 등으로 닫기 방지하려면 DialogContent에 props 추가 */}
            <DialogContent
                className="sm:max-w-md bg-zinc-950 text-white border-zinc-800"
                onPointerDownOutside={(e) => e.preventDefault()}
                onEscapeKeyDown={(e) => e.preventDefault()}
                // 닫기 버튼(X)을 숨기려면 CSS나 shadcn DialogPrimitive 설정을 건드려야 함.
                // 여기서는 className으로 제어 시도 (DialogClose가 내부적으로 렌더링되므로 [&>button]:hidden)
                // shadcn/ui 기본 close 버튼 숨기기:
                title="프로필 설정"
            >
                <style>{`
          [data-state='open'] button[class*='absolute right-4'] {
            display: none;
          }
        `}</style>

                <DialogHeader className="flex flex-col items-center justify-center space-y-4 pt-4">
                    <div className="relative group cursor-pointer" onClick={handleImageClick}>
                        <Avatar className="w-24 h-24 rounded-2xl">
                            <AvatarImage src={previewImage || ""} className="object-cover" />
                            <AvatarFallback className="bg-amber-700 text-4xl font-bold rounded-2xl">
                                {userName.charAt(0)}
                            </AvatarFallback>
                        </Avatar>
                        <div className="absolute -bottom-2 -right-2 bg-white rounded-full p-1.5 border-4 border-zinc-950">
                            <Camera className="w-4 h-4 text-black" />
                        </div>
                        <input
                            type="file"
                            ref={fileInputRef}
                            className="hidden"
                            accept="image/*"
                            onChange={handleFileChange}
                        />
                    </div>
                    <div className="text-center space-y-1">
                        <DialogTitle className="text-xl font-bold">
                            안녕하세요 {userName}님!
                        </DialogTitle>
                        <DialogDescription className="text-zinc-400">
                            다른 사용자에게 보여질 프로필을 설정해주세요.
                        </DialogDescription>
                    </div>
                </DialogHeader>

                <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 pt-4">
                    <div className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="name" className="text-sm font-medium text-zinc-300">
                                프로필 이름 <span className="text-blue-500">*</span>
                            </Label>
                            <Input
                                id="name"
                                {...register("name")}
                                className="bg-zinc-900 border-zinc-800 focus:ring-blue-500 h-12"
                                placeholder="이름을 입력하세요"
                            />
                            {errors.name && (
                                <p className="text-red-500 text-xs">{errors.name.message as string}</p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="handle" className="text-sm font-medium text-zinc-300">
                                프로필 핸들(아이디) <span className="text-blue-500">*</span>
                            </Label>
                            <Input
                                id="handle"
                                {...register("handle")}
                                className="bg-zinc-900 border-zinc-800 focus:ring-blue-500 h-12"
                                placeholder="영문 소문자, 숫자, 하이픈(-)"
                            />
                            <div className="text-[11px] text-zinc-500 space-y-1 px-1">
                                <p>* 핸들은 프로필 링크로 사용되며 변경할 수 없습니다.</p>
                                <p>* 영문 소문자(a-z)와 숫자(0-9), 하이픈(-) 사용 가능, 최대 30자</p>
                            </div>
                            {errors.handle && (
                                <p className="text-red-500 text-xs">{errors.handle.message as string}</p>
                            )}
                        </div>
                    </div>

                    <div className="space-y-3 pt-2">
                        <div className={`p-4 rounded-xl border transition-colors flex items-start gap-3 cursor-pointer ${watch("agreeHandle") ? "bg-zinc-900 border-zinc-700" : "bg-zinc-900/50 border-zinc-800"
                            }`}
                            onClick={() => setValue("agreeHandle", !watch("agreeHandle"), { shouldValidate: true })}
                        >
                            <div className={`mt-0.5 w-5 h-5 rounded-full border flex items-center justify-center transition-colors ${watch("agreeHandle") ? "bg-white border-white" : "border-zinc-600"
                                }`}>
                                {watch("agreeHandle") && <Check className="w-3 h-3 text-black stroke-[3]" />}
                            </div>
                            <div className="text-sm text-zinc-400 leading-tight">
                                [필수] 프로필 핸들(아이디)는 영구적이며 다시 변경할 수 없음에 동의합니다.
                                <span className="underline ml-1">자세히 알아보기</span>
                            </div>
                        </div>
                        {errors.agreeHandle && <p className="text-red-500 text-xs px-1">필수 동의 항목입니다.</p>}

                        <div className={`p-4 rounded-xl border transition-colors flex items-start gap-3 cursor-pointer ${watch("agreePersonal") ? "bg-zinc-900 border-zinc-700" : "bg-zinc-900/50 border-zinc-800"
                            }`}
                            onClick={() => setValue("agreePersonal", !watch("agreePersonal"), { shouldValidate: true })}
                        >
                            <div className={`mt-0.5 w-5 h-5 rounded-full border flex items-center justify-center transition-colors ${watch("agreePersonal") ? "bg-white border-white" : "border-zinc-600"
                                }`}>
                                {watch("agreePersonal") && <Check className="w-3 h-3 text-black stroke-[3]" />}
                            </div>
                            <div className="text-sm text-zinc-400 leading-tight">
                                [필수] 개인 계정을 기업 또는 브랜드 명의로 사용하는 것을 권장하지 않습니다...
                                <span className="underline ml-1">자세히 알아보기</span>
                            </div>
                        </div>
                        {errors.agreePersonal && <p className="text-red-500 text-xs px-1">필수 동의 항목입니다.</p>}
                    </div>

                    <Button
                        type="submit"
                        className="w-full h-12 text-base font-semibold bg-white text-black hover:bg-zinc-200 mt-2"
                        disabled={!isValid || isSubmitting}
                    >
                        {isSubmitting ? "저장 중..." : "프로필 설정 완료"}
                    </Button>
                </form>
            </DialogContent>
        </Dialog>
    );
}
