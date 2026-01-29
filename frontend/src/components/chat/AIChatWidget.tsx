import { useState, useRef, useEffect } from "react";
import { X, Send, Bot, Sparkles, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface Message {
    id: string;
    role: "user" | "assistant";
    content: string;
    timestamp: Date;
}

interface AIChatWidgetProps {
    isOpen: boolean;
    onClose: () => void;
}

export function AIChatWidget({ isOpen, onClose }: AIChatWidgetProps) {
    const [messages, setMessages] = useState<Message[]>([
        {
            id: "welcome",
            role: "assistant",
            content: "안녕하세요! FitMe AI 채용 비서입니다. \n이력서 첨삭이나 면접 준비, 무엇이든 물어보세요!",
            timestamp: new Date(),
        },
    ]);
    const [input, setInput] = useState("");
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        if (isOpen) {
            scrollToBottom();
        }
    }, [messages, isOpen]);

    const handleSend = () => {
        if (!input.trim()) return;

        const userMessage: Message = {
            id: Date.now().toString(),
            role: "user",
            content: input,
            timestamp: new Date(),
        };

        setMessages((prev) => [...prev, userMessage]);
        setInput("");

        // Simulate AI response
        setTimeout(() => {
            const aiMessage: Message = {
                id: (Date.now() + 1).toString(),
                role: "assistant",
                content: "죄송합니다. 현재 AI 서버와 연결되지 않았습니다. (데모 모드)",
                timestamp: new Date(),
            };
            setMessages((prev) => [...prev, aiMessage]);
        }, 1000);
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-5 fade-in duration-300">
            <Card className="w-[360px] md:w-[400px] h-[600px] shadow-2xl border-border/50 flex flex-col overflow-hidden">
                {/* Header */}
                <div className="bg-gradient-to-r from-sky-500 to-teal-400 p-4 flex items-center justify-between shrink-0">
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
                            <p className="text-xs text-white/80">보통 1분 내 응답</p>
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
                                        <AvatarFallback className="bg-sky-100 text-sky-600">
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
                                    {msg.content}
                                </div>

                                <span className="text-[10px] text-muted-foreground self-end mb-1 px-1">
                                    {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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
                                input.trim() ? "bg-sky-500 hover:bg-sky-600 text-white" : "bg-transparent text-muted-foreground hover:bg-muted"
                            )}
                            onClick={handleSend}
                            disabled={!input.trim()}
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
