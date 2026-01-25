/**
 * 결제 관련 상수 정의
 */

// 결제 수단 (백엔드 PaymentMethod enum과 동일)
export const PAYMENT_METHOD = {
    CARD: 'CARD',           // 카드
    TRANSFER: 'TRANSFER',   // 계좌이체
    EASY_PAY: 'EASY_PAY'    // 간편결제
};

// 상품 타입 (백엔드 ProductType enum과 동일)
export const PRODUCT_TYPE = {
    ONE_TIME: 'ONE_TIME',       // 단건 결제 (크레딧)
    SUBSCRIPTION: 'SUBSCRIPTION' // 정기 구독
};

// 구매자 타입 (백엔드 BuyerType enum과 동일)
export const BUYER_TYPE = {
    MEMBER: 'MEMBER',     // 일반 회원 (CANDIDATE)
    EMPLOYER: 'EMPLOYER'  // 기업 회원
};

// 상품 판매 상태
export const SALE_STATUS = {
    ON_SALE: 'ON_SALE',   // 판매 중
    PAUSED: 'PAUSED',     // 일시정지
    STOPPED: 'STOPPED'    // 판매 종료
};

// 결제 성공/실패 URL
export const PAYMENT_URLS = {
    SUCCESS: '/payment/success',
    FAIL: '/payment/fail'
};

// 토스페이먼츠 설정
export const TOSS_CONFIG = {
    // 클라이언트 키는 환경변수에서 가져옴 (VITE_TOSS_CLIENT_KEY)
    getClientKey: () => import.meta.env.VITE_TOSS_CLIENT_KEY || '',

    // 리다이렉트 URL 생성
    getSuccessUrl: () => `${window.location.origin}${PAYMENT_URLS.SUCCESS}`,
    getFailUrl: () => `${window.location.origin}${PAYMENT_URLS.FAIL}`
};
