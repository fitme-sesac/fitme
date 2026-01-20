import { useState, useEffect, useCallback } from 'react';
import {
  getJobPostings,
  getJobPosting,
  createJobPosting,
  updateJobPosting,
  deleteJobPosting,
  updateJobStatus,
  getApplicationsByJob,
  getApplication,
  updateApplicationStatus,
} from '../api/jobApi';

/**
 * 채용공고 목록을 가져오는 훅
 */
export function useJobPostings(initialParams = {}) {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [params, setParams] = useState({
    page: 0,
    size: 10,
    status: null,
    ...initialParams,
  });

  const fetchJobs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getJobPostings(params);
      
      // 페이지네이션 응답 형식에 따라 처리
      if (data.content) {
        setJobs(data.content);
        setPagination({
          page: data.number || 0,
          size: data.size || 10,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0,
        });
      } else {
        setJobs(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      setError(err.response?.data?.message || '채용공고를 불러오는데 실패했습니다.');
      setJobs([]);
    } finally {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => {
    fetchJobs();
  }, [fetchJobs]);

  const changePage = (newPage) => {
    setParams((prev) => ({ ...prev, page: newPage }));
  };

  const changeFilter = (filterParams) => {
    setParams((prev) => ({ ...prev, ...filterParams, page: 0 }));
  };

  return {
    jobs,
    loading,
    error,
    pagination,
    params,
    changePage,
    changeFilter,
    refetch: fetchJobs,
  };
}

/**
 * 단일 채용공고를 가져오는 훅
 */
export function useJobPosting(jobId) {
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchJob = useCallback(async () => {
    if (!jobId) {
      setJob(null);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getJobPosting(jobId);
      setJob(data);
    } catch (err) {
      setError(err.response?.data?.message || '채용공고를 불러오는데 실패했습니다.');
      setJob(null);
    } finally {
      setLoading(false);
    }
  }, [jobId]);

  useEffect(() => {
    fetchJob();
  }, [fetchJob]);

  return { job, loading, error, refetch: fetchJob };
}

/**
 * 채용공고 CRUD 작업을 위한 훅
 */
export function useJobMutation() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const create = async (data) => {
    try {
      setLoading(true);
      setError(null);
      const result = await createJobPosting(data);
      return result;
    } catch (err) {
      setError(err.response?.data?.message || '채용공고 생성에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const update = async (jobId, data) => {
    try {
      setLoading(true);
      setError(null);
      const result = await updateJobPosting(jobId, data);
      return result;
    } catch (err) {
      setError(err.response?.data?.message || '채용공고 수정에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const remove = async (jobId) => {
    try {
      setLoading(true);
      setError(null);
      await deleteJobPosting(jobId);
    } catch (err) {
      setError(err.response?.data?.message || '채용공고 삭제에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const changeStatus = async (jobId, status) => {
    try {
      setLoading(true);
      setError(null);
      const result = await updateJobStatus(jobId, status);
      return result;
    } catch (err) {
      setError(err.response?.data?.message || '상태 변경에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { create, update, remove, changeStatus, loading, error };
}

/**
 * 특정 채용공고의 지원자 목록을 가져오는 훅
 */
export function useApplicationsByJob(jobId, initialParams = {}) {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  });
  const [params, setParams] = useState({
    page: 0,
    size: 10,
    status: null,
    ...initialParams,
  });

  const fetchApplications = useCallback(async () => {
    if (!jobId) {
      setApplications([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getApplicationsByJob(jobId, params);
      
      if (data.content) {
        setApplications(data.content);
        setPagination({
          page: data.number || 0,
          size: data.size || 10,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0,
        });
      } else {
        setApplications(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      setError(err.response?.data?.message || '지원자 목록을 불러오는데 실패했습니다.');
      setApplications([]);
    } finally {
      setLoading(false);
    }
  }, [jobId, params]);

  useEffect(() => {
    fetchApplications();
  }, [fetchApplications]);

  const changePage = (newPage) => {
    setParams((prev) => ({ ...prev, page: newPage }));
  };

  const changeFilter = (filterParams) => {
    setParams((prev) => ({ ...prev, ...filterParams, page: 0 }));
  };

  return {
    applications,
    loading,
    error,
    pagination,
    params,
    changePage,
    changeFilter,
    refetch: fetchApplications,
  };
}

/**
 * 단일 지원 정보를 가져오는 훅
 */
export function useApplication(applicationId) {
  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchApplication = useCallback(async () => {
    if (!applicationId) {
      setApplication(null);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await getApplication(applicationId);
      setApplication(data);
    } catch (err) {
      setError(err.response?.data?.message || '지원 정보를 불러오는데 실패했습니다.');
      setApplication(null);
    } finally {
      setLoading(false);
    }
  }, [applicationId]);

  useEffect(() => {
    fetchApplication();
  }, [fetchApplication]);

  return { application, loading, error, refetch: fetchApplication };
}

/**
 * 지원 상태 변경을 위한 훅
 */
export function useApplicationMutation() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const changeStatus = async (applicationId, status) => {
    try {
      setLoading(true);
      setError(null);
      const result = await updateApplicationStatus(applicationId, status);
      return result;
    } catch (err) {
      setError(err.response?.data?.message || '상태 변경에 실패했습니다.');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { changeStatus, loading, error };
}

export default {
  useJobPostings,
  useJobPosting,
  useJobMutation,
  useApplicationsByJob,
  useApplication,
  useApplicationMutation,
};
