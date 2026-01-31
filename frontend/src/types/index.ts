/**
 * 타입 정의 - ERD_Table_0122.txt 기반
 */

// member 테이블
export interface User {
  memberId: number;
  memberUid: string;
  name: string;
  email: string;
  loginId?: string;
  role: 'CANDIDATE' | 'EMPLOYER' | 'SERVICEADMIN' | 'APPROVEADMIN' | 'MASTER';
  status: 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN';
  gender?: 'MALE' | 'FEMALE';
  birthDate?: string;
  phone?: string;
  authProvider: 'NAVER' | 'GOOGLE' | 'KAKAO' | 'OTHER';
  lastLoginAt?: string;
  phoneVerifiedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// job_posting 테이블
export interface Job {
  jobId: number;
  employerId: number;
  title: string;
  description: string;
  status: 'DRAFT' | 'OPEN' | 'CLOSED';
  location?: string;
  salaryText?: string;
  stack?: string[];  // TEXT[] 배열
  requiredExperience: number;  // 최소 경력 연수
  requiredQuestions?: Record<string, any>;  // JSONB
  viewCount: number;
  applyCount: number;
  recruitmentCapacity: number;
  adBidCredit: number;  // 광고 입찰가
  summary?: string;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
  // 조인된 employer 정보
  employer?: Employer;
}

// job_application 테이블
export interface Application {
  applicationId: number;
  jobId: number;
  memberId: number;
  resumeId: number;
  status: 'SUBMITTED' | 'VIEWED' | 'INTERVIEW' | 'HIRED' | 'REJECTED' | 'CANCELED';
  appliedAt: string;
  viewedAt?: string;
  canceledAt?: string;
  contactDisclosedAt?: string;
  answers?: Record<string, any>;  // JSONB
  createdAt: string;
  updatedAt: string;
  // 조인된 정보
  job?: Job;
  resume?: Resume;
}

// resume 테이블
export interface Resume {
  resumeId: number;
  memberId: number;
  title: string;
  tagline?: string;
  isPrimary: boolean;
  isPublic: boolean;
  status: 'DRAFT' | 'ACTIVE' | 'DELETED';
  field: 'RESUME' | 'PORTFOLIO' | 'INTRO';
  content?: string;
  summary?: string;
  summaryStatus: 'NONE' | 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  reStack?: string[];  // 기술 스택 배열
  careerYears: number;  // 경력 연수
  preferenceLocation?: string;
  preferenceSalary?: string;
  employmentType?: string;
  school?: string;
  schoolState?: string;
  schoolClass?: string;
  targetJobId?: number;
  lastModifiedAt: string;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

// employer 테이블
export interface Employer {
  employerId: number;
  employerUid: string;
  name: string;
  logoUrl?: string;
  industry?: string;
  foundedYear?: number;
  employeeCount?: number;
  location?: string;
  description?: string;
  culture?: string;
  benefits?: string;
  techStack?: string;
  contactEmail?: string;
  contactPhone?: string;
  websiteUrl?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

// interview_schedule 테이블
export interface InterviewSchedule {
  interviewId: number;
  applicationId: number;
  stage: '1ST' | '2ND' | 'FINAL';
  method: 'ONSITE' | 'VIDEO' | 'PHONE';
  location?: string;
  meetingUrl?: string;
  startAt: string;
  endAt: string;
  status: 'PROPOSED' | 'CONFIRMED' | 'CANCELED' | 'DONE';
  createdByMemberId?: number;
  createdAt: string;
  updatedAt: string;
}

// job_scrap 테이블
export interface JobScrap {
  scrapId: number;
  memberId: number;
  jobId: number;
  createdAt: string;
  // 조인된 정보
  job?: Job;
}

// resume_career 테이블
export interface ResumeCareer {
  careerId: number;
  resumeId: number;
  companyName: string;
  department: string;
  role: string;
  startDate: string;
  endDate?: string;
  isCurrent?: boolean;
  isVerified?: boolean;
  createdAt: string;
  updatedAt: string;
}

// resume_project 테이블
export interface ResumeProject {
  projectId: number;
  resumeId: number;
  title: string;
  startDate?: string;
  endDate?: string;
  contributionPct?: number;
  techStack?: string[];
  description?: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

// resume_certificate 테이블
export interface ResumeCertificate {
  resumeCertificateId: number;
  resumeId: number;
  name: string;
  issuer: string;
  acquisitionDate: string;
  isVerified?: boolean;
  createdAt: string;
  updatedAt: string;
}

// API 응답 타입
export interface AuthStatusResponse {
  authenticated: boolean;
  name?: string;
  role?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;  // current page
  size: number;
  first: boolean;
  last: boolean;
}

// 지원 상태 한글 매핑
export const APPLICATION_STATUS_LABELS: Record<Application['status'], string> = {
  SUBMITTED: '서류심사중',
  VIEWED: '열람됨',
  INTERVIEW: '면접예정',
  HIRED: '합격',
  REJECTED: '불합격',
  CANCELED: '지원취소',
};

// 면접 단계 한글 매핑
export const INTERVIEW_STAGE_LABELS: Record<InterviewSchedule['stage'], string> = {
  '1ST': '1차 면접',
  '2ND': '2차 면접',
  'FINAL': '최종 면접',
};

// 면접 방식 한글 매핑
export const INTERVIEW_METHOD_LABELS: Record<InterviewSchedule['method'], string> = {
  ONSITE: '대면',
  VIDEO: '화상',
  PHONE: '전화',
};
