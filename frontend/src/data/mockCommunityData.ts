
export const MOCK_POSTS = [
    {
        id: "1",
        author: {
            id: "user1",
            name: "김개발",
            title: "Senior Frontend Developer",
            company: "테크스타트업",
            avatar: "",
        },
        content: "개발자로 성장하면서 가장 중요했던 건 '왜'를 끊임없이 질문하는 것이었습니다.\n\n코드를 작성할 때 단순히 동작하는 것에 만족하지 않고, 왜 이 방식이 최선인지, 더 나은 방법은 없는지 고민하는 습관이 저를 성장시켰어요.\n\n주니어 개발자분들께 조금이나마 도움이 되었으면 합니다. 💪",
        category: "career_tips" as const,
        likes: 42,
        comments: 8,
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
    {
        id: "2",
        author: {
            id: "company1",
            name: "인공지능스타트업",
            title: "AI 혁신 기업",
            avatar: "",
        },
        content: "🎉 시리즈 A 투자 유치를 완료했습니다!\n\n저희 팀과 함께 AI의 미래를 만들어갈 인재를 찾고 있습니다. Frontend, Backend, ML Engineer 포지션 오픈했으니 많은 관심 부탁드립니다.",
        category: "company_news" as const,
        link: {
            url: "https://example.com/careers",
            title: "AI스타트업 채용 공고",
            description: "Frontend, Backend, ML Engineer 포지션 채용 중",
            image: "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=600",
        },
        likes: 128,
        comments: 23,
        createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
        isLiked: true,
    },
    {
        id: "3",
        author: {
            id: "user2",
            name: "이면접",
            title: "HR Manager",
            company: "빅테크",
        },
        content: "기술 면접에서 가장 많이 하는 실수 TOP 3\n\n1. 모르는 문제에 대해 아는 척하기\n→ 차라리 모른다고 솔직히 말하고, 어떻게 접근할지 설명하세요\n\n2. 코드만 작성하고 설명하지 않기\n→ 생각 과정을 말로 표현하세요\n\n3. 질문하지 않기\n→ 요구사항이 불명확하면 반드시 물어보세요\n\n이것만 기억해도 면접 결과가 달라집니다! 🎯",
        category: "career_tips" as const,
        likes: 256,
        comments: 45,
        createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
    {
        id: "4",
        author: {
            id: "user3",
            name: "박주니어",
            title: "Junior Developer",
            company: "스타트업",
        },
        content: "스타트업과 대기업 중에 어디가 더 성장하기 좋을까요? 🤔\n\n현재 2년차 개발자인데, 이직을 고민하고 있습니다. 대기업의 체계적인 교육 vs 스타트업의 다양한 경험... 선배님들의 조언 부탁드립니다!",
        category: "qna" as const,
        likes: 18,
        comments: 32,
        createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
    {
        id: "5",
        author: {
            id: "company2",
            name: "핀테크은행",
            title: "디지털 금융 혁신",
        },
        content: "📢 2024 개발자 채용 시작!\n\nSpring Boot, React 기반의 핀테크 서비스를 함께 만들어갈 분을 찾습니다.\n\n✅ 경력 3년 이상\n✅ 유연근무제\n✅ 스톡옵션 제공\n\n관심 있으시면 DM 주세요!",
        category: "company_news" as const,
        likes: 67,
        comments: 12,
        createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(),
        isLiked: false,
    },
];

export const POPULAR_POSTS = [
    { id: "1", title: "개발자 커리어 성장을 위한 5가지 팁", likes: 128, author: "김개발" },
    { id: "2", title: "스타트업 vs 대기업, 어디가 좋을까?", likes: 95, author: "이창업" },
    { id: "3", title: "면접에서 자주 받는 질문 총정리", likes: 87, author: "박면접" },
    { id: "4", title: "2024 개발자 연봉 트렌드", likes: 76, author: "최연봉" },
    { id: "5", title: "주니어 개발자가 알아야 할 것들", likes: 64, author: "정주니" },
];

export const RECOMMENDED_MEMBERS = [
    { id: "1", name: "김개발", title: "Senior Developer", company: "테크컴퍼니", avatar: "" },
    { id: "2", name: "이스타트업", title: "Co-Founder", company: "AI스타트업", avatar: "" },
    { id: "3", name: "박리크루터", title: "HR Manager", company: "빅테크", avatar: "" },
];

export const ANNOUNCEMENTS = [
    { id: "1", title: "FitMe 커뮤니티 이용 가이드", date: "2024.01.15", isNew: true },
    { id: "2", title: "개인정보 처리방침 개정 안내", date: "2024.01.10", isNew: false },
    { id: "3", title: "서비스 점검 안내 (1/20)", date: "2024.01.08", isNew: false },
];

// Individual User Mock Data
export const MY_POSTS = [
    {
        id: "1",
        title: "신입 개발자 취업 준비 꿀팁 공유합니다",
        content: "안녕하세요. 이번에 취업에 성공하게 되어 제가 공부했던 방법들을 공유하려고 합니다...",
        category: "career_tips",
        categoryLabel: "커리어 꿀팁",
        likes: 42,
        comments: 15,
        views: 1205,
        createdAt: "2024-01-20",
    },
    {
        id: "2",
        title: "React Query vs SWR 어떤걸 사용하시나요?",
        content: "현재 프로젝트에 도입하려고 하는데 장단점이 궁금합니다.",
        category: "qna",
        categoryLabel: "Q&A",
        likes: 12,
        comments: 8,
        views: 450,
        createdAt: "2024-01-15",
    },
    {
        id: "3",
        title: "판교 출퇴근 2시간... 자취가 답일까요?",
        content: "매일 왕복 4시간이 길바닥에 버려지니 너무 힘드네요 ㅠㅠ",
        category: "general",
        categoryLabel: "자유게시판",
        likes: 56,
        comments: 32,
        views: 2100,
        createdAt: "2024-01-10",
    }
];

export const MY_COMMENTS = [
    {
        id: "c1",
        postTitle: "백엔드 개발자 로드맵 질문있습니다",
        content: "Java랑 Spring Boot 먼저 깊게파시는걸 추천드려요!",
        createdAt: "2024-01-22",
    },
    {
        id: "c2",
        postTitle: "이력서 피드백 부탁드립니다",
        content: "프로젝트 경험 부분에서 구체적인 수치를 언급하면 더 좋을 것 같습니다.",
        createdAt: "2024-01-18",
    }
];

export const LIKED_POSTS = [
    {
        id: "4",
        title: "2024년 개발자 연봉 테이블 정리",
        author: "테크리크루터",
        category: "company_news",
        categoryLabel: "기업 뉴스",
        createdAt: "2024-01-05",
    },
    {
        id: "5",
        title: "면접에서 가장 많이 탈락하는 유형",
        author: "시니어개발자",
        category: "career_tips",
        categoryLabel: "커리어 꿀팁",
        createdAt: "2024-01-01",
    }
];

// Company User Mock Data
export const COMPANY_MY_POSTS = [
    {
        id: "1",
        title: "우리 회사의 개발 문화에 대해 소개합니다",
        content: "자율 출퇴근제와 코드 리뷰 문화를 중심으로...",
        category: "company_culture",
        categoryLabel: "기업 문화",
        likes: 152,
        comments: 23,
        views: 3400,
        createdAt: "2024-01-20",
    },
    {
        id: "2",
        title: "신입 개발자 채용 관련 자주 묻는 질문",
        content: "채용 프로세스와 코딩 테스트 난이도에 대해...",
        category: "hiring",
        categoryLabel: "채용",
        likes: 89,
        comments: 45,
        views: 5600,
        createdAt: "2024-01-15",
    },
];

export const COMPANY_MY_COMMENTS = [
    {
        id: "c1",
        postTitle: "연봉 협상 시 기준이 어떻게 되나요?",
        content: "내규에 따르지만 경력에 따라 유연하게 조정 가능합니다.",
        createdAt: "2024-01-22",
    },
];

export const COMPANY_LIKED_POSTS = [
    {
        id: "4",
        title: "2024년 개발 트렌드 분석",
        author: "TechTrend",
        category: "tech",
        categoryLabel: "기술 트렌드",
        createdAt: "2024-01-05",
    },
];
