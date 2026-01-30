import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from "@/components/ui/accordion";

const faqs = [
    {
        question: "무료 플랜에서 Pro로 업그레이드하면 어떤 혜택이 있나요?",
        answer: "Pro 플랜으로 업그레이드하시면 월 50명의 인재 프로필을 열람할 수 있고, 인재 연락처 확인 및 면접 제안 기능을 사용하실 수 있습니다. 또한 고급 검색 필터로 더 정확한 인재를 찾으실 수 있습니다.",
    },
    {
        question: "결제 방법은 어떻게 되나요?",
        answer: "신용카드, 체크카드, 계좌이체 등 다양한 결제 수단을 지원합니다. Enterprise 플랜의 경우 세금계산서 발행이 가능합니다.",
    },
    {
        question: "언제든지 플랜을 변경할 수 있나요?",
        answer: "네, 언제든지 플랜을 업그레이드하거나 다운그레이드할 수 있습니다. 업그레이드 시 차액만 결제하시면 되고, 다운그레이드는 다음 결제일부터 적용됩니다.",
    },
    {
        question: "환불 정책은 어떻게 되나요?",
        answer: "결제 후 7일 이내에 서비스를 이용하지 않으셨다면 전액 환불이 가능합니다. 자세한 내용은 이용약관을 참고해주세요.",
    },
    {
        question: "Enterprise 플랜의 전담 매니저는 어떤 지원을 해주나요?",
        answer: "전담 매니저가 채용 전략 수립부터 인재 추천, 면접 일정 조율까지 채용 과정 전반을 지원합니다. 또한 정기적인 채용 분석 리포트를 제공하여 효율적인 채용을 도와드립니다.",
    },
];

export function FAQ() {
    return (
        <Accordion type="single" collapsible className="w-full">
            {faqs.map((faq, index) => (
                <AccordionItem key={index} value={`item-${index}`} className="border-border/50">
                    <AccordionTrigger className="text-left text-foreground hover:text-accent hover:no-underline">
                        {faq.question}
                    </AccordionTrigger>
                    <AccordionContent className="text-muted-foreground">
                        {faq.answer}
                    </AccordionContent>
                </AccordionItem>
            ))}
        </Accordion>
    );
}
