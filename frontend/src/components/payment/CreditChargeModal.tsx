import { useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Coins, CreditCard, Smartphone, Banknote, ShieldCheck, CheckCircle2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { cn } from "@/lib/utils";

interface CreditOption {
    id: string;
    credits: number;
    price: number;
    bonus?: number;
}

const CREDIT_OPTIONS: CreditOption[] = [
    { id: "1", credits: 1000, price: 11000 },
    { id: "2", credits: 5000, price: 55000, bonus: 250 },
    { id: "3", credits: 10000, price: 110000, bonus: 1000 },
    { id: "4", credits: 30000, price: 330000, bonus: 4500 },
    { id: "5", credits: 50000, price: 550000, bonus: 10000 },
    { id: "6", credits: 100000, price: 990000, bonus: 25000 },
];

export function CreditChargeModal({
    open,
    onOpenChange
}: {
    open: boolean;
    onOpenChange: (open: boolean) => void
}) {
    const { user } = useAuth();
    const [selectedId, setSelectedId] = useState<string>("3");
    const [paymentMethod, setPaymentMethod] = useState<string>("easy-pay");
    const [agreed, setAgreed] = useState(false);
    const [isCharging, setIsCharging] = useState(false);

    const selectedOption = CREDIT_OPTIONS.find(opt => opt.id === selectedId) || CREDIT_OPTIONS[2];
    const totalCredits = selectedOption.credits + (selectedOption.bonus || 0);

    const handleCharge = () => {
        if (!agreed) return;
        setIsCharging(true);
        // Simulate API call
        setTimeout(() => {
            setIsCharging(false);
            onOpenChange(false);
            // In real app, you'd show a success toast or update balance via context
        }, 2000);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-w-4xl p-0 overflow-hidden border-none bg-white rounded-xl shadow-2xl">
                <div className="flex flex-col md:flex-row h-full">
                    {/* Left Panel: Summary (Riot Style) */}
                    <div className="w-full md:w-[320px] bg-slate-50 p-8 border-r border-slate-100 flex flex-col">
                        <div className="mb-8">
                            <div className="flex flex-col items-center text-center">
                                <div className="h-20 w-20 rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-200 mb-4 transform -rotate-3">
                                    <Coins className="h-10 w-10 text-white" />
                                </div>
                                <h2 className="text-2xl font-black text-slate-800 tracking-tight">크레딧 충전하기</h2>
                                <div className="h-1 w-12 bg-blue-600 rounded-full mt-2" />
                            </div>
                        </div>

                        <div className="space-y-6 flex-1">
                            <div className="space-y-2">
                                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">결제 알림 계정</p>
                                <p className="text-sm font-bold text-slate-700 truncate">{user?.email}</p>
                            </div>

                            <div className="space-y-4 pt-4 border-t border-slate-200">
                                <div className="flex justify-between items-end">
                                    <p className="text-sm font-bold text-slate-500">충전 예정</p>
                                    <div className="text-right">
                                        <p className="text-2xl font-black text-blue-600 leading-none">+{totalCredits.toLocaleString()}</p>
                                        <p className="text-[10px] font-bold text-blue-400 mt-1 uppercase">Credits</p>
                                    </div>
                                </div>
                                {selectedOption.bonus && (
                                    <div className="flex justify-between text-xs font-bold">
                                        <span className="text-emerald-500">보너스 합산됨</span>
                                        <span className="text-emerald-500">+{selectedOption.bonus.toLocaleString()}</span>
                                    </div>
                                )}
                            </div>

                            <div className="mt-auto pt-8">
                                <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-sm space-y-3">
                                    <div className="flex items-center gap-2 text-[10px] font-bold text-slate-400">
                                        <ShieldCheck className="h-3 w-3" />
                                        보안 결제 시스템 작동 중
                                    </div>
                                    <div className="flex justify-between items-center text-sm">
                                        <span className="font-bold text-slate-500">결제 금액</span>
                                        <span className="text-xl font-black text-slate-900">{selectedOption.price.toLocaleString()}원</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="mt-8 text-center">
                            <p className="text-[10px] text-slate-400 font-medium">리그 오브 레전드 RP 충전 방식의<br />고급스러운 크레딧 시스템입니다.</p>
                        </div>
                    </div>

                    {/* Right Panel: Selection Area */}
                    <div className="flex-1 p-8 overflow-y-auto max-h-[85vh]">
                        <div className="space-y-8">
                            {/* Step 1: Amount Selection */}
                            <section className="space-y-4">
                                <div className="flex items-center justify-between">
                                    <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                        <span className="h-6 w-6 rounded-full bg-slate-900 text-white text-xs flex items-center justify-center font-black">1</span>
                                        충전 금액 선택
                                    </h3>
                                    <Badge variant="secondary" className="bg-blue-50 text-blue-600 font-bold">인기 상품</Badge>
                                </div>

                                <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
                                    {CREDIT_OPTIONS.map((option) => (
                                        <div
                                            key={option.id}
                                            onClick={() => setSelectedId(option.id)}
                                            className={cn(
                                                "relative cursor-pointer group transition-all duration-200 rounded-xl border-2 p-4",
                                                selectedId === option.id
                                                    ? "border-blue-600 bg-blue-50/30 ring-4 ring-blue-50"
                                                    : "border-slate-100 bg-white hover:border-slate-200 hover:shadow-md"
                                            )}
                                        >
                                            {selectedId === option.id && (
                                                <CheckCircle2 className="absolute top-2 right-2 h-4 w-4 text-blue-600" />
                                            )}
                                            <div className="space-y-1">
                                                <p className="text-[10px] font-bold text-slate-400 uppercase">Credits</p>
                                                <p className="text-xl font-black text-slate-800">{option.credits.toLocaleString()}</p>
                                                <p className="text-sm font-bold text-slate-500 group-hover:text-slate-700">{option.price.toLocaleString()}원</p>
                                            </div>
                                            {option.bonus && (
                                                <Badge className="absolute -bottom-2 -right-2 bg-emerald-500 text-[10px] font-black border-2 border-white">
                                                    +{option.bonus.toLocaleString()} 보너스
                                                </Badge>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </section>

                            {/* Step 2: Payment Method */}
                            <section className="space-y-4">
                                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                                    <span className="h-6 w-6 rounded-full bg-slate-900 text-white text-xs flex items-center justify-center font-black">2</span>
                                    결제 수단 선택
                                </h3>

                                <Tabs value={paymentMethod} onValueChange={setPaymentMethod} className="w-full">
                                    <TabsList className="grid grid-cols-5 w-full bg-slate-100 h-11 p-1 rounded-lg">
                                        <TabsTrigger value="easy-pay" className="text-xs font-bold data-[state=active]:bg-white data-[state=active]:text-blue-600 rounded-md">간편결제</TabsTrigger>
                                        <TabsTrigger value="card" className="text-xs font-bold data-[state=active]:bg-white data-[state=active]:text-blue-600 rounded-md">신용카드</TabsTrigger>
                                        <TabsTrigger value="phone" className="text-xs font-bold data-[state=active]:bg-white data-[state=active]:text-blue-600 rounded-md">휴대폰</TabsTrigger>
                                        <TabsTrigger value="bank" className="text-xs font-bold data-[state=active]:bg-white data-[state=active]:text-blue-600 rounded-md">계좌이체</TabsTrigger>
                                        <TabsTrigger value="voucher" className="text-xs font-bold data-[state=active]:bg-white data-[state=active]:text-blue-600 rounded-md">상품권</TabsTrigger>
                                    </TabsList>

                                    <div className="mt-6 p-6 border-2 border-slate-100 rounded-xl bg-slate-50/50">
                                        <TabsContent value="easy-pay" className="mt-0 space-y-4">
                                            <p className="text-xs font-bold text-slate-500">결제하기 버튼을 눌러 선택하신 서비스의 결제 창에서 진행하세요.</p>
                                            <RadioGroup defaultValue="kakaopay" className="grid grid-cols-3 gap-4">
                                                <div className="flex items-center space-x-2 bg-white p-3 rounded-lg border border-slate-200">
                                                    <RadioGroupItem value="kakaopay" id="kakaopay" />
                                                    <Label htmlFor="kakaopay" className="text-xs font-bold cursor-pointer">kakaopay</Label>
                                                </div>
                                                <div className="flex items-center space-x-2 bg-white p-3 rounded-lg border border-slate-200">
                                                    <RadioGroupItem value="tosspay" id="tosspay" />
                                                    <Label htmlFor="tosspay" className="text-xs font-bold cursor-pointer">Toss</Label>
                                                </div>
                                                <div className="flex items-center space-x-2 bg-white p-3 rounded-lg border border-slate-200">
                                                    <RadioGroupItem value="payco" id="payco" />
                                                    <Label htmlFor="payco" className="text-xs font-bold cursor-pointer">PAYCO</Label>
                                                </div>
                                            </RadioGroup>
                                        </TabsContent>

                                        <TabsContent value="card" className="mt-0">
                                            <div className="flex flex-col items-center justify-center py-4 space-y-2 opacity-60">
                                                <CreditCard className="h-8 w-8 text-slate-400" />
                                                <p className="text-xs font-bold">모든 신용카드 결제가 가능합니다.</p>
                                            </div>
                                        </TabsContent>

                                        <TabsContent value="phone" className="mt-0">
                                            <div className="flex flex-col items-center justify-center py-4 space-y-2 opacity-60">
                                                <Smartphone className="h-8 w-8 text-slate-400" />
                                                <p className="text-xs font-bold">통신사 소액결제가 가능합니다.</p>
                                            </div>
                                        </TabsContent>

                                        <TabsContent value="bank" className="mt-0">
                                            <div className="flex flex-col items-center justify-center py-4 space-y-2 opacity-60">
                                                <Banknote className="h-8 w-8 text-slate-400" />
                                                <p className="text-xs font-bold">실시간 계좌이체 서비스를 제공합니다.</p>
                                            </div>
                                        </TabsContent>

                                        <TabsContent value="voucher" className="mt-0">
                                            <div className="flex flex-col items-center justify-center py-4 space-y-2 opacity-60">
                                                <div className="h-8 w-12 bg-slate-200 rounded border-2 border-slate-300 flex items-center justify-center font-black text-[8px] text-slate-400">VOUCHER</div>
                                                <p className="text-xs font-bold">문화상품권을 사용할 수 있습니다.</p>
                                            </div>
                                        </TabsContent>
                                    </div>
                                </Tabs>
                            </section>

                            {/* Step 3: Agreements */}
                            <section className="p-4 bg-slate-100 rounded-xl space-y-4">
                                <div className="flex items-start space-x-3">
                                    <Checkbox
                                        id="agreement"
                                        checked={agreed}
                                        onCheckedChange={(checked) => setAgreed(checked === true)}
                                        className="mt-1 data-[state=checked]:bg-blue-600"
                                    />
                                    <div className="space-y-1">
                                        <Label htmlFor="agreement" className="text-xs font-bold text-slate-700 leading-none cursor-pointer">
                                            상품, 가격 및 유효기간을 확인하였으며, 계약 관련 고지 사항과 크레딧 정책 및 결제 진행에 동의합니다.
                                        </Label>
                                        <p className="text-[10px] text-slate-400 font-medium leading-relaxed">충전된 크레딧은 채용 서비스 유료 기능 이용에 사용되며, 환불 규정은 이용약관을 따릅니다.</p>
                                    </div>
                                </div>
                            </section>

                            <div className="pt-4">
                                <Button
                                    onClick={handleCharge}
                                    disabled={!agreed || isCharging}
                                    className={cn(
                                        "w-full h-14 text-lg font-black tracking-tight rounded-xl transition-all duration-300",
                                        agreed
                                            ? "bg-blue-600 hover:bg-blue-700 text-white shadow-xl shadow-blue-200 transform translate-y-0 active:translate-y-1"
                                            : "bg-slate-200 text-slate-400"
                                    )}
                                >
                                    {isCharging ? (
                                        <div className="flex items-center gap-2">
                                            <div className="h-5 w-5 border-4 border-white border-t-transparent rounded-full animate-spin" />
                                            결제 처리 중...
                                        </div>
                                    ) : (
                                        "결제하기"
                                    )}
                                </Button>
                            </div>
                        </div>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
