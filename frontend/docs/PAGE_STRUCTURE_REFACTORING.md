# Frontend Pages 구조 리팩토링 문서

> 작성일: 2025-02-01
> 수정일: 2025-02-01
> 목적: 페이지 파일을 역할별로 분류하여 유지보수성 향상

---

## 0. 최근 변경사항 (2025-02-01)

### Mock 데이터 → API 연결 완료

커뮤니티 관련 컴포넌트들이 Mock 데이터 대신 실제 백엔드 API를 호출하도록 변경되었습니다.

**수정된 파일:**
- `src/components/mypage/CommunityManagement.tsx` - API 연결
- `src/components/community/CommunitySidebar.tsx` - API 연결
- `src/components/company/CompanyCommunityManagement.tsx` - API 연결
- `src/api/community.js` - API 함수 확장

**삭제된 파일:**
- `src/data/mockCommunityData.ts` - Mock 데이터 파일 삭제

**추가된 API 엔드포인트:**
| API 함수 | 엔드포인트 | 설명 |
|----------|-----------|------|
| `getMyPosts()` | `GET /api/community/my/posts` | 내 게시글 조회 |
| `getMyComments()` | `GET /api/community/my/comments` | 내 댓글 조회 |
| `getMyLikedPosts()` | `GET /api/community/my/liked` | 좋아요한 글 조회 |
| `getMyCommunityStats()` | `GET /api/community/my/stats` | 커뮤니티 활동 통계 |
| `getPopularPosts()` | `GET /api/community/popular` | 인기 게시글 |
| `getRecommendedMembers()` | `GET /api/community/recommended-members` | 추천 멤버 |
| `getAnnouncements()` | `GET /api/community/announcements` | 공지사항 |
| `getPosts()` | `GET /api/community/posts` | 게시글 목록 |
| `createPost()` | `POST /api/community/posts` | 게시글 작성 |
| `updatePost()` | `PUT /api/community/posts/:id` | 게시글 수정 |
| `deletePost()` | `DELETE /api/community/posts/:id` | 게시글 삭제 |
| `likePost()` | `POST /api/community/posts/:id/like` | 좋아요 |
| `unlikePost()` | `DELETE /api/community/posts/:id/like` | 좋아요 취소 |

**백엔드 구현 필요:**
위 API 엔드포인트들이 백엔드에 구현되어 있어야 합니다. 커뮤니티 관련 컨트롤러가 아직 없다면 구현이 필요합니다.

---

### 사용자 정보 표시 수정 (DB 연결)

**문제점:**
- 마이페이지, 커뮤니티 사이드바 등에서 "사용자님 안녕하세요"로 표시되는 문제
- `AuthContext`의 `user.user_metadata.display_name`이 비어있음
- 백엔드 `/api/auth/status`가 JWT 정보만 반환하고 DB 정보를 반환하지 않음

**해결:**

1. **백엔드 `AuthStatusController` 수정** (`backend/src/main/java/.../auth/controller/AuthStatusController.java`)
   - `UserRepository` 주입하여 DB에서 실제 사용자 정보 조회
   - `name`, `email`, `phone`, `user_metadata` 등 상세 정보 반환

2. **백엔드 `ResumeService.getProfileSummary()` 추가** (`backend/src/main/java/.../resume/service/ResumeService.java`)
   - `/api/v1/resumes/my-profile-summary` 엔드포인트 추가
   - 사용자 기본 정보 + 대표 이력서 요약 반환

3. **프론트엔드 수정:**
   - `JobSeekerMyPage.tsx`: `profileSummary.display_name` 또는 `user.name` 사용
   - `CommunitySidebar.tsx`: `user.name` fallback 추가

**API 응답 구조 (`/api/auth/status`):**
```json
{
  "authenticated": true,
  "id": 1,
  "name": "홍길동",
  "email": "user@example.com",
  "role": "CANDIDATE",
  "userid": "testuser",
  "phone": "010-1234-5678",
  "user_metadata": {
    "display_name": "홍길동",
    "avatar_url": "",
    "job_title": ""
  }
}
```

**API 응답 구조 (`/api/v1/resumes/my-profile-summary`):**
```json
{
  "id": 1,
  "display_name": "홍길동",
  "email": "user@example.com",
  "phone": "010-1234-5678",
  "role": "CANDIDATE",
  "resumeCount": 2,
  "avatar_url": "...",
  "job_title": "백엔드 개발",
  "primaryResume": {
    "id": 1,
    "title": "이력서 제목",
    "field": "백엔드 개발",
    "skills": ["Java", "Spring"],
    "preferenceLocation": "서울"
  }
}
```

