/**
 * 인증 상태 조회 커스텀 훅
 */
import { useState, useEffect } from 'react';
import { getAuthStatus } from '../api/paymentApi';

/**
 * 현재 로그인 상태 및 사용자 정보를 조회하는 훅
 * 
 * @returns {{
 *   authenticated: boolean,
 *   name: string,
 *   role: string,
 *   loading: boolean,
 *   error: string|null,
 *   isEmployer: boolean,
 *   buyerType: string
 * }}
 */
export function useAuth() {
    const [auth, setAuth] = useState({
        authenticated: false,
        name: '',
        role: ''
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        getAuthStatus()
            .then(res => {
                setAuth(res.data);
                setError(null);
            })
            .catch(err => {
                console.error('인증 상태 조회 실패:', err);
                setError('인증 정보를 불러올 수 없습니다.');
                setAuth({ authenticated: false, name: '', role: '' });
            })
            .finally(() => {
                setLoading(false);
            });
    }, []);

    // EMPLOYER 여부 판단
    const isEmployer = auth.role === 'EMPLOYER';

    // 결제 API에서 사용할 buyerType (CANDIDATE → MEMBER 매핑)
    const buyerType = isEmployer ? 'EMPLOYER' : 'MEMBER';

    return {
        ...auth,
        loading,
        error,
        isEmployer,
        buyerType
    };
}
