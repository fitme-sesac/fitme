// frontend/src/components/chat/AIChatWidget.tsx
import { useState, useRef, useEffect, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { X, Send, Bot, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { chatbotQuery } from "@/api/chatbot";

interface Message {
    id: string;
    role: "user" | "assistant";
    content: string;
    timestamp: Date;
}

interface AIChatWidgetProps {
    isOpen: boolean;
    onClose: () => void;
    /** 라우트 변경 등 외부 트리거로 채팅 세션을 초기화할 때 사용 */
    resetSeq: number;
}


const _LINK_RE = /(https?:\/\/[^\s<]+)|(\/jobs\/\d+)/g;

function linkifyLine(line: string, navigate?: (to: string) => void): ReactNode[] {
    const nodes: ReactNode[] = [];
    let last = 0;

    line.replace(_LINK_RE, (match, absUrl, relPath, offset) => {
        const idx = offset as number;
        if (idx > last) nodes.push(line.slice(last, idx));

        if (absUrl) {
            nodes.push(
                <a
                    key={`u-${idx}`}
                    href={absUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="underline break-all text-sky-600 hover:text-sky-700"
                >
                    {absUrl}
                </a>
            );
        } else if (relPath) {
            nodes.push(
                <a
                    key={`p-${idx}`}
                    href={relPath}
                    onClick={(e) => {
                        if (!navigate) return;
                        e.preventDefault();
                        navigate(relPath);
                    }}
                    className="underline break-all text-sky-600 hover:text-sky-700"
                >
                    {relPath}
                </a>
            );
        } else {
            nodes.push(match);
        }

        last = idx + match.length;
        return match;
    });

    if (last < line.length) nodes.push(line.slice(last));
    return nodes;
}

function renderMessageContent(content: string, navigate?: (to: string) => void): ReactNode {
    const lines = (content ?? "").split("\n");
    return (
        <>
            {lines.map((line, i) => (
                <span key={i}>
                    {linkifyLine(line, navigate)}
                    {i < lines.length - 1 ? <br /> : null}
                </span>
            ))}
        </>
    );
}

const WELCOME_MESSAGE: Message = {
    id: "welcome",
    role: "assistant",
    content:
        "안녕하세요! FitMe AI 채용 비서입니다. \n공고에 대한 정보들을 물어보세요!",
    timestamp: new Date(),
};

export function AIChatWidget({ isOpen, onClose, resetSeq }: AIChatWidgetProps) {
    const navigate = useNavigate();
    const [messages, setMessages] = useState<Message[]>([WELCOME_MESSAGE]);
    const [input, setInput] = useState("");

    // ✅ 대화 컨텍스트는 "현재 페이지(라우트)"에서만 유지 (localStorage 저장 금지)
    const [conversationId, setConversationId] = useState<string | null>(null);

    const [isSending, setIsSending] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        if (isOpen) scrollToBottom();
    }, [messages, isOpen]);

    // ✅ 라우트 변경(또는 외부 resetSeq 변화) 시: UI/컨텍스트 모두 초기화
    useEffect(() => {
        setMessages([
            {
                ...WELCOME_MESSAGE,
                timestamp: new Date(),
            },
        ]);
        setConversationId(null);
        setInput("");
        setIsSending(false);
    }, [resetSeq]);

    const handleSend = async () => {
        if (!input.trim() || isSending) return;

        const text = input.trim();

        const userMessage: Message = {
            id: Date.now().toString(),
            role: "user",
            content: text,
            timestamp: new Date(),
        };

        const pendingId = `${Date.now()}-assistant`;
        const pendingMessage: Message = {
            id: pendingId,
            role: "assistant",
            content: "답변 생성 중...",
            timestamp: new Date(),
        };

        setMessages((prev) => [...prev, userMessage, pendingMessage]);
        setInput("");
        setIsSending(true);

        try {
            const data = await chatbotQuery({
                message: text,
                conversation_id: conversationId, // ✅ 현재 페이지에서만 유지되는 ID
            });

            if (data?.conversation_id) {
                setConversationId(data.conversation_id);
            }

            setMessages((prev) =>
                prev.map((m) =>
                    m.id === pendingId
                        ? { ...m, content: data?.answer ?? "(빈 응답)", timestamp: new Date() }
                        : m
                )
            );
        } catch (e: any) {
            const msg = e?.response?.data?.detail || e?.message || "AI 서버 호출 실패";

            setMessages((prev) =>
                prev.map((m) =>
                    m.id === pendingId
                        ? { ...m, content: `오류: ${msg}`, timestamp: new Date() }
                        : m
                )
            );
        } finally {
            setIsSending(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            void handleSend();
        }
    };

    // ✅ 닫아도 언마운트하지 않고 숨김 처리 → 같은 페이지에서는 대화/메시지 유지
    return (
        <div
            className={cn(
                "fixed bottom-6 right-6 z-50",
                isOpen
                    ? "animate-in slide-in-from-bottom-5 fade-in duration-300"
                    : "hidden"
            )}
        >
            <Card className="w-[360px] md:w-[400px] h-[600px] shadow-2xl border-border/50 flex flex-col overflow-hidden">
                {/* Header */}
                <div
                    className="p-4 flex items-center justify-between shrink-0"
                    style={{ background: "linear-gradient(90deg, #5AB2FA 0%, #3DCEC9 100%)" }}
                >
                    <div className="flex items-center gap-3">
                        <div className="relative">
                            <Avatar className="h-10 w-10 border-2 border-white/20">
                                <AvatarImage src="/ai-avatar.png" />
                                <AvatarFallback className="bg-white/10 text-white">
                                    <Bot className="h-6 w-6" />
                                </AvatarFallback>
                            </Avatar>
                            <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-400 border-2 border-sky-500 rounded-full"></span>
                        </div>
                        <div className="text-white">
                            <h3 className="font-bold text-base flex items-center gap-1">
                                FitMe AI
                                <Sparkles className="h-3 w-3 text-yellow-300" />
                            </h3>
                            <p className="text-xs text-white/80">
                                {isSending ? "응답 생성 중..." : "보통 10초 내 응답"}
                            </p>
                        </div>
                    </div>
                    <Button
                        variant="ghost"
                        size="icon"
                        className="text-white hover:bg-white/20 rounded-full"
                        onClick={onClose}
                    >
                        <X className="h-5 w-5" />
                    </Button>
                </div>

                {/* Messages Area */}
                <ScrollArea className="flex-1 p-4 bg-muted/30">
                    <div className="space-y-4">
                        <div className="text-center text-xs text-muted-foreground my-4">
                            오늘 {new Date().toLocaleDateString()}
                        </div>

                        {messages.map((msg) => (
                            <div
                                key={msg.id}
                                className={cn(
                                    "flex items-start gap-2 max-w-[85%]",
                                    msg.role === "user" ? "ml-auto flex-row-reverse" : ""
                                )}
                            >
                                {msg.role === "assistant" && (
                                    <Avatar className="h-8 w-8 mt-1 border">
                                        <AvatarFallback className="bg-sky-50 text-sky-500">
                                            <Bot className="h-4 w-4" />
                                        </AvatarFallback>
                                    </Avatar>
                                )}

                                <div
                                    className={cn(
                                        "p-3 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap shadow-sm",
                                        msg.role === "user"
                                            ? "bg-sky-500 text-white rounded-tr-none"
                                            : "bg-background border rounded-tl-none"
                                    )}
                                >
                                    {renderMessageContent(msg.content, navigate)}
                                </div>

                                <span className="text-[10px] text-muted-foreground self-end mb-1 px-1">
                                    {msg.timestamp.toLocaleTimeString([], {
                                        hour: "2-digit",
                                        minute: "2-digit",
                                    })}
                                </span>
                            </div>
                        ))}
                        <div ref={messagesEndRef} />
                    </div>
                </ScrollArea>

                {/* Input Area */}
                <div className="p-4 bg-background border-t shrink-0">
                    <div className="relative flex items-end gap-2">
                        <Input
                            placeholder="메시지를 입력하세요..."
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="pr-12 resize-none py-3 h-12 rounded-xl bg-muted/30 focus:bg-background transition-colors"
                        />
                        <Button
                            size="icon"
                            className={cn(
                                "absolute right-1 bottom-1 h-10 w-10 rounded-lg transition-all",
                                input.trim() && !isSending
                                    ? "bg-sky-500 hover:bg-sky-600 text-white"
                                    : "bg-transparent text-muted-foreground hover:bg-muted"
                            )}
                            onClick={() => void handleSend()}
                            disabled={!input.trim() || isSending}
                        >
                            <Send className="h-5 w-5" />
                        </Button>
                    </div>

                    <div className="text-center mt-2">
                        <span className="text-[10px] text-muted-foreground">
                            FitMe AI는 실수할 수 있습니다. 중요한 정보는 확인해 주세요.
                        </span>
                    </div>
                </div>
            </Card>
        </div>
    );
}