---

## 1. 변경 개요

### 변경 전 구조
```
frontend/src/pages/
├── *.jsx / *.tsx (36개 파일이 평탄하게 나열)
└── payment/ (3개 파일)
```

### 변경 후 구조
```
frontend/src/pages/
├── guest/          # 비회원/인증 관련 (15개)
├── jobseeker/      # 구직자 전용 (4개)
├── company/        # 기업회원 전용 (3개)
├── admin/          # 관리자 전용 (1개)
├── common/         # 공통 기능 (10개)
└── payment/        # 결제 관련 (3개) - 기존 유지
```

---

## 2. 폴더별 페이지 분류

### 2.1 guest/ (비회원/인증 관련)
| 파일명 | 용도 | 라우트 |
|--------|------|--------|
| `Auth.tsx` | 로그인/회원가입 통합 페이지 | `/auth` |
| `LoginPage.jsx` | 레거시 로그인 | `/Login` → `/auth` 리다이렉트 |
| `RegisterPage.jsx` | 레거시 회원가입 | `/Register` → `/auth` 리다이렉트 |
| `FindUserIdPage.jsx` | 아이디 찾기 | `/FindUserId`, `/auth/find-id` |
| `VerifyUserIdCodePage.jsx` | 아이디 찾기 인증 | `/VerifyUserIdCode` |
| `ResultUserIdPage.jsx` | 아이디 찾기 결과 | `/ResultUserId`, `/auth/find-id/result` |
| `FindPasswordPage.jsx` | 비밀번호 찾기 | `/FindPassword`, `/auth/find-password` |
| `VerifyCodePage.jsx` | 비밀번호 인증코드 | `/VerifyCode` |
| `NewPasswordPage.jsx` | 새 비밀번호 설정 | `/NewPassword` |
| `ChangePasswordPage.jsx` | 비밀번호 변경 | `/User/Change_Password` |
| `ResetPasswordPage.jsx` | 비밀번호 리셋 | - |
| `ReEnterCredentialsPage.jsx` | 자격증명 재입력 | - |
| `FirstSocialLoginPage.jsx` | 소셜 최초 로그인 | `/FirstSocialLogin` |
| `OAuthCallbackPage.jsx` | OAuth 콜백 처리 | - |
| `JobSeekerSignup.tsx` | 구직자 회원가입 | `/signup/job-seeker` |

**백엔드 의존성:**
- `UserController`
- `FindUserIdApiController`
- `FindPasswordApiController`
- `PhoneOtpController`
- `PasswordResetController`
- `CustomOAuth2UserService`

**DB 테이블:**
- `member`
- `phone_verification`
- `member_login_log`

---

### 2.2 jobseeker/ (구직자 전용)
| 파일명 | 용도 | 라우트 | 접근 권한 |
|--------|------|--------|-----------|
| `JobSeekerMyPage.tsx` | 구직자 마이페이지 | `/jobseeker/mypage` | CANDIDATE |
| `Resume.tsx` | 이력서 관리 | `/resume` | - |
| `Interview.tsx` | 면접 일정 관리 | `/interview` | - |
| `MyPage.tsx` | 마이페이지 (공통) | `/mypage` | - |

**백엔드 의존성:**
- `ResumeController`
- `AiResumeController`
- `InterviewController`
- `JobApplicationController`
- `MyPageService`

**DB 테이블:**
- `resume` (+ 7개 하위 테이블)
  - `resume_profile`
  - `resume_career`
  - `resume_certificate`
  - `resume_project`
  - `resume_attachment`
  - `resume_link`
- `interview_schedule`
- `interview_response`
- `job_application`
- `job_scrap`

---

### 2.3 company/ (기업회원 전용)
| 파일명 | 용도 | 라우트 | 접근 권한 |
|--------|------|--------|-----------|
| `CompanyDashboard.tsx` | 기업 대시보드 | `/company/dashboard` | EMPLOYER |
| `CompanyManagement.tsx` | 기업 관리 | `/companies` | - |
| `Talents.tsx` | 인재 검색/추천 | `/talents` | - |

**백엔드 의존성:**
- `EmployerController`
- `JobController`
- `JobApplicationController`
- `AdCampaignController`
- `AdServeController`
- `ResumeMatchController`

**DB 테이블:**
- `employer`
- `employer_member`
- `job_posting`
- `job_application`
- `ad_campaign`
- `ad_click_event`
- `resume` (AI 매칭용 - vector embedding)

