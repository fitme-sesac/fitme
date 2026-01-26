/**
 * 토스페이먼츠 결제위젯 컴포넌트 (Payment Widget SDK)
 * 
 * 사용법:
 * <TossPaymentWidget
 *   orderId="uuid-order-id"
 *   orderName="크레딧 1000점"
 *   amount={10000}
 *   customerName="홍길동"
 *   customerEmail="user@email.com"
 * />
 */
import { useEffect, useRef, useState } from 'react';
import { TOSS_CONFIG, PAYMENT_URLS } from '../constants/paymentConstants';

export default function TossPaymentWidget({
    orderId,
    orderName,
    amount,
    customerName = '',
    customerEmail = ''
}) {
    const paymentWidgetRef = useRef(null);
    const paymentMethodsWidgetRef = useRef(null);
    const agreementWidgetRef = useRef(null);
    const [isReady, setIsReady] = useState(false);
    const [error, setError] = useState(null);
    const [isProcessing, setIsProcessing] = useState(false);

    // customerKey 생성 (비회원은 ANONYMOUS, 회원은 고유 ID 사용)
    const customerKey = orderId || `ANONYMOUS_${Date.now()}`;

    // 토스페이먼츠 결제위젯 SDK 초기화
    useEffect(() => {
        const clientKey = TOSS_CONFIG.getClientKey();

        if (!clientKey) {
            setError('토스페이먼츠 클라이언트 키가 설정되지 않았습니다.');
            return;
        }

        // 결제위젯 SDK 동적 로드
        const loadPaymentWidget = async () => {
            try {
                // 결제위젯 SDK 스크립트 로드
                if (!window.PaymentWidget) {
                    const script = document.createElement('script');
                    script.src = 'https://js.tosspayments.com/v1/payment-widget';
                    script.async = true;
                    script.onload = () => {
                        initializeWidget(clientKey);
                    };
                    script.onerror = () => {
                        setError('결제위젯 SDK를 불러오는데 실패했습니다.');
                    };
                    document.head.appendChild(script);
                } else {
                    initializeWidget(clientKey);
                }
            } catch (err) {
                console.error('결제위젯 SDK 로드 실패:', err);
                setError('결제 시스템을 불러오는 중 오류가 발생했습니다.');
            }
        };

        const initializeWidget = async (clientKey) => {
            try {
                // 결제위젯 인스턴스 생성
                const paymentWidget = window.PaymentWidget(clientKey, customerKey);
                paymentWidgetRef.current = paymentWidget;

                // 결제 수단 위젯 렌더링
                const paymentMethodsWidget = paymentWidget.renderPaymentMethods(
                    '#payment-methods',
                    { value: amount },
                    { variantKey: 'DEFAULT' }
                );
                paymentMethodsWidgetRef.current = paymentMethodsWidget;

                // 약관 동의 위젯 렌더링
                const agreementWidget = paymentWidget.renderAgreement('#agreement', {
                    variantKey: 'AGREEMENT'
                });
                agreementWidgetRef.current = agreementWidget;

                setIsReady(true);
            } catch (err) {
                console.error('결제위젯 초기화 실패:', err);
                setError('결제위젯 초기화에 실패했습니다: ' + err.message);
            }
        };

        loadPaymentWidget();

        // 클린업
        return () => {
            // 위젯 정리 (필요시)
        };
    }, [amount, customerKey]);

    // 금액 변경 시 위젯 업데이트
    useEffect(() => {
        if (paymentMethodsWidgetRef.current && amount) {
            paymentMethodsWidgetRef.current.updateAmount(amount);
        }
    }, [amount]);

    // 결제 요청
    const handlePayment = async () => {
        if (!paymentWidgetRef.current) {
            setError('결제위젯이 준비되지 않았습니다.');
            return;
        }

        setIsProcessing(true);

        try {
            await paymentWidgetRef.current.requestPayment({
                orderId: orderId,
                orderName: orderName,
                customerName: customerName,
                customerEmail: customerEmail,
                successUrl: `${window.location.origin}${PAYMENT_URLS.SUCCESS}`,
                failUrl: `${window.location.origin}${PAYMENT_URLS.FAIL}`
            });
        } catch (err) {
            console.error('결제 요청 실패:', err);
            if (err.code === 'USER_CANCEL') {
                // 사용자가 결제를 취소한 경우
                setError(null);
            } else {
                setError(`결제 실패: ${err.message} (${err.code})`);
            }
        } finally {
            setIsProcessing(false);
        }
    };

    if (error) {
        return (
            <div className="payment-widget-error" style={{ padding: '20px', textAlign: 'center' }}>
                <p style={{ color: '#dc3545', marginBottom: '16px' }}>{error}</p>
                <button
                    onClick={() => window.location.reload()}
                    style={{
                        padding: '10px 20px',
                        backgroundColor: '#6c757d',
                        color: 'white',
                        border: 'none',
                        borderRadius: '6px',
                        cursor: 'pointer'
                    }}
                >
                    다시 시도
                </button>
            </div>
        );
    }

    return (
        <div className="payment-widget" style={{ maxWidth: '540px', margin: '0 auto' }}>
            {/* 주문 정보 */}
            <div className="payment-info" style={{
                marginBottom: '20px',
                padding: '16px',
                backgroundColor: '#f8f9fa',
                borderRadius: '8px'
            }}>
                <h3 style={{ margin: '0 0 12px 0' }}>결제 정보</h3>
                <p style={{ margin: '4px 0' }}><strong>주문명:</strong> {orderName}</p>
                <p style={{ margin: '4px 0' }}><strong>결제 금액:</strong> {amount?.toLocaleString()}원</p>
            </div>

            {/* 결제 수단 위젯 영역 */}
            <div id="payment-methods" style={{ marginBottom: '20px' }}></div>

            {/* 약관 동의 위젯 영역 */}
            <div id="agreement" style={{ marginBottom: '20px' }}></div>

            {/* 결제 버튼 */}
            <button
                onClick={handlePayment}
                disabled={!isReady || isProcessing}
                style={{
                    width: '100%',
                    padding: '16px 24px',
                    fontSize: '18px',
                    fontWeight: 'bold',
                    backgroundColor: isReady && !isProcessing ? '#3182f6' : '#ccc',
                    color: 'white',
                    border: 'none',
                    borderRadius: '8px',
                    cursor: isReady && !isProcessing ? 'pointer' : 'not-allowed'
                }}
            >
                {isProcessing ? '처리 중...' : `${amount?.toLocaleString()}원 결제하기`}
            </button>

            {!isReady && (
                <p style={{ textAlign: 'center', color: '#666', marginTop: '16px' }}>
                    결제위젯 로딩 중...
                </p>
            )}
        </div>
    );
}

