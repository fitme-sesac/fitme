import { http } from "./http";

/**
 * 지원 API 서비스
 * - ERD: job_application 테이블
 */

/**
 * 내 지원 현황 조회
 * @returns {Promise<Application[]>}
 * 
 * 응답 필드 (ERD job_application 기준):
 * - applicationId, jobId, resumeId, status
 * - appliedAt, viewedAt, contactDisclosedAt
 * - answers (JSONB)
 * - job (조인된 채용공고 정보)
 */
export async function getMyApplications() {
    const response = await http.get("/api/applications/my");
    return response.data;
}

/**
 * 지원하기
 * @param {number} jobId - 채용공고 ID
 * @param {number} resumeId - 이력서 ID
 * @param {Object} answers - 질문 답변 (optional)
 */
export async function applyToJob(jobId, resumeId, answers = null) {
    const response = await http.post("/api/applications", {
        jobId,
        resumeId,
        answers
    });
    return response.data;
}

/**
 * 지원 취소
 * @param {number} applicationId - 지원 ID
 */
export async function cancelApplication(applicationId) {
    const response = await http.post(`/api/applications/${applicationId}/cancel`);
    return response.data;
}

/**
 * 지원 상세 조회
 * @param {number} applicationId - 지원 ID
 */
export async function getApplication(applicationId) {
    const response = await http.get(`/api/applications/${applicationId}`);
    return response.data;
}

/**
 * 지원 상태별 카운트
 * @returns {Promise<{submitted: number, viewed: number, interview: number, hired: number, rejected: number}>}
 */
export async function getApplicationStatusCounts() {
    const response = await http.get("/api/applications/my/counts");
    return response.data;
}
