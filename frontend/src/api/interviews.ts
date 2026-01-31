import { http } from "./http";

/**
 * 면접 일정 API 서비스
 * Reference: InterviewController.java
 */

export interface InterviewDTO {
    interviewId: number;
    applicationId: number;

    // 면접 정보
    stage: "FIRST" | "SECOND" | "FINAL";
    method: "ONSITE" | "VIDEO" | "PHONE";
    location?: string;
    meetingUrl?: string;
    startAt: string; // ISO string
    endAt: string; // ISO string
    status: "PROPOSED" | "CONFIRMED" | "CANCELED" | "DONE";

    // 지원 정보
    jobId: number;
    jobTitle: string;
    companyName: string;

    // 지원자 정보
    candidateId: number;
    candidateName: string;
    resumeTitle: string;

    createdAt: string;
}

/**
 * 내 면접 일정 조회 (지원자용)
 * GET /api/v1/interviews/me
 */
export async function getMyInterviews() {
    const response = await http.get("/api/v1/interviews/me");
    return response.data;
}

/**
 * 다가오는 면접 조회 (지원자용)
 * GET /api/v1/interviews/me/upcoming
 */
export async function getUpcomingInterviews() {
    const response = await http.get("/api/v1/interviews/me/upcoming");
    return response.data;
}

/**
 * 면접 응답 (지원자용) - 수락/거절/일정변경요청
 * POST /api/v1/interviews/{interviewId}/respond
 */
export async function respondToInterview(interviewId: number, type: "ACCEPT" | "DECLINE" | "RESCHEDULE", message?: string) {
    const response = await http.post(`/api/v1/interviews/${interviewId}/respond`, {
        type,
        message
    });
    return response.data;
}

/**
 * 특정 지원의 면접 일정 조회
 * GET /api/v1/interviews/application/{applicationId}
 */
export async function getInterviewsByApplication(applicationId: number) {
    const response = await http.get(`/api/v1/interviews/application/${applicationId}`);
    return response.data;
}

/**
 * 면접 상세 조회
 * GET /api/v1/interviews/{interviewId}
 */
export async function getInterview(interviewId: number) {
    const response = await http.get(`/api/v1/interviews/${interviewId}`);
    return response.data;
}
