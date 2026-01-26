import { Sparkles } from "lucide-react";

const footerLinks = {
  서비스: ["채용공고", "인재검색", "기업정보", "교육/이벤트"],
  기업서비스: ["인재풀 열람", "광고 상품", "구독 플랜", "API"],
  지원: ["자주 묻는 질문", "1:1 문의", "이용가이드", "공지사항"],
  회사: ["회사 소개", "채용", "블로그", "제휴 문의"],
};

const socialLinks = [
  { name: "LinkedIn", href: "#" },
  { name: "GitHub", href: "#" },
  { name: "Twitter", href: "#" },
];

export function Footer() {
  return (
    <footer className="border-t border-border bg-card">
      <div className="container py-12 lg:py-16">
        <div className="grid gap-8 lg:grid-cols-6">
          {/* 브랜드 */}
          <div className="lg:col-span-2">
            <div className="mb-4 flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary">
                <Sparkles className="h-5 w-5 text-primary-foreground" />
              </div>
              <span className="text-xl font-bold">FitMe</span>
            </div>
            <p className="mb-4 text-muted-foreground max-w-xs">
              AI 기반 리버스 리크루팅 플랫폼. 
              기업이 당신을 먼저 찾아오는 새로운 채용 경험.
            </p>
            <div className="flex gap-3">
              {socialLinks.map((link) => (
                <a
                  key={link.name}
                  href={link.href}
                  className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary text-muted-foreground hover:bg-primary hover:text-primary-foreground transition-colors"
                >
                  <span className="sr-only">{link.name}</span>
                  <span className="text-xs font-medium">{link.name[0]}</span>
                </a>
              ))}
            </div>
          </div>

          {/* 링크들 */}
          {Object.entries(footerLinks).map(([title, links]) => (
            <div key={title}>
              <h3 className="mb-4 font-semibold text-foreground">{title}</h3>
              <ul className="space-y-2">
                {links.map((link) => (
                  <li key={link}>
                    <a
                      href="#"
                      className="text-muted-foreground hover:text-primary transition-colors"
                    >
                      {link}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* 하단 */}
        <div className="mt-12 flex flex-col gap-4 border-t border-border pt-8 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">
            © 2026 FitMe. All rights reserved.
          </p>
          <div className="flex gap-4 text-sm text-muted-foreground">
            <a href="#" className="hover:text-foreground transition-colors">
              이용약관
            </a>
            <a href="#" className="hover:text-foreground transition-colors">
              개인정보처리방침
            </a>
            <a href="#" className="hover:text-foreground transition-colors">
              쿠키 정책
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
