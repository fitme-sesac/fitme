import { http } from "./http";

/**
 * 채용공고 API 서비스
 * - 공개 채용공고 조회 (비로그인/로그인 모두 가능)
 * - 로그인 사용자는 기술 스택 매칭 정보 포함
 */

/**
 * 공개 채용공고 목록 조회
 * @param {Object} params - 검색/필터 파라미터
 * @param {number} params.page - 페이지 번호 (0부터 시작)
 * @param {number} params.size - 페이지 크기
 * @param {string} params.keyword - 검색 키워드
 * @param {string} params.stack - 기술 스택 필터
 * @param {string} params.location - 지역 필터
 * @param {number} params.minExperience - 최소 경력 (년)
 * @param {number} params.maxExperience - 최대 경력 (년)
 * @returns {Promise<JobListResponse>}
 */
export async function getPublicJobs({
  page = 0,
  size = 12,
  keyword = "",
  stack = "",
  location = "",
  minExperience = null,
  maxExperience = null,
} = {}) {
  const params = new URLSearchParams();
  params.append("page", page);
  params.append("size", size);
  if (keyword) params.append("keyword", keyword);
  if (stack) params.append("stack", stack);
  if (location) params.append("location", location);
  if (minExperience !== null && minExperience > 0) params.append("minExperience", minExperience);
  if (maxExperience !== null && maxExperience < 10) params.append("maxExperience", maxExperience);

  const response = await http.get(`/api/public/jobs?${params.toString()}`);
  return response.data;
}

/**
 * 공개 채용공고 상세 조회
 * @param {number} jobId - 채용공고 ID
 * @returns {Promise<JobDTO>}
 */
export async function getPublicJob(jobId) {
  const response = await http.get(`/api/public/jobs/${jobId}`);
  return response.data;
}

/**
 * 필터 옵션 조회 (기술 스택, 지역 목록)
 * @returns {Promise<FilterOptions>}
 */
export async function getFilterOptions() {
  const response = await http.get("/api/public/jobs/filter-options");
  return response.data;
}

/**
 * 채용공고 스크랩 토글 (로그인 필요)
 * - 스크랩 추가/취소를 한 번에 처리
 * @param {number} jobId - 채용공고 ID
 * @returns {Promise<{scraped: boolean, message: string}>}
 */
export async function scrapJob(jobId) {
  const response = await http.post(`/api/v1/jobs/${jobId}/scrap`);
  return response.data;
}

/**
 * 채용공고 스크랩 취소 (로그인 필요)
 * - 토글 API 사용 (POST로 다시 호출하면 취소됨)
 * @param {number} jobId - 채용공고 ID
 * @returns {Promise<{scraped: boolean, message: string}>}
 */
export async function unscrapJob(jobId) {
  // 백엔드가 토글 방식이므로 동일한 POST 엔드포인트 사용
  const response = await http.post(`/api/v1/jobs/${jobId}/scrap`);
  return response.data;
}

/**
 * 채용공고 지원 (로그인 필요)
 * @param {number} jobId - 채용공고 ID
 * @param {Object} application - 지원 정보
 * @param {number} application.resumeId - 이력서 ID
 * @param {Object} application.answers - 질문 답변
 * @returns {Promise<ApplicationDTO>}
 */
export async function applyToJob(jobId, application) {
  // 지원 API는 /api/v1/applications 사용
  const response = await http.post(`/api/v1/applications`, {
    jobId,
    resumeId: application.resumeId,
    answers: application.answers
  });
  return response.data;
}

/**
 * 스크랩한 공고 목록 조회 (로그인 필요)
 * ERD: job_scrap JOIN job_posting
 * @param {Object} params - 페이징 파라미터
 * @param {number} params.page - 페이지 번호 (0부터 시작)
 * @param {number} params.size - 페이지 크기
 * @returns {Promise<Page<JobDTO>>}
 */
export async function getMyScrapedJobs({ page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();
  params.append("page", page);
  params.append("size", size);
  const response = await http.get(`/api/v1/jobs/scraps?${params.toString()}`);
  return response.data;
}

/**
 * 스크랩 여부 확인
 * @param {number} jobId - 채용공고 ID
 * @returns {Promise<{scraped: boolean}>}
 */
export async function checkScrapStatus(jobId) {
  const response = await http.get(`/api/v1/jobs/${jobId}/scrap-status`);
  return response.data;
}

