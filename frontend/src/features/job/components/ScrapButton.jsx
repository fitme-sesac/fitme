import React, { useState, useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../../contexts/AuthContext';
import { toggleScrap, getScrapStatus } from '../api/jobApi';

/**
 * 스크랩 버튼 컴포넌트
 * - 로그인 상태에서만 스크랩 상태 조회/토글 가능
 * - 스크랩 토글 후 마이페이지 관련 쿼리를 무효화하여 데이터 갱신
 */
const ScrapButton = ({ jobId, initialScraped = false, onToggle, size = 'md', showText = false }) => {
  const [isScraped, setIsScraped] = useState(initialScraped);
  const [loading, setLoading] = useState(false);
  const [statusLoaded, setStatusLoaded] = useState(false);
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isMounted = useRef(true);

  // 컴포넌트 마운트/언마운트 추적
  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  // 초기 스크랩 상태 조회 (로그인 상태에서만)
  useEffect(() => {
    const fetchScrapStatus = async () => {
      // 로그인 상태가 아니면 API 호출하지 않음
      if (!user) {
        setIsScraped(false);
        setStatusLoaded(true);
        return;
      }

      try {
        console.log('=== 스크랩 상태 조회 ===', { jobId, user });
        const response = await getScrapStatus(jobId);
        console.log('=== 스크랩 상태 응답 ===', response);
        if (isMounted.current) {
          setIsScraped(response?.scraped ?? false);
          setStatusLoaded(true);
        }
      } catch (err) {
        // 401 에러는 비로그인 상태 - 무시
        if (isMounted.current) {
          setIsScraped(false);
          setStatusLoaded(true);
        }
        console.error('스크랩 상태 조회 실패:', err.response?.data || err.message);
      }
    };

    if (jobId) {
      setStatusLoaded(false);
      fetchScrapStatus();
    }
  }, [jobId, user]);

  const handleClick = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    // 로그인 확인
    if (!user) {
      alert('로그인이 필요합니다.');
      return;
    }

    if (loading) return;

    setLoading(true);
    try {
      console.log('=== 스크랩 토글 요청 ===', { jobId, user });
      const response = await toggleScrap(jobId);
      console.log('=== 스크랩 토글 응답 ===', response);
      
      const { scraped, message, error } = response;
      
      if (error) {
        console.error('스크랩 토글 에러 응답:', response);
        alert(message || '스크랩 처리에 실패했습니다.');
        return;
      }
      
      if (isMounted.current) {
        setIsScraped(scraped);
      }
      
      // 마이페이지 관련 쿼리 무효화하여 데이터 갱신
      queryClient.invalidateQueries({ queryKey: ["profileSummary"] });
      queryClient.invalidateQueries({ queryKey: ["scrapedJobs"] });
      queryClient.invalidateQueries({ queryKey: ["myScrapedJobs"] });
      queryClient.invalidateQueries({ queryKey: ["publicJobs"] });
      queryClient.invalidateQueries({ queryKey: ["publicJob"] });
      
      if (onToggle) {
        onToggle(scraped, message);
      }
    } catch (err) {
      console.error('=== 스크랩 토글 예외 ===', err);
      console.error('에러 상세:', err.response?.data);
      if (err.response?.status === 401) {
        alert('로그인이 필요합니다.');
      } else if (err.response?.status === 403 || err.response?.data?.message?.includes('다시 로그인')) {
        alert('세션이 만료되었습니다. 다시 로그인해주세요.');
      } else {
        alert(err.response?.data?.message || '스크랩 처리에 실패했습니다.');
      }
    } finally {
      if (isMounted.current) {
        setLoading(false);
      }
    }
  };

  const sizeClass = size === 'sm' ? 'btn-sm' : size === 'lg' ? 'btn-lg' : '';
  const iconSize = size === 'sm' ? '' : size === 'lg' ? 'fs-4' : 'fs-5';

  return (
    <button
      className={`btn ${isScraped ? 'btn-danger' : 'btn-outline-secondary'} ${sizeClass}`}
      onClick={handleClick}
      disabled={loading}
      title={isScraped ? '스크랩 취소' : '스크랩'}
    >
      {loading ? (
        <span className="spinner-border spinner-border-sm"></span>
      ) : (
        <>
          <i className={`bi ${isScraped ? 'bi-heart-fill' : 'bi-heart'} ${iconSize}`}></i>
          {showText && <span className="ms-1">{isScraped ? '스크랩됨' : '스크랩'}</span>}
        </>
      )}
    </button>
  );
};

export default ScrapButton;
