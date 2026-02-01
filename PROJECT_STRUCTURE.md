# FitMe 프로젝트 구조 및 연결 현황

## 프로젝트 개요
FitMe는 구직자와 기업을 연결하는 AI 기반 채용 플랫폼입니다.

---

## 디렉토리 구조

```
fitme/
├── frontend/                    # React + Vite 프론트엔드
│   └── src/
│       ├── api/                 # API 클라이언트
│       ├── components/          # 공통 컴포넌트
│       ├── contexts/            # React Context (Auth 등)
│       ├── features/            # 도메인별 기능 모듈
│       ├── hooks/               # Custom Hooks
│       └── pages/               # 페이지 컴포넌트
│           ├── common/          # 공통 페이지
│           ├── guest/           # 비회원 페이지
│           ├── jobseeker/       # 구직자 전용
│           ├── company/         # 기업 전용
│           ├── admin/           # 관리자 전용
│           └── payment/         # 결제 관련
├── backend/                     # Spring Boot 백엔드
│   └── src/main/java/com/example/pproject/
│       ├── application/         # 지원 관련
│       ├── auth/                # 인증
│       ├── employer/            # 기업 관리
│       ├── job/                 # 채용공고
│       ├── notification/        # 알림
│       ├── proposal/            # 인재 제안 (NEW)
│       ├── resume/              # 이력서
│       └── user/                # 사용자
└── ai-worker/                   # FastAPI AI 서버
    └── app/
        ├── chatbot/             # 구직자용 AI 챗봇
        ├── employer_chatbot/    # 기업용 AI 챗봇 (NEW)
        ├── resumes/             # 이력서 분석
        └── jobpostings/         # 공고 분석
```

---

## 라우트 현황

### 공통 페이지
| 경로 | 컴포넌트 | 설명 |
|------|----------|------|
| `/` | Index | 홈페이지 |
| `/jobs` | Jobs | 채용공고 목록 |
| `/jobs/:jobId` | JobDetail | 채용공고 상세 |
| `/companies` | CompanyManagement | 기업 목록 |
| `/companies/:companyId` | CompanyDetail | 기업 상세 |
| `/community` | Community | 커뮤니티 |
| `/support` | Support | 고객센터 |
| `/settings` | Settings | 설정 (로그인 필요) |

### 인증 페이지
| 경로 | 컴포넌트 | 설명 |
|------|----------|------|
| `/auth` | Auth | 로그인/회원가입 |
| `/auth/find-id` | FindUserIdPage | 아이디 찾기 |
| `/auth/find-password` | FindPasswordPage | 비밀번호 찾기 |

### 구직자 전용
| 경로 | 컴포넌트 | 설명 | 상태 |
|------|----------|------|------|
| `/interview` | Interview | 면접 일정 관리 | OK |
| `/resume` | Resume | 이력서 관리 | OK |
| `/mypage` | MyPage | 마이페이지 | OK |
| `/proposals` | Proposals | 받은 포지션 제안 | NEW |
| `/applications` | Applications | 입사지원 현황 | NEW |

### 기업 전용
| 경로 | 컴포넌트 | 설명 | 상태 |
|------|----------|------|------|
| `/company/dashboard` | CompanyDashboard | 기업 대시보드 | OK |
| `/talents` | Talents | 인재풀 | OK |
| `/talents/:talentId` | TalentDetail | 인재 상세 + 제안 | NEW |
| `/employer/jobs` | JobListPage | 채용공고 관리 | OK |
| `/employer/jobs/create` | JobCreatePage | 공고 등록 | OK |
| `/employer/jobs/:jobId` | JobDetailPage | 공고 상세 | OK |
| `/employer/jobs/:jobId/edit` | JobEditPage | 공고 수정 | OK |

---

## API 엔드포인트

### 백엔드 (Spring Boot - localhost:8080)

#### 인재 제안 API (NEW)
| Method | 경로 | 설명 |
|--------|------|------|
| POST | `/api/proposals` | 제안 생성 (기업용) |
| GET | `/api/proposals/sent` | 보낸 제안 목록 (기업용) |
| DELETE | `/api/proposals/{id}` | 제안 취소 (기업용) |
| GET | `/api/proposals/received` | 받은 제안 목록 (구직자용) |
| GET | `/api/proposals/{id}` | 제안 상세 (구직자용) |
| POST | `/api/proposals/{id}/respond` | 제안 응답 (구직자용) |
| GET | `/api/proposals/pending-count` | 미확인 제안 수 |

