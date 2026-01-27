import { useState, useEffect } from "react";
import { Search, Bell, Menu, Sparkles, LogIn, LogOut, User, Coins } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/AuthContext";
import { Link, useNavigate } from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { useNotifications } from "@/features/notification/hooks/useNotifications";

export function Header() {
  const { user, signOut, isCompany } = useAuth();
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    loading: notifLoading,
    fetchRecentNotifications,
    markAsRead,
    markAllAsRead,
    startPolling,
    stopPolling
  } = useNotifications();

  const [notifOpen, setNotifOpen] = useState(false);
  const [creditBalance, setCreditBalance] = useState(0);

  // 크레딧 잔액 조회 (API 연동 시 활성화)
  useEffect(() => {
    if (user) {
      // TODO: 실제 API 연동
      // fetchCreditBalance().then(setCreditBalance);
      setCreditBalance(10000); // 임시 값
    }
  }, [user]);

  // 알림 폴링 시작/종료
  useEffect(() => {
    if (user) {
      startPolling(30000);
    }
    return () => stopPolling();
  }, [user, startPolling, stopPolling]);

  // 알림 드롭다운 열릴 때 최신 알림 로드
  useEffect(() => {
    if (notifOpen && user) {
      fetchRecentNotifications(10);
    }
  }, [notifOpen, user, fetchRecentNotifications]);

  const handleNotificationClick = (notification: any) => {
    markAsRead(notification.id);
    setNotifOpen(false);
    // 알림 링크로 이동
    if (notification.link) {
      navigate(notification.link);
    }
  };

  // 기업 회원은 마이페이지 클릭 시 대시보드로 이동
  const mypageHref = isCompany ? "/company/dashboard" : "/mypage";

  return (
    <header className="sticky top-0 z-50 h-16 border-b border-border bg-card/95 backdrop-blur supports-[backdrop-filter]:bg-card/80">
      <div className="flex h-full items-center justify-between px-4 lg:px-6">
        {/* 모바일 메뉴 & 로고 */}
        <div className="flex items-center gap-3 lg:hidden">
          <Button variant="ghost" size="icon" className="lg:hidden">
            <Menu className="h-5 w-5" />
          </Button>
          <Link to="/" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <Sparkles className="h-4 w-4 text-primary-foreground" />
            </div>
            <span className="text-lg font-bold">FitMe</span>
          </Link>
        </div>

        {/* 검색바 */}
        <div className="hidden md:flex flex-1 max-w-xl mx-auto lg:ml-0">
          <div className="relative w-full">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="search"
              placeholder="포지션, 기업, 기술스택으로 검색하세요"
              className="w-full pl-10 pr-4 h-10 bg-secondary border-0 focus-visible:ring-primary"
            />
          </div>
        </div>

        {/* 우측 액션 */}
        <div className="flex items-center gap-2">
          {/* 크레딧 표시 (로그인 시) */}
          {user && (
            <div className="hidden sm:flex items-center gap-2 mr-2">
              <Badge variant="secondary" className="gap-1 px-3 py-1.5">
                <Coins className="h-3.5 w-3.5 text-amber-500" />
                <span className="font-medium">{creditBalance.toLocaleString()}</span>
              </Badge>
              <Button size="sm" variant="outline" asChild className="h-8">
                <Link to="/payment/products">충전</Link>
              </Button>
            </div>
          )}

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
                    navigate("/notifications");
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
                    <AvatarImage src={user.user_metadata?.avatar_url} alt={user.email || ""} />
                    <AvatarFallback className="bg-primary text-primary-foreground">
                      {user.email?.charAt(0).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent className="w-56" align="end" forceMount>
                <div className="flex items-center justify-start gap-2 p-2">
                  <div className="flex flex-col space-y-1 leading-none">
                    <p className="font-medium">{user.user_metadata?.display_name || user.email}</p>
                    <p className="text-xs text-muted-foreground">{user.email}</p>
                  </div>
                </div>
                {/* 모바일에서 크레딧 표시 */}
                <div className="sm:hidden px-2 pb-2">
                  <div className="flex items-center justify-between p-2 bg-secondary rounded-lg">
                    <div className="flex items-center gap-1">
                      <Coins className="h-4 w-4 text-amber-500" />
                      <span className="text-sm font-medium">{creditBalance.toLocaleString()}</span>
                    </div>
                    <Link to="/payment/products" className="text-xs text-primary hover:underline">
                      충전
                    </Link>
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
