import { useState, useEffect, useRef } from "react";
import { Search, Bell, Menu, Target, LogIn, LogOut, User, Coins, ArrowRight, Loader2 } from "lucide-react";
import { getMyLedgers } from "@/api/wallet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/AuthContext";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { useNotificationContext } from "@/contexts/NotificationContext";

import { CreditChargeModal } from "@/components/payment/CreditChargeModal";

// Mock data deleted

export function Header() {
  const { user, signOut, isCompany, isAdmin, credits, userRole } = useAuth() as any;
  const navigate = useNavigate();
  const [recentLedgers, setRecentLedgers] = useState<any[]>([]);
  const [loadingLedgers, setLoadingLedgers] = useState(false);
  const notifCtx = useNotificationContext();
  const {
    notifications,
    unreadCount,
    loading: notifLoading,
    fetchRecentNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
  } = notifCtx ?? {
    notifications: [],
    unreadCount: 0,
    loading: false,
    fetchRecentNotifications: async () => {},
    fetchUnreadCount: async () => {},
    markAsRead: async () => {},
    markAllAsRead: async () => {},
  };

  const [searchParams] = useSearchParams();
  const [notifOpen, setNotifOpen] = useState(false);
  const [creditOpen, setCreditOpen] = useState(false);
  const [chargeModalOpen, setChargeModalOpen] = useState(false);
  const notifOpenOnceRef = useRef(false);
  // 알림 폴링은 NotificationProvider에서 한 번만 수행 (Header/면접 페이지 동일 unreadCount)

  // 크레딧 내역 가져오기 (드롭다운 열릴 때)
  useEffect(() => {
    if (user && creditOpen) {
      const fetchHistory = async () => {
        setLoadingLedgers(true);
        try {
          const res = await getMyLedgers(userRole);
          setRecentLedgers(res.content?.slice(0, 5) || []);
        } catch {
          setRecentLedgers([]);
        } finally {
          setLoadingLedgers(false);
        }
      };
      fetchHistory();
    }
  }, [user, creditOpen, userRole]);

  // URL에 결제 성공/실패/확인 파라미터가 있으면 모달을 자동으로 엽니다.
  useEffect(() => {
    const shouldOpen =
        searchParams.get("payment_success") === "true" ||
        searchParams.get("payment_fail") === "true" ||
        searchParams.get("payment_confirm") === "true";

    if (shouldOpen) {
      setChargeModalOpen(true);
    }
  }, [searchParams]);

  // 알림 드롭다운 열릴 때 최신 알림 목록 + 읽지 않은 수 동시 갱신 (헤더/면접 페이지 동일 상태)
  useEffect(() => {
    if (!notifOpen || !user) {
      notifOpenOnceRef.current = false;
      return;
    }
    if (notifOpenOnceRef.current) return;
    notifOpenOnceRef.current = true;
    fetchRecentNotifications(10);
    fetchUnreadCount();
  }, [notifOpen, user, fetchRecentNotifications, fetchUnreadCount]);

  const handleNotificationClick = (notification: any) => {
    markAsRead(notification.id);
    setNotifOpen(false);
    // 알림 링크로 이동 (백엔드 DTO: linkUrl)
    const url = notification.linkUrl ?? notification.link;
    if (url) {
      if (url.startsWith("http")) {
        window.location.href = url;
      } else {
        navigate(url);
      }
    }
  };

  // 기업 회원은 마이페이지 클릭 시 대시보드로 이동
  const mypageHref = isCompany ? "/company/dashboard" : "/mypage";

  return (
    <header className="sticky top-0 z-50 h-16 border-b border-border bg-sidebar/95 backdrop-blur supports-[backdrop-filter]:bg-sidebar/80">
      <div className="h-full max-w-7xl mx-auto flex items-center justify-between px-4 lg:px-6">
        {/* 모바일 메뉴 & 로고 */}
        <div className="flex items-center gap-3 lg:hidden">
          <Button variant="ghost" size="icon" className="lg:hidden">
            <Menu className="h-5 w-5" />
          </Button>
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <Target className="h-4 w-4 text-primary-foreground" />
            </div>
            <span className="text-lg font-bold">FitMe</span>
          </Link>
        </div>

        {/* 검색바 */}
        <div className="hidden md:flex flex-1 max-w-xl mx-auto lg:ml-0">
          <div className="relative w-full home-search-bar">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground home-search-icon" />
            <Input
              type="search"
              placeholder="포지션, 기업, 기술스택으로 검색하세요"
              className="w-full pl-10 pr-4 h-10 bg-[#EAEFEF] border-0 focus-visible:ring-primary rounded-full transition-all"
            />
          </div>
        </div>

        {/* 우측 액션 */}
        <div className="flex items-center gap-2">
          {/* 크레딧 표시 (로그인 시) */}
          {user && (
            <div className="hidden sm:flex items-center gap-2 mr-2">
              <DropdownMenu open={creditOpen} onOpenChange={setCreditOpen}>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" className="h-auto p-0 hover:bg-transparent">
                    <Badge variant="secondary" className="gap-1 px-3 py-1.5 cursor-pointer hover:bg-slate-200 transition-colors">
                      <Coins className="h-3.5 w-3.5 text-amber-500" />
                      <span className="font-medium">{credits.toLocaleString()}</span>
                    </Badge>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-72" align="end" forceMount>
                  <div className="p-3 border-b flex items-center justify-between">
                    <span className="font-bold text-sm">최근 크레딧 내역</span>
                    <Badge variant="outline" className="text-[10px] font-normal border-slate-200">사용 가능</Badge>
                  </div>
                  <div className="max-h-[280px] overflow-y-auto">
                    {loadingLedgers ? (
                      <div className="p-8 flex flex-col items-center gap-2 text-muted-foreground">
                        <Loader2 className="h-5 w-5 animate-spin" />
                        <span className="text-[10px]">불러오는 중...</span>
                      </div>
                    ) : recentLedgers.length === 0 ? (
                      <div className="p-8 text-center text-[10px] text-muted-foreground">
                        내역이 없습니다.
                      </div>
                    ) : (
                      recentLedgers.map((log) => (
                        <div key={log.ledgerId} className="p-3 border-b last:border-0 flex items-center justify-between hover:bg-slate-50 transition-colors">
                          <div className="space-y-0.5 max-w-[180px]">
                            <p className="text-xs font-bold text-slate-700 truncate">{log.memo}</p>
                            <p className="text-[10px] text-slate-400">{new Date(log.occurredAt).toLocaleDateString()}</p>
                          </div>
                          <div className={`text-xs font-extrabold ${log.type === 'CREDIT' ? 'text-emerald-500' : 'text-rose-500'}`}>
                            {log.type === 'CREDIT' ? '+' : '-'}{log.amount.toLocaleString()}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                  <div className="p-2 border-t">
                    <Button
                      variant="ghost"
                      className="w-full h-9 text-xs font-bold text-slate-500 hover:text-primary transition-colors flex items-center justify-center gap-1"
                      onClick={() => {
                        setCreditOpen(false);
                        if (isCompany) {
                          navigate("/companies?tab=billing");
                        } else {
                          navigate("/mypage?tab=history");
                        }
                      }}
                    >
                      전체 내역 확인하기 <ArrowRight className="h-3 w-3" />
                    </Button>
                  </div>
                </DropdownMenuContent>
              </DropdownMenu>
              <Button size="sm" variant="outline" onClick={() => setChargeModalOpen(true)} className="h-8 border-slate-200 hover:bg-slate-50 font-bold">
                충전
              </Button>
            </div>
          )}

          {/* 크레딧 충전 모달 */}
          <CreditChargeModal open={chargeModalOpen} onOpenChange={setChargeModalOpen} />

          {/* 알림 드롭다운 */}
          <DropdownMenu open={notifOpen} onOpenChange={setNotifOpen}>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="relative">
                <Bell className="h-5 w-5" />
                {unreadCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-accent text-[10px] font-bold text-accent-foreground">
                    {unreadCount > 9 ? "9+" : unreadCount}
                  </span>
                )}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-80" align="end" forceMount>
              {/* 알림 헤더 */}
              <div className="flex items-center justify-between p-3 border-b">
                <span className="font-semibold flex items-center gap-2">
                  알림
                  {unreadCount > 0 && (
                    <Badge variant="destructive" className="text-xs px-1.5 py-0">
                      {unreadCount}
                    </Badge>
                  )}
                </span>
                {unreadCount > 0 && (
                  <button
                    className="text-xs text-primary hover:underline"
                    onClick={markAllAsRead}
                  >
                    모두 읽음
                  </button>
                )}
              </div>

              {/* 알림 목록 */}
              <div className="max-h-[300px] overflow-y-auto">
                {notifLoading ? (
                  <div className="p-6 text-center text-muted-foreground">
                    <div className="animate-spin h-5 w-5 border-2 border-primary border-t-transparent rounded-full mx-auto mb-2" />
                    로딩중...
                  </div>
                ) : notifications.length === 0 ? (
                  <div className="p-6 text-center text-muted-foreground">
                    <Bell className="h-8 w-8 mx-auto mb-2 opacity-50" />
                    <p>알림이 없습니다</p>
                  </div>
                ) : (
                  notifications.slice(0, 5).map((notification: any) => (
                    <div
                      key={notification.id}
                      className={`p-3 border-b last:border-0 hover:bg-muted/50 cursor-pointer transition-colors ${!notification.isRead ? "bg-primary/5" : ""
                        }`}
                      onClick={() => handleNotificationClick(notification)}
                    >
                      <div className="flex items-start gap-3">
                        {!notification.isRead && (
                          <div className="w-2 h-2 rounded-full bg-primary mt-2 flex-shrink-0" />
                        )}
                        <div className={!notification.isRead ? "" : "ml-5"}>
                          <p className="text-sm font-medium line-clamp-1">
                            {notification.title}
                          </p>
                          <p className="text-xs text-muted-foreground line-clamp-2 mt-0.5">
                            {notification.message}
                          </p>
                          <p className="text-xs text-muted-foreground mt-1">
                            {notification.createdAt ? new Date(notification.createdAt).toLocaleDateString() : ""}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* 알림 푸터 */}
              <div className="border-t p-2">
                <Button
                  variant="ghost"
                  className="w-full text-sm"
                  onClick={() => {
                    setNotifOpen(false);
                    navigate(isCompany ? "/company/dashboard" : "/interview?tab=notifications");
                  }}
                >
                  모든 알림 보기
                </Button>
              </div>
            </DropdownMenuContent>
          </DropdownMenu>

          <Button variant="ghost" size="icon" className="md:hidden">
            <Search className="h-5 w-5" />
          </Button>

          {user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative h-10 w-10 rounded-full">
                  <Avatar className="h-9 w-9">
                    <AvatarImage src={user.user_metadata?.avatar_url} alt={user.email || (user as any).userid || ""} />
                    <AvatarFallback className="bg-primary text-primary-foreground">
                      {(user.email || (user as any).userid || (user as any).name || "U").charAt(0).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-56" align="end" forceMount>
                <div className="flex items-center justify-start gap-2 p-2">
                  <div className="flex flex-col space-y-1 leading-none">
                    <div className="flex items-center gap-2">
                      <p className="font-medium truncate max-w-[120px]">
                        {user.user_metadata?.name ||
                          user.user_metadata?.display_name ||
                          user.user_metadata?.full_name ||
                          user.user_metadata?.handle ||
                          user.username ||
                          user.email?.split('@')[0] ||
                          "사용자"}
                      </p>
                      <Badge variant="secondary" className="text-[10px] px-1.5 py-0 h-5 font-normal shrink-0">
                        {isAdmin ? "관리자" : isCompany ? "기업" : "구직자"}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground truncate max-w-[180px]">
                      {user.email || (user as any).userid || (user as any).name || "이메일 없음"}
                    </p>
                  </div>
                </div>
                {/* 모바일에서 크레딧 표시 */}
                <div className="sm:hidden px-2 pb-2">
                  <div className="flex items-center justify-between p-2 bg-secondary rounded-lg">
                    <div className="flex items-center gap-1">
                      <Coins className="h-4 w-4 text-amber-500" />
                      <span className="text-sm font-medium">{credits.toLocaleString()}</span>
                    </div>
                    <button
                      onClick={() => setChargeModalOpen(true)}
                      className="text-xs text-primary font-bold hover:underline"
                    >
                      충전
                    </button>
                  </div>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link to={mypageHref} className="cursor-pointer">
                    <User className="mr-2 h-4 w-4" />
                    마이페이지
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={signOut} className="cursor-pointer text-destructive">
                  <LogOut className="mr-2 h-4 w-4" />
                  로그아웃
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <Button asChild className="hidden sm:flex btn-gradient-primary gap-2">
              <Link to="/auth">
                <LogIn className="h-4 w-4" />
                로그인
              </Link>
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
