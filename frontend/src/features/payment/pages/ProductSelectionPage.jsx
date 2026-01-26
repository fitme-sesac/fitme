/**
 * 상품 선택 페이지
 * - MEMBER: ONE_TIME (크레딧) 상품만 표시
 * - EMPLOYER: ONE_TIME + SUBSCRIPTION 상품 모두 표시 (탭으로 분리)
 */
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getProducts, createPayment } from '../api/paymentApi';
import { PRODUCT_TYPE, SALE_STATUS } from '../constants/paymentConstants';
import TossPaymentWidget from '../components/TossPaymentWidget';

export default function ProductSelectionPage() {
    const navigate = useNavigate();
    const { authenticated, loading: authLoading, isEmployer, buyerType, name } = useAuth();

    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // 선택된 상품
    const [selectedProduct, setSelectedProduct] = useState(null);

    // 결제 준비 상태
    const [paymentReady, setPaymentReady] = useState(false);
    const [paymentData, setPaymentData] = useState(null);

    // EMPLOYER 전용: 현재 탭 (credit / subscription)
    const [activeTab, setActiveTab] = useState('credit');

    // 상품 목록 조회
    useEffect(() => {
        if (authLoading) return;

        if (!authenticated) {
            // 로그인 필요
            navigate('/Login', { replace: true });
            return;
        }

        fetchProducts();
    }, [authenticated, authLoading, navigate]);

    const fetchProducts = async () => {
        try {
            setLoading(true);
            const response = await getProducts(0, 50);
            setProducts(response.data.content || []);
        } catch (err) {
            console.error('상품 목록 조회 실패:', err);
            setError('상품 목록을 불러오는 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 상품 필터링 (사용자 타입 + 탭에 따라)
    const getFilteredProducts = () => {
        // 판매 중인 상품만
        let filtered = products.filter(p => p.saleStatus === SALE_STATUS.ON_SALE);

        if (isEmployer) {
            // EMPLOYER: 탭에 따라 필터링
            if (activeTab === 'credit') {
                filtered = filtered.filter(p => p.productType === PRODUCT_TYPE.ONE_TIME);
            } else {
                filtered = filtered.filter(p => p.productType === PRODUCT_TYPE.SUBSCRIPTION);
            }
        } else {
            // MEMBER: ONE_TIME만
            filtered = filtered.filter(p => p.productType === PRODUCT_TYPE.ONE_TIME);
        }

        return filtered;
    };

    // 상품 선택
    const handleSelectProduct = (product) => {
        setSelectedProduct(product);
        setPaymentReady(false);
        setPaymentData(null);
    };

    // 결제 준비 (서버에 주문 정보 등록)
    const handlePreparePayment = async () => {
        if (!selectedProduct) return;

        try {
            setLoading(true);
            const response = await createPayment({
                amount: selectedProduct.priceAmount,
                orderName: selectedProduct.name,
                buyerType: buyerType,
                productCode: selectedProduct.productCode
            });

            setPaymentData({
                orderId: response.data.orderId,
                orderName: selectedProduct.name,
                amount: Number(selectedProduct.priceAmount),
                customerName: name || ''
            });
            setPaymentReady(true);
        } catch (err) {
            console.error('결제 준비 실패:', err);
            setError(err.response?.data?.message || '결제 준비 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 결제 실패 핸들러
    const handlePaymentFail = (code, message) => {
        setError(`결제 실패: ${message} (${code})`);
        setPaymentReady(false);
    };

    // 로딩 중
    if (authLoading || loading) {
        return (
            <div style={{ textAlign: 'center', padding: '60px 20px' }}>
                <p>로딩 중...</p>
            </div>
        );
    }

    // 에러 표시
    if (error) {
        return (
            <div style={{ textAlign: 'center', padding: '60px 20px' }}>
                <p style={{ color: '#dc3545' }}>{error}</p>
                <button
                    onClick={() => { setError(null); fetchProducts(); }}
                    style={{ marginTop: '16px', padding: '8px 16px' }}
                >
                    다시 시도
                </button>
            </div>
        );
    }

    const filteredProducts = getFilteredProducts();

    return (
        <div style={{ maxWidth: '800px', margin: '0 auto', padding: '40px 20px' }}>
            {/* 헤더 */}
            <div style={{ marginBottom: '32px' }}>
                <h1 style={{ fontSize: '28px', fontWeight: 'bold', marginBottom: '8px' }}>
                    상품 구매
                </h1>
                <p style={{ color: '#666' }}>
                    {isEmployer ? '크레딧 또는 구독 상품을 선택해 주세요.' : '크레딧 상품을 선택해 주세요.'}
                </p>
            </div>

            {/* EMPLOYER 전용 탭 */}
            {isEmployer && (
                <div style={{ marginBottom: '24px', display: 'flex', gap: '8px' }}>
                    <button
                        onClick={() => { setActiveTab('credit'); setSelectedProduct(null); setPaymentReady(false); }}
                        style={{
                            padding: '12px 24px',
                            fontSize: '14px',
                            fontWeight: activeTab === 'credit' ? 'bold' : 'normal',
                            backgroundColor: activeTab === 'credit' ? '#3182f6' : '#e5e7eb',
                            color: activeTab === 'credit' ? 'white' : '#374151',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer'
                        }}
                    >
                        💰 크레딧 충전
                    </button>
                    <button
                        onClick={() => { setActiveTab('subscription'); setSelectedProduct(null); setPaymentReady(false); }}
                        style={{
                            padding: '12px 24px',
                            fontSize: '14px',
                            fontWeight: activeTab === 'subscription' ? 'bold' : 'normal',
                            backgroundColor: activeTab === 'subscription' ? '#8b5cf6' : '#e5e7eb',
                            color: activeTab === 'subscription' ? 'white' : '#374151',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer'
                        }}
                    >
                        📦 구독 플랜
                    </button>
                </div>
            )}

            {/* 상품 목록 */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '16px', marginBottom: '32px' }}>
                {filteredProducts.length === 0 ? (
                    <p style={{ color: '#666', gridColumn: '1 / -1' }}>등록된 상품이 없습니다.</p>
                ) : (
                    filteredProducts.map(product => (
                        <div
                            key={product.productId}
                            onClick={() => handleSelectProduct(product)}
                            style={{
                                padding: '20px',
                                border: selectedProduct?.productId === product.productId
                                    ? '2px solid #3182f6'
                                    : '1px solid #e5e7eb',
                                borderRadius: '12px',
                                cursor: 'pointer',
                                backgroundColor: selectedProduct?.productId === product.productId
                                    ? '#eff6ff'
                                    : 'white',
                                transition: 'all 0.2s'
                            }}
                        >
                            <h3 style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '8px' }}>
                                {product.name}
                            </h3>

                            {product.productType === PRODUCT_TYPE.ONE_TIME && product.creditAmount && (
                                <p style={{ color: '#666', fontSize: '14px', marginBottom: '8px' }}>
                                    💎 {product.creditAmount.toLocaleString()} 크레딧
                                </p>
                            )}

                            {product.productType === PRODUCT_TYPE.SUBSCRIPTION && product.planTier && (
                                <p style={{ color: '#8b5cf6', fontSize: '14px', marginBottom: '8px' }}>
                                    🏷️ {product.planTier} 플랜
                                </p>
                            )}

                            <p style={{ fontSize: '20px', fontWeight: 'bold', color: '#3182f6' }}>
                                {Number(product.priceAmount).toLocaleString()}원
                            </p>
                        </div>
                    ))
                )}
            </div>

            {/* 선택된 상품 + 결제 영역 */}
            {selectedProduct && !paymentReady && (
                <div style={{
                    padding: '24px',
                    backgroundColor: '#f8f9fa',
                    borderRadius: '12px',
                    marginBottom: '24px'
                }}>
                    <h3 style={{ marginBottom: '16px' }}>선택된 상품</h3>
                    <p><strong>{selectedProduct.name}</strong></p>
                    <p style={{ fontSize: '24px', fontWeight: 'bold', color: '#3182f6', marginTop: '8px' }}>
                        {Number(selectedProduct.priceAmount).toLocaleString()}원
                    </p>
                    <button
                        onClick={handlePreparePayment}
                        style={{
                            marginTop: '20px',
                            width: '100%',
                            padding: '16px',
                            fontSize: '18px',
                            fontWeight: 'bold',
                            backgroundColor: '#3182f6',
                            color: 'white',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer'
                        }}
                    >
                        결제하기
                    </button>
                </div>
            )}

            {/* 결제 위젯 */}
            {paymentReady && paymentData && (
                <div style={{
                    padding: '24px',
                    backgroundColor: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: '12px'
                }}>
                    <TossPaymentWidget
                        orderId={paymentData.orderId}
                        orderName={paymentData.orderName}
                        amount={paymentData.amount}
                        customerName={paymentData.customerName}
                        onFail={handlePaymentFail}
                    />

                    <button
                        onClick={() => { setPaymentReady(false); setPaymentData(null); }}
                        style={{
                            marginTop: '16px',
                            width: '100%',
                            padding: '12px',
                            fontSize: '14px',
                            backgroundColor: '#e5e7eb',
                            color: '#374151',
                            border: 'none',
                            borderRadius: '8px',
                            cursor: 'pointer'
                        }}
                    >
                        취소하고 다시 선택하기
                    </button>
                </div>
            )}
        </div>
    );
}
