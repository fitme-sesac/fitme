import { http } from "./http";

/**
 * 이력서 API 서비스
 * - 이력서 CRUD
 * - AI 요약 기능
 */

/**
 * 내 이력서 목록 조회 (로그인 필요)
 * @returns {Promise<ResumeListResponse>}
 */
export async function getMyResumes() {
  const response = await http.get("/api/v1/resumes");
  return response.data;
}

/**
 * 이력서 상세 조회 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @returns {Promise<ResumeDTO>}
 */
export async function getResume(resumeId) {
  const response = await http.get(`/api/v1/resumes/${resumeId}`);
  return response.data;
}

/**
 * 이력서 생성 (로그인 필요)
 * @param {Object} resume - 이력서 정보
 * @returns {Promise<ResumeDTO>}
 */
export async function createResume(resume) {
  const response = await http.post("/api/v1/resumes", resume);
  return response.data;
}

/**
 * 이력서 수정 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @param {Object} resume - 이력서 정보
 * @returns {Promise<ResumeDTO>}
 */
export async function updateResume(resumeId, resume) {
  const response = await http.patch(`/api/v1/resumes/${resumeId}`, resume);
  return response.data;
}

/**
 * 이력서 삭제 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @returns {Promise<void>}
 */
export async function deleteResume(resumeId) {
  const response = await http.delete(`/api/v1/resumes/${resumeId}`);
  return response.data;
}

/**
 * 이력서 복제 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @returns {Promise<number>} 새 이력서 ID
 */
export async function copyResume(resumeId) {
  const response = await http.post(`/api/v1/resumes/${resumeId}/copy`);
  return response.data;
}

/**
 * 대표 이력서 설정 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @returns {Promise<ResumeDTO>}
 */
export async function setPrimaryResume(resumeId) {
  const response = await http.patch(`/api/v1/resumes/${resumeId}/primary`);
  return response.data;
}

/**
 * 이력서 공개 설정 변경 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @param {boolean} isPublic - 공개 여부
 * @returns {Promise<ResumeDTO>}
 */
export async function setResumeVisibility(resumeId, isPublic) {
  const response = await http.patch(`/api/v1/resumes/${resumeId}/visibility`, {
    isPublic,
  });
  return response.data;
}

/**
 * AI 이력서 요약 요청 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @returns {Promise<{message: string}>}
 */
export async function requestResumeSummary(resumeId) {
  const response = await http.post(`/api/v1/resumes/${resumeId}/summarize`);
  return response.data;
}

/**
 * 이력서 파일 업로드 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @param {File} file - 파일 객체
 * @returns {Promise<AttachmentDTO>}
 */
export async function uploadResumeFile(resumeId, file) {
  const formData = new FormData();
  formData.append("file", file);
  const response = await http.post(
    `/api/v1/resumes/${resumeId}/attachments`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }
  );
  return response.data;
}

/**
 * 이력서 첨부파일 삭제 (로그인 필요)
 * @param {number} resumeId - 이력서 ID
 * @param {number} attachmentId - 첨부파일 ID
 * @returns {Promise<void>}
 */
export async function deleteResumeAttachment(resumeId, attachmentId) {
  const response = await http.delete(
    `/api/v1/resumes/${resumeId}/attachments/${attachmentId}`
  );
  return response.data;
}
/**
 * 내 프로필 요약 정보 조회 (마이페이지용)
 * @returns {Promise<Object>}
 */
export async function getMyProfileSummary() {
  const response = await http.get("/api/v1/resumes/my-profile-summary");
  return response.data;
}
