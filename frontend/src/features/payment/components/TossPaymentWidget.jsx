/**
 * 토스페이먼츠 결제위젯 컴포넌트
 * 
 * 사용법:
 * <TossPaymentWidget
 *   orderId="uuid-order-id"
 *   orderName="크레딧 1000점"
 *   amount={10000}
 *   customerName="홍길동"
 *   onSuccess={(paymentKey, orderId, amount) => {}}
 *   onFail={(code, message) => {}}
 * />
 */
import { useEffect, useRef, useState } from 'react';
import { TOSS_CONFIG, PAYMENT_METHOD } from '../constants/paymentConstants';

export default function TossPaymentWidget({
    orderId,
    orderName,
    amount,
    customerName = '',
    customerEmail = '',
    onSuccess,
    onFail
}) {
    const paymentWidgetRef = useRef(null);
    const [isReady, setIsReady] = useState(false);
    const [error, setError] = useState(null);

    // 토스페이먼츠 SDK 초기화
    useEffect(() => {
        const clientKey = TOSS_CONFIG.getClientKey();

        if (!clientKey) {
            setError('토스페이먼츠 클라이언트 키가 설정되지 않았습니다.');
            return;
        }

        // SDK 동적 로드 (npm 패키지 설치 전까지 CDN 사용)
        const loadTossPayments = async () => {
            try {
                // @tosspayments/tosspayments-sdk 패키지가 설치되면 아래 주석 해제
                // const { loadTossPayments } = await import('@tosspayments/tosspayments-sdk');
                // const tossPayments = await loadTossPayments(clientKey);

                // CDN 방식 (패키지 설치 전 임시)
                if (!window.TossPayments) {
                    const script = document.createElement('script');
                    script.src = 'https://js.tosspayments.com/v2/standard';
                    script.async = true;
                    script.onload = () => {
                        initializeWidget();
                    };
                    document.head.appendChild(script);
                } else {
                    initializeWidget();
                }
            } catch (err) {
                console.error('토스페이먼츠 SDK 로드 실패:', err);
                setError('결제 시스템을 불러오는 중 오류가 발생했습니다.');
            }
        };

        const initializeWidget = async () => {
            try {
                const clientKey = TOSS_CONFIG.getClientKey();
                const tossPayments = window.TossPayments(clientKey);
                paymentWidgetRef.current = tossPayments;
                setIsReady(true);
            } catch (err) {
                console.error('결제위젯 초기화 실패:', err);
                setError('결제위젯 초기화에 실패했습니다.');
            }
        };

        loadTossPayments();
    }, []);

    // 결제 요청
    const handlePayment = async (method = PAYMENT_METHOD.CARD) => {
        if (!paymentWidgetRef.current) {
            setError('결제위젯이 준비되지 않았습니다.');
            return;
        }

        try {
            const payment = paymentWidgetRef.current.payment({ customerKey: orderId });

            await payment.requestPayment({
                method: getPaymentMethodName(method),
                amount: {
                    currency: 'KRW',
                    value: amount
                },
                orderId: orderId,
                orderName: orderName,
                customerName: customerName,
                customerEmail: customerEmail,
                successUrl: TOSS_CONFIG.getSuccessUrl(),
                failUrl: TOSS_CONFIG.getFailUrl()
            });
        } catch (err) {
            console.error('결제 요청 실패:', err);
            if (onFail) {
                onFail(err.code || 'UNKNOWN', err.message || '결제 요청 중 오류가 발생했습니다.');
            }
        }
    };

    // 결제 수단 이름 변환
    const getPaymentMethodName = (method) => {
        switch (method) {
            case PAYMENT_METHOD.CARD:
                return '카드';
            case PAYMENT_METHOD.TRANSFER:
                return '계좌이체';
            case PAYMENT_METHOD.EASY_PAY:
                return '간편결제';
            default:
                return '카드';
        }
    };

    if (error) {
        return (
            <div className="payment-widget-error">
                <p style={{ color: '#dc3545' }}>{error}</p>
                <button onClick={() => window.location.reload()}>
                    다시 시도
                </button>
            </div>
        );
    }

    return (
        <div className="payment-widget">
            <div className="payment-info" style={{ marginBottom: '20px', padding: '16px', backgroundColor: '#f8f9fa', borderRadius: '8px' }}>
                <h3 style={{ margin: '0 0 12px 0' }}>결제 정보</h3>
                <p style={{ margin: '4px 0' }}><strong>주문명:</strong> {orderName}</p>
                <p style={{ margin: '4px 0' }}><strong>결제 금액:</strong> {amount?.toLocaleString()}원</p>
            </div>

            <div className="payment-methods" style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                <button
                    onClick={() => handlePayment(PAYMENT_METHOD.CARD)}
                    disabled={!isReady}
                    style={{
                        padding: '12px 24px',
                        fontSize: '16px',
                        backgroundColor: isReady ? '#3182f6' : '#ccc',
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: isReady ? 'pointer' : 'not-allowed',
                        flex: '1',
                        minWidth: '120px'
                    }}
                >
                    💳 카드 결제
                </button>

                <button
                    onClick={() => handlePayment(PAYMENT_METHOD.TRANSFER)}
                    disabled={!isReady}
                    style={{
                        padding: '12px 24px',
                        fontSize: '16px',
                        backgroundColor: isReady ? '#22c55e' : '#ccc',
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: isReady ? 'pointer' : 'not-allowed',
                        flex: '1',
                        minWidth: '120px'
                    }}
                >
                    🏦 계좌이체
                </button>

                <button
                    onClick={() => handlePayment(PAYMENT_METHOD.EASY_PAY)}
                    disabled={!isReady}
                    style={{
                        padding: '12px 24px',
                        fontSize: '16px',
                        backgroundColor: isReady ? '#8b5cf6' : '#ccc',
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: isReady ? 'pointer' : 'not-allowed',
                        flex: '1',
                        minWidth: '120px'
                    }}
                >
                    📱 간편결제
                </button>
            </div>

            {!isReady && (
                <p style={{ textAlign: 'center', color: '#666', marginTop: '16px' }}>
                    결제위젯 로딩 중...
                </p>
            )}
        </div>
    );
}
