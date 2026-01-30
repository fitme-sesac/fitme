import { Target } from "lucide-react";
import { Link } from "react-router-dom";

const footerLinkGroups = [
  {
    title: "서비스",
    items: [
      { label: "채용공고", path: "/jobs" },
      { label: "인재검색", path: "/talents" },
      { label: "기업정보", path: "/companies" },
      { label: "교육/이벤트" },
    ],
  },
  {
    title: "기업서비스",
    items: [
      { label: "인재풀 열람", path: "/talents" },
      { label: "광고 상품", path: "/payment/products" },
      { label: "구독 플랜", path: "/payment/products" },
      { label: "API" },
    ],
  },
  {
    title: "지원",
    items: [
      { label: "자주 묻는 질문", path: "/support" },
      { label: "1:1 문의", path: "/support" },
      { label: "이용가이드" },
      { label: "공지사항", path: "/support" },
    ],
  },
  {
    title: "회사",
    items: [
      { label: "회사 소개" },
      { label: "채용" },
      { label: "블로그" },
      { label: "제휴 문의" },
    ],
  },
];

const socialLinks = [
  { name: "LinkedIn", href: "#" },
  { name: "GitHub", href: "#" },
  { name: "Twitter", href: "#" },
];

function FooterLink({ item }) {
  const handlePrepare = (e) => {
    e.preventDefault();
    alert("준비중입니다.");
  };

  if (item.path) {
    return (
      <Link
        to={item.path}
        className="text-muted-foreground hover:text-primary transition-colors"
      >
        {item.label}
      </Link>
    );
  }
  return (
    <button
      type="button"
      onClick={handlePrepare}
      className="text-muted-foreground hover:text-primary transition-colors text-left"
    >
      {item.label}
    </button>
  );
}

export function Footer() {
  return (
    <footer className="border-t border-border bg-card">
      <div className="container py-12 lg:py-16">
        <div className="grid gap-8 lg:grid-cols-6">
          <div className="lg:col-span-2">
            <div className="mb-4 flex items-center gap-2">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary">
                <Target className="h-5 w-5 text-primary-foreground" />
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

          {footerLinkGroups.map((group) => (
            <div key={group.title}>
              <h3 className="mb-4 font-semibold text-foreground">{group.title}</h3>
              <ul className="space-y-2">
                {group.items.map((item) => (
                  <li key={item.label}>
                    <FooterLink item={item} />
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 flex flex-col gap-4 border-t border-border pt-8 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-muted-foreground">
            © 2026 FitMe. All rights reserved.
          </p>
          <div className="flex gap-4 text-sm text-muted-foreground">
            <button
              type="button"
              onClick={() => alert("준비중입니다.")}
              className="hover:text-foreground transition-colors"
            >
              이용약관
            </button>
            <button
              type="button"
              onClick={() => alert("준비중입니다.")}
              className="hover:text-foreground transition-colors"
            >
              개인정보처리방침
            </button>
            <button
              type="button"
              onClick={() => alert("준비중입니다.")}
              className="hover:text-foreground transition-colors"
            >
              쿠키 정책
            </button>
          </div>
        </div>
      </div>
    </footer>
  );
}