#### 지원 API
| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/applications/me` | 내 지원 현황 |
| POST | `/api/v1/applications` | 지원하기 |
| DELETE | `/api/v1/applications/{id}` | 지원 취소 |

#### 알림 API
| Method | 경로 | 설명 |
|--------|------|------|
| GET | `/api/notifications` | 알림 목록 |
| GET | `/api/notifications/recent` | 최근 알림 |
| GET | `/api/notifications/unread-count` | 읽지 않은 수 |
| PATCH | `/api/notifications/{id}/read` | 읽음 처리 |
| PATCH | `/api/notifications/read-all` | 전체 읽음 |

### AI 서버 (FastAPI - localhost:8000)

#### 구직자용 챗봇
| Method | 경로 | 설명 |
|--------|------|------|
| POST | `/chatbot/query` | 채용 정보 질의 |

#### 기업용 챗봇 (NEW)
| Method | 경로 | 설명 |
|--------|------|------|
| POST | `/employer-chatbot/query` | 지원자 현황 질의 |

---

## 새로 추가된 기능

### 1. 인재 제안 기능 (Proposal)

**백엔드 파일:**
```
backend/src/main/java/com/example/pproject/
├── Constant/ProposalStatus.java
└── proposal/
    ├── controller/TalentProposalController.java
    ├── dto/
    │   ├── ProposalCreateRequest.java
    │   ├── ProposalRespondRequest.java
    │   └── ProposalResponse.java
    ├── entity/TalentProposal.java
    ├── repository/TalentProposalRepository.java
    └── service/TalentProposalService.java
```

**프론트엔드 파일:**
```
frontend/src/
├── api/proposal.ts
├── pages/company/TalentDetail.tsx
└── pages/jobseeker/Proposals.tsx
```

**흐름:**
1. 기업회원이 `/talents`에서 인재 클릭
2. `/talents/:talentId`에서 "제안하기" 버튼 클릭
3. 제안 모달에서 제목, 메시지, 연봉 등 입력 후 전송
4. 구직자에게 알림 발송
5. 구직자가 `/proposals`에서 제안 확인 및 수락/거절

### 2. 기업용 AI 챗봇

**백엔드 파일:**
```
ai-worker/app/employer_chatbot/
├── __init__.py
├── config.py
├── router.py
├── schemas.py
├── repository.py
└── services/
    ├── agent.py
    └── memory.py
```

**프론트엔드 파일:**
```
frontend/src/
├── api/employer-chatbot.js
└── components/chat/EmployerAIChatWidget.tsx
```

**지원 질문:**
- 오늘 지원자 몇명이야?
- 이번달 지원 현황 보여줘
- 백엔드 공고 지원자 목록
- 서류 통과한 지원자는?
- 우리 공고 성과 분석해줘
- 지원자들이 많이 보유한 스킬은?

### 3. 입사지원 현황 페이지

**프론트엔드 파일:**
```
frontend/src/pages/jobseeker/Applications.tsx
```

**기능:**
- 지원한 공고 목록 조회
- 상태별 필터 (진행중/완료)
- 지원 취소
- 공고 상세 이동

---

## 데이터베이스 테이블

### talent_proposal (NEW)
```sql
CREATE TABLE talent_proposal (
    proposal_id BIGSERIAL PRIMARY KEY,
    employer_id BIGINT NOT NULL REFERENCES employer(employer_id),
    candidate_id BIGINT NOT NULL REFERENCES member(member_id),
    job_id BIGINT REFERENCES job_posting(job_id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    title VARCHAR(200) NOT NULL,
    message TEXT,
    offered_salary VARCHAR(100),
    offered_position VARCHAR(100),
    expires_at TIMESTAMP,
    viewed_at TIMESTAMP,
    responded_at TIMESTAMP,
    response_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_talent_proposal UNIQUE (employer_id, candidate_id, job_id)
);
```

---

## 사이드바 메뉴 구성

### 구직자용
- 홈 → `/`
- 채용공고 → `/jobs`
- 면접 → `/interview`
- 이력서 → `/resume`
- 커뮤니티 → `/community`
- 고객센터 → `/support`
- AI → 구직자용 챗봇
- 마이페이지 → `/mypage`

### 기업용
- 홈 → `/`
- 채용공고 → `/jobs`
- 인재풀 → `/talents`
- 기업 → `/companies`
- 구독 → `/payment/products`
- 커뮤니티 → `/community`
- 고객센터 → `/support`
- AI → 기업용 챗봇 (NEW)
- 마이페이지 → `/company/dashboard`

---

## 연동 상태 요약

| 기능 | Frontend | Backend | AI Server | 상태 |
|------|----------|---------|-----------|------|
| 채용공고 조회 | O | O | - | OK |
| 공고 지원 | O | O | - | OK |
| 이력서 관리 | O | O | O | OK |
| 면접 일정 | O | O | - | OK |
| 알림 | O | O | - | OK |
| 구직자 AI 챗봇 | O | - | O | OK |
| 기업 AI 챗봇 | O | - | O | NEW |
| 인재풀 조회 | O | O | - | OK |
| 인재 제안 | O | O | - | NEW |
| 제안 응답 | O | O | - | NEW |
| 입사지원 현황 | O | O | - | NEW |

---

## 환경 변수

### Frontend (.env)
```
VITE_API_BASE_URL=http://localhost:8080
```

### Backend (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fitme
```

### AI Worker (.env)
```
OPENAI_API_KEY=sk-...
REDIS_URL=redis://localhost:6379
DATABASE_URL=postgresql://...
```

---

## 실행 방법

```bash
# Frontend
cd frontend
npm install
npm run dev

# Backend
cd backend
./gradlew bootRun

# AI Worker
cd ai-worker
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

# Docker (전체)
docker-compose up -d
```
