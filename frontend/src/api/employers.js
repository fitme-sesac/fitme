import { http } from "./http";

/**
 * 기업 API 서비스
 * - 공개 기업 정보 조회
 * - 기업 회원용 대시보드 및 관리 API
 */

/**
 * 공개 기업 목록 조회 (채용 중인 기업)
 * @param {Object} params - 검색/필터 파라미터
 * @param {number} params.page - 페이지 번호
 * @param {number} params.size - 페이지 크기
 * @param {string} params.industry - 업종 필터
 * @returns {Promise<EmployerListResponse>}
 */
export async function getPublicEmployers({
  page = 0,
  size = 10,
  industry = "",
} = {}) {
  const params = new URLSearchParams();
  params.append("page", page);
  params.append("size", size);
  if (industry) params.append("industry", industry);

  const response = await http.get(`/api/public/employers?${params.toString()}`);
  return response.data;
}

/**
 * 공개 기업 상세 조회
 * @param {number} employerId - 기업 ID
 * @returns {Promise<EmployerDTO>}
 */
export async function getPublicEmployer(employerId) {
  const response = await http.get(`/api/public/employers/${employerId}`);
  return response.data;
}

/**
 * 기업 대시보드 조회 (로그인 필요)
 * @returns {Promise<DashboardDTO>}
 */
export async function getEmployerDashboard() {
  const response = await http.get("/api/employer/dashboard");
  return response.data;
}

/**
 * 기업 프로필 조회 (로그인 필요)
 * @returns {Promise<EmployerProfileDTO>}
 */
export async function getEmployerProfile() {
  const response = await http.get("/api/employer/profile");
  return response.data;
}

/**
 * 기업 프로필 저장 (로그인 필요)
 * @param {Object} profile - 기업 프로필 정보
 * @returns {Promise<EmployerProfileDTO>}
 */
export async function saveEmployerProfile(profile) {
  const response = await http.post("/api/employer/profile", profile);
  return response.data;
}

/**
 * 지원자 목록 조회 (로그인 필요)
 * @param {string} status - 상태 필터 (SUBMITTED, VIEWED, INTERVIEW, HIRED, REJECTED, CANCELED)
 * @returns {Promise<ApplicantListDTO>}
 */
export async function getApplicants(status = "") {
  const params = status ? `?status=${status}` : "";
  const response = await http.get(`/api/employer/applicants${params}`);
  return response.data;
}

/**
 * 특정 채용공고의 지원자 목록 조회 (로그인 필요)
 * @param {number} jobId - 채용공고 ID
 * @param {string} status - 상태 필터
 * @returns {Promise<ApplicantListDTO>}
 */
export async function getApplicantsByJob(jobId, status = "") {
  const params = status ? `?status=${status}` : "";
  const response = await http.get(`/api/employer/jobs/${jobId}/applicants${params}`);
  return response.data;
}

/**
 * 지원자 상태 변경 (로그인 필요)
 * @param {number} applicationId - 지원 ID
 * @param {string} status - 새 상태
 * @returns {Promise<void>}
 */
export async function updateApplicantStatus(applicationId, status) {
  const response = await http.patch(
    `/api/employer/applicants/${applicationId}/status`,
    { status }
  );
  return response.data;
}

/**
 * 면접 일정 목록 조회 (로그인 필요)
 * @param {number} year - 연도
 * @param {number} month - 월
 * @returns {Promise<InterviewListDTO>}
 */
export async function getInterviews(year, month) {
  const params = new URLSearchParams();
  if (year) params.append("year", year);
  if (month) params.append("month", month);
  const query = params.toString() ? `?${params.toString()}` : "";
  const response = await http.get(`/api/employer/interviews${query}`);
  return response.data;
}

/**
 * 면접 일정 생성 (로그인 필요)
 * @param {Object} interview - 면접 일정 정보
 * @returns {Promise<InterviewDTO>}
 */
export async function createInterview(interview) {
  const response = await http.post("/api/employer/interviews", interview);
  return response.data;
}

/**
 * 면접 일정 수정 (로그인 필요)
 * @param {number} interviewId - 면접 일정 ID
 * @param {Object} interview - 면접 일정 정보
 * @returns {Promise<InterviewDTO>}
 */
export async function updateInterview(interviewId, interview) {
  const response = await http.put(
    `/api/employer/interviews/${interviewId}`,
    interview
  );
  return response.data;
}

/**
 * 면접 일정 삭제 (로그인 필요)
 * @param {number} interviewId - 면접 일정 ID
 * @returns {Promise<void>}
 */
export async function deleteInterview(interviewId) {
  const response = await http.delete(`/api/employer/interviews/${interviewId}`);
  return response.data;
}

/**
 * 광고 통계 조회 (로그인 필요)
 * @param {boolean} demo - 데모 모드 (true: 실제 클릭이 없을 때 데모 데이터 표시, false: 실제 데이터만)
 * @returns {Promise<AdStatsDTO>}
 */
export async function getAdStats(demo = true) {
  const response = await http.get(`/api/employer/ad-stats?demo=${demo}`);
  return response.data;
}

/**
 * 광고 캠페인 생성 (로그인 필요)
 * @param {Object} campaign - 캠페인 정보
 * @param {number} campaign.employerId - 기업 ID
 * @param {number} campaign.jobId - 채용공고 ID
 * @param {number} campaign.cpcBid - CPC 입찰가 (원)
 * @param {number} campaign.dailyBudget - 일일 예산 (원)
 * @param {string} campaign.startDate - 시작일 (YYYY-MM-DD)
 * @param {string} campaign.endDate - 종료일 (YYYY-MM-DD)
 * @returns {Promise<AdCampaignResponseDTO>}
 */
export async function createAdCampaign(campaign) {
  const response = await http.post("/api/v1/ad/campaigns", campaign);
  return response.data;
}

/**
 * 광고 캠페인 상태 변경 (로그인 필요)
 * @param {number} campaignId - 캠페인 ID
 * @param {string} status - 새 상태 (ACTIVE, PAUSED, ENDED)
 * @returns {Promise<AdCampaignResponseDTO>}
 */
export async function updateAdCampaignStatus(campaignId, status) {
  const response = await http.patch(`/api/v1/ad/campaigns/${campaignId}/status?status=${status}`);
  return response.data;
}

/**
 * 광고 캠페인 수정 (로그인 필요, 기업 소유권 검증)
 * @param {number} campaignId - 캠페인 ID
 * @param {Object} updates - 수정할 필드 (null인 필드는 수정하지 않음)
 * @param {number} updates.cpcBid - CPC 입찰가 (원)
 * @param {number} updates.dailyBudget - 일일 예산 (원)
 * @param {string} updates.startDate - 시작일 (YYYY-MM-DD)
 * @param {string} updates.endDate - 종료일 (YYYY-MM-DD)
 * @returns {Promise<{message: string}>}
 */
export async function updateAdCampaign(campaignId, updates) {
  const response = await http.patch(`/api/employer/ad-campaigns/${campaignId}`, updates);
  return response.data;
}

/**
 * 기업 채용공고 목록 조회 (로그인 필요)
 * @param {Object} params - 페이징 파라미터
 * @param {number} params.page - 페이지 번호 (0부터 시작)
 * @param {number} params.size - 페이지 크기
 * @returns {Promise<JobListResponseDTO>}
 */
export async function getMyJobPostings({ page = 0, size = 50 } = {}) {
  const response = await http.get(`/api/jobs?page=${page}&size=${size}`);
  return response.data;
}
