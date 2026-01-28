import { useState, useEffect, useCallback } from 'react';
import {
  getMyEmployer,
  getDashboardStats,
  getRecentApplications,
  getWalletBalance,
  getSubscription,
} from '../api/employerApi';

/**
 * 현재 로그인한 기업 회원의 기업 정보를 가져오는 훅
 */
export function useMyEmployer() {
  const [employer, setEmployer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchEmployer = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getMyEmployer();
      setEmployer(data);
    } catch (err) {
      setError(err.response?.data?.message || '기업 정보를 불러오는데 실패했습니다.');
      setEmployer(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchEmployer();
  }, [fetchEmployer]);

  return { employer, loading, error, refetch: fetchEmployer };
}

/**
 * 대시보드 통계 정보를 가져오는 훅
 */
export function useDashboardStats() {
  const [stats, setStats] = useState({
    totalJobPostings: 0,
    activeJobPostings: 0,
    totalApplications: 0,
    newApplications: 0,
    interviewScheduled: 0,
    creditBalance: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchStats = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getDashboardStats();
      setStats(data);
    } catch (err) {
      setError(err.response?.data?.message || '통계 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  return { stats, loading, error, refetch: fetchStats };
}

/**
 * 최근 지원자 목록을 가져오는 훅
 */
export function useRecentApplications(limit = 5) {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchApplications = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getRecentApplications(limit);
      setApplications(data);
    } catch (err) {
      setError(err.response?.data?.message || '지원자 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [limit]);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  return { applications, loading, error, refetch: fetchApplications };
}

/**
 * 크레딧 잔액 정보를 가져오는 훅
 */
export function useWalletBalance() {
  const [balance, setBalance] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchBalance = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getWalletBalance();
      setBalance(data.balance || 0);
    } catch (err) {
      setError(err.response?.data?.message || '잔액 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBalance();
  }, [fetchBalance]);

  return { balance, loading, error, refetch: fetchBalance };
}

/**
 * 구독 정보를 가져오는 훅
 */
export function useSubscription() {
  const [subscription, setSubscription] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchSubscription = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getSubscription();
      setSubscription(data);
    } catch (err) {
      setError(err.response?.data?.message || '구독 정보를 불러오는데 실패했습니다.');
      setSubscription(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSubscription();
  }, [fetchSubscription]);

  return { subscription, loading, error, refetch: fetchSubscription };
}

export default {
  useMyEmployer,
  useDashboardStats,
  useRecentApplications,
  useWalletBalance,
  useSubscription,
};
