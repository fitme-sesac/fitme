/**
 * 결제 성공 페이지
 * - 토스 결제 인증 성공 후 리다이렉트되는 페이지
 * - URL 파라미터에서 paymentKey, orderId, amount 추출
 * - 백엔드에 결제 승인 요청
 */
import { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { confirmPayment } from '../api/paymentApi';

export default function PaymentSuccessPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const [status, setStatus] = useState('processing'); // processing | success | error
    const [paymentResult, setPaymentResult] = useState(null);
    const [error, setError] = useState(null);

    // 중복 호출 방지
    const confirmedRef = useRef(false);

    useEffect(() => {
        // 중복 호출 방지
        if (confirmedRef.current) return;
        confirmedRef.current = true;

        const paymentKey = searchParams.get('paymentKey');
        const orderId = searchParams.get('orderId');
        const amount = searchParams.get('amount');

        if (!paymentKey || !orderId || !amount) {
            setStatus('error');
            setError('결제 정보가 올바르지 않습니다.');
            return;
        }

        handleConfirmPayment(paymentKey, orderId, Number(amount));
    }, [searchParams]);

    const handleConfirmPayment = async (paymentKey, orderId, amount) => {
        try {
            const response = await confirmPayment({
                paymentKey,
                orderId,
                amount
            });

            setPaymentResult(response.data);
            setStatus('success');
        } catch (err) {
            console.error('결제 승인 실패:', err);
            setStatus('error');
            setError(err.response?.data?.message || '결제 승인 중 오류가 발생했습니다.');
        }
    };

    // 처리 중
    if (status === 'processing') {
        return (
            <div style={{
                maxWidth: '500px',
                margin: '0 auto',
                padding: '80px 20px',
                textAlign: 'center'
            }}>
                <div style={{ fontSize: '48px', marginBottom: '24px' }}>⏳</div>
                <h1 style={{ fontSize: '24px', marginBottom: '16px' }}>결제 승인 중...</h1>
                <p style={{ color: '#666' }}>잠시만 기다려 주세요.</p>
            </div>
        );
    }

    // 성공
    if (status === 'success') {
        return (
            <div style={{
                maxWidth: '500px',
                margin: '0 auto',
                padding: '80px 20px',
                textAlign: 'center'
            }}>
                <div style={{ fontSize: '64px', marginBottom: '24px' }}>✅</div>
                <h1 style={{ fontSize: '28px', fontWeight: 'bold', marginBottom: '16px', color: '#22c55e' }}>
                    결제가 완료되었습니다!
                </h1>

                {paymentResult && (
                    <div style={{
                        backgroundColor: '#f8f9fa',
                        padding: '24px',
                        borderRadius: '12px',
                        marginTop: '24px',
                        marginBottom: '32px',
                        textAlign: 'left'
                    }}>
                        <h3 style={{ marginBottom: '16px', fontSize: '16px', color: '#666' }}>결제 정보</h3>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                            <span style={{ color: '#666' }}>주문번호</span>
                            <span style={{ fontWeight: 'bold', fontSize: '12px' }}>{paymentResult.orderId}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                            <span style={{ color: '#666' }}>상품명</span>
                            <span style={{ fontWeight: 'bold' }}>{paymentResult.orderName}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                            <span style={{ color: '#666' }}>결제 금액</span>
                            <span style={{ fontWeight: 'bold', color: '#3182f6' }}>
                                {Number(paymentResult.paidAmount).toLocaleString()}원
                            </span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                            <span style={{ color: '#666' }}>결제 수단</span>
                            <span style={{ fontWeight: 'bold' }}>{paymentResult.method}</span>
                        </div>
                    </div>
                )}

                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                    <Link
                        to="/"
                        style={{
                            padding: '14px 28px',
                            backgroundColor: '#3182f6',
                            color: 'white',
                            textDecoration: 'none',
                            borderRadius: '8px',
                            fontWeight: 'bold'
                        }}
                    >
                        홈으로
                    </Link>
                    <Link
                        to="/products"
                        style={{
                            padding: '14px 28px',
                            backgroundColor: '#e5e7eb',
                            color: '#374151',
                            textDecoration: 'none',
                            borderRadius: '8px',
                            fontWeight: 'bold'
                        }}
                    >
                        추가 구매
                    </Link>
                </div>
            </div>
        );
    }

    // 에러
    return (
        <div style={{
            maxWidth: '500px',
            margin: '0 auto',
            padding: '80px 20px',
            textAlign: 'center'
        }}>
            <div style={{ fontSize: '64px', marginBottom: '24px' }}>❌</div>
            <h1 style={{ fontSize: '28px', fontWeight: 'bold', marginBottom: '16px', color: '#dc3545' }}>
                결제 승인 실패
            </h1>
            <p style={{ color: '#666', marginBottom: '32px' }}>{error}</p>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
                <Link
                    to="/products"
                    style={{
                        padding: '14px 28px',
                        backgroundColor: '#3182f6',
                        color: 'white',
                        textDecoration: 'none',
                        borderRadius: '8px',
                        fontWeight: 'bold'
                    }}
                >
                    다시 시도하기
                </Link>
                <Link
                    to="/"
                    style={{
                        padding: '14px 28px',
                        backgroundColor: '#e5e7eb',
                        color: '#374151',
                        textDecoration: 'none',
                        borderRadius: '8px',
                        fontWeight: 'bold'
                    }}
                >
                    홈으로
                </Link>
            </div>
        </div>
    );
}
