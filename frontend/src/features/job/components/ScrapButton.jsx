import React, { useState, useEffect } from 'react';
import { toggleScrap, getScrapStatus } from '../api/jobApi';

/**
 * 스크랩 버튼 컴포넌트
 */
const ScrapButton = ({ jobId, initialScraped = false, onToggle, size = 'md', showText = false }) => {
  const [isScraped, setIsScraped] = useState(initialScraped);
  const [loading, setLoading] = useState(false);

  // 초기 스크랩 상태 조회
  useEffect(() => {
    const fetchScrapStatus = async () => {
      try {
        const { scraped } = await getScrapStatus(jobId);
        setIsScraped(scraped);
      } catch (err) {
        // 비로그인 사용자는 에러 무시
        console.debug('스크랩 상태 조회 실패:', err);
      }
    };

    if (jobId) {
      fetchScrapStatus();
    }
  }, [jobId]);

  const handleClick = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (loading) return;

    setLoading(true);
    try {
      const { scraped, message } = await toggleScrap(jobId);
      setIsScraped(scraped);
      if (onToggle) {
        onToggle(scraped, message);
      }
    } catch (err) {
      console.error('스크랩 토글 실패:', err);
      if (err.response?.status === 401) {
        alert('로그인이 필요합니다.');
      } else {
        alert('스크랩 처리에 실패했습니다.');
      }
    } finally {
      setLoading(false);
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
