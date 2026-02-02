// frontend/src/api/proposal.ts
import axios from "axios";

export interface ProposalCreateRequest {
    candidateId: number;
    jobId?: number;
    title: string;
    message?: string;
    offeredSalary?: string;
    offeredPosition?: string;
    expirationDays?: number;
}

export interface ProposalRespondRequest {
    accept: boolean;
    message?: string;
}

export interface ProposalResponse {
    proposalId: number;
    status: 'PENDING' | 'VIEWED' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELED';

    // 기업 정보
    employerId: number;
    employerName: string;
    employerLogo?: string;

    // 구직자 정보
    candidateId: number;
    candidateName: string;
    candidateEmail?: string;

    // 공고 정보
    jobId?: number;
    jobTitle?: string;

    // 제안 내용
    title: string;
    message?: string;
    offeredSalary?: string;
    offeredPosition?: string;

    // 시간 정보
    createdAt: string;
    expiresAt?: string;
    viewedAt?: string;
    respondedAt?: string;
    responseMessage?: string;
}

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}

// 기업용: 제안 보내기
export async function createProposal(request: ProposalCreateRequest): Promise<ProposalResponse> {
    const res = await axios.post("/api/proposals", request);
    return res.data;
}

// 기업용: 보낸 제안 목록
export async function getSentProposals(page = 0, size = 10): Promise<PageResponse<ProposalResponse>> {
    const res = await axios.get("/api/proposals/sent", {
        params: { page, size }
    });
    return res.data;
}

// 기업용: 제안 취소
export async function cancelProposal(proposalId: number): Promise<void> {
    await axios.delete(`/api/proposals/${proposalId}`);
}

// 구직자용: 받은 제안 목록
export async function getReceivedProposals(page = 0, size = 10): Promise<PageResponse<ProposalResponse>> {
    const res = await axios.get("/api/proposals/received", {
        params: { page, size }
    });
    return res.data;
}

// 구직자용: 제안 상세 조회
export async function getProposal(proposalId: number): Promise<ProposalResponse> {
    const res = await axios.get(`/api/proposals/${proposalId}`);
    return res.data;
}

// 구직자용: 제안에 응답
export async function respondToProposal(proposalId: number, request: ProposalRespondRequest): Promise<ProposalResponse> {
    const res = await axios.post(`/api/proposals/${proposalId}/respond`, request);
    return res.data;
}

// 구직자용: 미확인 제안 수
export async function getPendingProposalCount(): Promise<{ count: number }> {
    const res = await axios.get("/api/proposals/pending-count");
    return res.data;
}
