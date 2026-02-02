import { http } from "./http";

/**
 * 지원 API 서비스
 * - ERD: job_application 테이블
 */

/**
 * 내 지원 현황 조회 (백엔드: GET /api/v1/applications/me)
 * @returns {Promise<Application[]>}
 */
export async function getMyApplications() {
    const response = await http.get("/api/v1/applications/me");
    return response.data;
}

/**
 * 지원하기 (백엔드: POST /api/v1/applications)
 * @param {number} jobId - 채용공고 ID
 * @param {number} resumeId - 이력서 ID
 * @param {Object} answers - 질문 답변 (optional)
 */
export async function applyToJob(jobId, resumeId, answers = null) {
    const response = await http.post("/api/v1/applications", {
        jobId,
        resumeId,
        answers
    });
    return response.data;
}

/**
 * 지원 취소 (백엔드: DELETE /api/v1/applications/:applicationId)
 * @param {number} applicationId - 지원 ID
 */
export async function cancelApplication(applicationId) {
    const response = await http.delete(`/api/v1/applications/${applicationId}`);
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
