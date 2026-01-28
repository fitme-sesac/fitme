/**
 * 결제 관련 API 호출 모듈
 */
import { http } from "../../../api/http";

// ===== 인증 =====

/**
 * 현재 로그인 상태 및 사용자 정보 조회
 * @returns {Promise<{authenticated: boolean, name: string, role: string}>}
 */
export const getAuthStatus = () => http.get('/api/auth/status');

// ===== 상품 =====

/**
 * 상품 목록 조회 (페이징)
 * @param {number} page - 페이지 번호 (0부터 시작)
 * @param {number} size - 페이지 크기
 * @returns {Promise<{content: Array, totalPages: number, totalElements: number}>}
 */
export const getProducts = (page = 0, size = 20) => 
  http.get('/api/v1/products', { params: { page, size } });

/**
 * 상품 단건 조회
 * @param {number} productId - 상품 ID
 */
export const getProduct = (productId) => 
  http.get(`/api/v1/products/${productId}`);

// ===== 결제 =====

/**
 * 결제 생성 (주문 정보 서버 등록)
 * - 토스 결제창 호출 전에 먼저 호출해야 함
 * @param {Object} data - 결제 생성 요청
 * @param {number} data.amount - 결제 금액
 * @param {string} data.orderName - 주문명
 * @param {string} data.buyerType - 구매자 타입 (MEMBER | EMPLOYER)
 * @param {string} data.productCode - 상품 코드
 * @param {string} [data.method] - 결제 수단 (CARD | TRANSFER | EASY_PAY)
 * @param {string} [data.idempotencyKey] - 멱등성 키 (선택)
 * @returns {Promise<{orderId: string, amount: number, orderName: string}>}
 */
export const createPayment = (data) => 
  http.post('/api/v1/payments/request', data);

/**
 * 결제 승인 (토스 인증 성공 후 최종 승인)
 * @param {Object} data - 결제 승인 요청
 * @param {string} data.paymentKey - 토스에서 받은 paymentKey
 * @param {string} data.orderId - 주문 ID
 * @param {number} data.amount - 결제 금액
 */
export const confirmPayment = (data) => 
  http.post('/api/v1/payments/confirm', data);

/**
 * 결제 취소
 * @param {string} orderId - 주문 ID
 * @param {Object} data - 취소 요청
 * @param {string} data.cancelReason - 취소 사유
 * @param {number} [data.cancelAmount] - 취소 금액 (부분취소 시)
 */
export const cancelPayment = (orderId, data) => 
  http.post(`/api/v1/payments/${orderId}/cancel`, data);

/**
 * 내 결제 목록 조회
 * @param {string} roleType - 사용자 타입 (CANDIDATE | EMPLOYER)
 * @param {number} page - 페이지 번호
 * @param {number} size - 페이지 크기
 */
export const getMyPayments = (roleType = 'CANDIDATE', page = 0, size = 10) => 
  http.get('/api/v1/payments/me', { params: { roleType, page, size } });

/**
 * 결제 상세 조회
 * @param {string} orderId - 주문 ID
 */
export const getPayment = (orderId) => 
  http.get(`/api/v1/payments/${orderId}`);