---

### 2.4 admin/ (관리자 전용)
| 파일명 | 용도 | 라우트 | 접근 권한 |
|--------|------|--------|-----------|
| `AdminDashboard.tsx` | 관리자 대시보드 | `/admin` | SERVICEADMIN |

**백엔드 의존성:**
- `AdminPaymentController`
- `AdminSubscriptionController`
- `AdminWalletController`
- `AdminProductController`

**DB 테이블:**
- 전체 테이블 접근 권한 (관리/모니터링)

---

### 2.5 common/ (공통 기능)
| 파일명 | 용도 | 라우트 |
|--------|------|--------|
| `Index.tsx` | 메인 홈페이지 | `/` |
| `HomePage.jsx` | 홈페이지 (대안) | - |
| `Jobs.tsx` | 채용공고 목록 | `/jobs` |
| `JobDetail.jsx` | 채용공고 상세 | `/jobs/:jobId` |
| `CompanyDetail.jsx` | 기업 상세 정보 | `/companies/:companyId` |
| `Community.tsx` | 커뮤니티 | `/community` |
| `Support.tsx` | 고객지원/FAQ | `/support` |
| `Settings.tsx` | 설정 | `/settings` (로그인 필요) |
| `Subscription.tsx` | 구독/상품 | `/subscription`, `/payment/products` |
| `NotFound.tsx` | 404 페이지 | `*` |

**백엔드 의존성:**
- `PublicJobController`
- `JobController`
- `JobScrapController`
- `EmployerController`
- `FAQController`
- `NoticeController`
- `ProductController`
- `SubscriptionController`

**DB 테이블:**
- `job_posting`
- `job_scrap`
- `job_view_log`
- `employer`
- `faq`
- `notice`
- `inquiry`
- `product`
- `subscription`

---

### 2.6 payment/ (결제 관련 - 기존 유지)
| 파일명 | 용도 | 라우트 |
|--------|------|--------|
| `PaymentCheckoutPage.tsx` | 결제 페이지 | - |
| `PaymentSuccessPage.tsx` | 결제 성공 | `/payment/success` |
| `PaymentFailPage.tsx` | 결제 실패 | `/payment/fail` |

**백엔드 의존성:**
- `OrderController`
- `PaymentController`
- `TossWebhookController`

**DB 테이블:**
- `orders`
- `payment`
- `payment_cancel`
- `wallet`
- `wallet_ledger`
- `wallet_credit_lot`

---

