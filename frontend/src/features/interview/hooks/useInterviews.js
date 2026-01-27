import { useState, useEffect, useCallback } from 'react';
import * as interviewApi from '../api/interviewApi';

/**
 * 면접 일정 관리 Hook
 */
export const useInterviews = (type = 'candidate') => {
  const [interviews, setInterviews] = useState([]);
  const [upcomingInterviews, setUpcomingInterviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 면접 목록 조회
  const fetchInterviews = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let data;
      if (type === 'employer') {
        data = await interviewApi.getEmployerInterviews();
      } else {
        data = await interviewApi.getMyInterviews();
      }
      setInterviews(data);
    } catch (err) {
      setError(err.response?.data?.message || '면접 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [type]);

  // 다가오는 면접 조회
  const fetchUpcomingInterviews = useCallback(async () => {
    try {
      let data;
      if (type === 'employer') {
        data = await interviewApi.getEmployerUpcomingInterviews();
      } else {
        data = await interviewApi.getUpcomingInterviews();
      }
      setUpcomingInterviews(data);
    } catch (err) {
      console.error('다가오는 면접 조회 실패:', err);
    }
  }, [type]);

  // 면접 생성 (기업용)
  const createInterview = useCallback(async (data) => {
    setLoading(true);
    try {
      const interviewId = await interviewApi.createInterview(data);
      await fetchInterviews();
      return interviewId;
    } catch (err) {
      setError(err.response?.data?.message || '면접 일정 생성에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  }, [fetchInterviews]);

  // 면접 응답 (지원자용)
  const respondToInterview = useCallback(async (interviewId, response, message) => {
    setLoading(true);
    try {
      await interviewApi.respondToInterview(interviewId, { response, message });
      await fetchInterviews();
      await fetchUpcomingInterviews();
    } catch (err) {
      setError(err.response?.data?.message || '면접 응답에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  }, [fetchInterviews, fetchUpcomingInterviews]);

  // 면접 취소
  const cancelInterview = useCallback(async (interviewId) => {
    setLoading(true);
    try {
      await interviewApi.cancelInterview(interviewId);
      await fetchInterviews();
      await fetchUpcomingInterviews();
    } catch (err) {
      setError(err.response?.data?.message || '면접 취소에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  }, [fetchInterviews, fetchUpcomingInterviews]);

  // 면접 완료 (기업용)
  const completeInterview = useCallback(async (interviewId) => {
    setLoading(true);
    try {
      await interviewApi.completeInterview(interviewId);
      await fetchInterviews();
    } catch (err) {
      setError(err.response?.data?.message || '면접 완료 처리에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  }, [fetchInterviews]);

  useEffect(() => {
    fetchInterviews();
    fetchUpcomingInterviews();
  }, [fetchInterviews, fetchUpcomingInterviews]);

  return {
    interviews,
    upcomingInterviews,
    loading,
    error,
    fetchInterviews,
    fetchUpcomingInterviews,
    createInterview,
    respondToInterview,
    cancelInterview,
    completeInterview,
  };
};

export default useInterviews;
