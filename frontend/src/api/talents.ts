import { http } from "./http";

export interface TalentListItem {
  id: number;           // member_id (구직자 PK)
  resumeId?: number;    // 이력서 PK
  name: string;
  title: string;
  summary?: string;
  experience: string;
  location: string;
  education?: string;
  skills: string[];
  salary: string;
  matchScore: number;
  lastUpdated?: string;
  isNew?: boolean;
  avatar?: string | null;
}

export interface TalentDetail extends TalentListItem {
  email?: string;
  phone?: string;
}

export interface TalentPageResponse {
  content: TalentListItem[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

/** 인재풀 목록 조회 (매칭율 지원) */
export async function getTalents(
  page = 0, 
  size = 12
): Promise<TalentPageResponse> {
  const res = await http.get<TalentPageResponse>("/api/talents", {
    params: { page, size },
  });
  return res.data;
}

/** 인재 상세 조회 */
export async function getTalentDetail(memberId: number): Promise<TalentDetail | null> {
  try {
    const res = await http.get<TalentDetail>(`/api/talents/${memberId}`);
    return res.data;
  } catch (e: unknown) {
    const err = e as { response?: { status?: number } };
    if (err?.response?.status === 404) return null;
    throw e;
  }
}