## 3. 백엔드-프론트엔드 의존성 맵

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Frontend Pages                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  guest/Auth ──────────────┬──► UserController                       │
│  guest/Find* ─────────────┤    └─► member                           │
│  guest/Verify* ───────────┤                                         │
│                           ├──► PhoneOtpController                   │
│                           │    └─► phone_verification               │
│                           │                                         │
│  jobseeker/Resume ────────┼──► ResumeController                     │
│                           │    └─► resume, resume_career,           │
│                           │        resume_certificate,              │
│                           │        resume_project, ...              │
│                           │                                         │
│  jobseeker/Interview ─────┼──► InterviewController                  │
│                           │    └─► interview_schedule,              │
│                           │        interview_response               │
│                           │                                         │
│  company/Dashboard ───────┼──► JobController                        │
│                           │    └─► job_posting                      │
│                           │                                         │
│                           ├──► JobApplicationController             │
│                           │    └─► job_application                  │
│                           │                                         │
│                           ├──► AdCampaignController                 │
│                           │    └─► ad_campaign, ad_click_event      │
│                           │                                         │
│  company/Talents ─────────┼──► ResumeMatchController                │
│                           │    └─► resume (vector similarity)       │
│                           │                                         │
│  admin/Dashboard ─────────┼──► Admin*Controller (4개)               │
│                           │    └─► 전체 테이블 관리                  │
│                           │                                         │
│  common/Jobs ─────────────┼──► PublicJobController                  │
│  common/JobDetail ────────┤    └─► job_posting (public read)        │
│                           │                                         │
│  payment/* ───────────────┼──► OrderController                      │
│                           │    └─► orders                           │
│                           ├──► PaymentController                    │
│                           │    └─► payment, wallet, wallet_ledger   │
│                           │                                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. ERD 테이블 - 기능 그룹 매핑

| 기능 그룹 | DB 테이블 | 관련 페이지 카테고리 |
|-----------|-----------|---------------------|
| **회원/인증** | `member`, `phone_verification`, `member_login_log` | `guest/` |
| **기업** | `employer`, `employer_member` | `company/` |
| **채용** | `job_posting`, `job_application`, `job_scrap`, `job_view_log` | `common/`, `company/`, `jobseeker/` |
| **이력서** | `resume`, `resume_profile`, `resume_career`, `resume_certificate`, `resume_project`, `resume_attachment`, `resume_link` | `jobseeker/` |
| **면접** | `interview_schedule`, `interview_response` | `jobseeker/`, `company/` |
| **결제/월렛** | `product`, `orders`, `payment`, `wallet`, `wallet_ledger`, `wallet_credit_lot` | `payment/`, `common/` |
| **구독** | `subscription`, `subscription_billing_cycle` | `company/`, `admin/` |
| **광고** | `ad_campaign`, `ad_click_event` | `company/` |
| **고객지원** | `inquiry`, `inquiry_message`, `notice`, `faq` | `common/` |
| **신고/제재** | `report`, `moderation_action`, `member_penalty_point` | `admin/` |
| **알림** | `notification_template`, `notification_delivery`, `notice_delivery` | 시스템 |

---

## 5. App.tsx 변경사항

### Import 구조 변경

**변경 전:**
```typescript
import Index from "./pages/Index";
import Jobs from "./pages/Jobs";
import Auth from "./pages/Auth";
// ... 평탄한 경로
```

**변경 후:**
```typescript
// ============================================
// Common Pages (공통 기능)
// ============================================
import Index from "./pages/common/Index";
import Jobs from "./pages/common/Jobs";
// ...

// ============================================
// Guest Pages (비회원/인증)
// ============================================
import Auth from "./pages/guest/Auth";
// ...

// ============================================
// JobSeeker Pages (구직자 전용)
// ============================================
import Resume from "./pages/jobseeker/Resume";
// ...

// ============================================
// Company Pages (기업회원 전용)
// ============================================
import CompanyDashboard from "./pages/company/CompanyDashboard";
// ...

// ============================================
// Admin Pages (관리자 전용)
// ============================================
import AdminDashboard from "./pages/admin/AdminDashboard";
// ...
```

---

## 6. 역할(Role) 기반 접근 제어

| Role | 설명 | 접근 가능 페이지 |
|------|------|-----------------|
| `CANDIDATE` | 구직자 | `jobseeker/*`, `common/*` |
| `EMPLOYER` | 기업회원 | `company/*`, `common/*` |
| `SERVICEADMIN` | 서비스 관리자 | `admin/*`, 전체 |
| `APPROVEADMIN` | 승인 관리자 | 일부 관리 기능 |
| `MASTER` | 마스터 | 전체 |

**PrivateRoute 사용 예시:**
```tsx
<Route
    path="/company/dashboard"
    element={
        <PrivateRoute requiredRole="EMPLOYER">
            <CompanyDashboard />
        </PrivateRoute>
    }
/>
```

---

## 7. 향후 확장 계획

### 7.1 Admin 페이지 분리 예정
```
admin/
├── AdminDashboard.tsx      # 현재
├── MemberManagement.tsx    # 회원 관리 (예정)
├── JobManagement.tsx       # 채용공고 관리 (예정)
├── ReportManagement.tsx    # 신고 관리 (예정)
├── PaymentManagement.tsx   # 결제 관리 (예정)
└── SystemMonitoring.tsx    # 시스템 모니터링 (예정)
```

### 7.2 각 폴더 index.ts 생성 권장
```typescript
// pages/guest/index.ts
export { default as Auth } from './Auth';
export { default as FindUserIdPage } from './FindUserIdPage';
// ...
```

---

## 8. 마이그레이션 체크리스트

- [x] 폴더 구조 생성 (`guest/`, `jobseeker/`, `company/`, `admin/`, `common/`)
- [x] 페이지 파일 이동
- [x] `App.tsx` import 경로 수정
- [ ] 빌드 테스트 (`npm run build`)
- [ ] 각 라우트 동작 테스트
- [ ] 레거시 URL 리다이렉트 동작 확인

---

## 9. 참고사항

- **URL 경로는 변경되지 않음**: 폴더 구조만 변경, 사용자에게 보이는 URL은 그대로 유지
- **레거시 호환**: `/Login`, `/Register`, `/User/*` 경로들은 새 경로로 리다이렉트 처리됨
- **백엔드 연동**: 백엔드 API 엔드포인트 변경 없음
