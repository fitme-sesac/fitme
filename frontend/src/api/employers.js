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
 * @returns {Promise<AdStatsDTO>}
 */
export async function getAdStats() {
  const response = await http.get("/api/employer/ad-stats");
  return response.data;
}
