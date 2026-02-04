# FitMe 프로젝트 비교: fitme (현재) vs fitme_fixed_v2 (첨부 폴더)

> 비교 기준: **현재 프로젝트** `c:\Users\Administrator\Desktop\front\v1\fitme`  
> 비교 대상: **첨부 폴더** `c:\Users\Administrator\Desktop\fitme_fixed_v2`  
> 작성일: 2026-02-03

---

## 1. 구조 차이 요약

| 구분 | fitme (현재 프로젝트) | fitme_fixed_v2 (첨부 폴더) |
|------|------------------------|-----------------------------|
| **루트** | `.github/` (CI, 이슈 템플릿), `.gitignore`, `docker-compose.yml` | `.env`, `.env.docker`, `.idea/`, `docker-compose.yml`, `PROJECT_FIX_REPORT.md`, `PROJECT_STRUCTURE.md` |
| **백엔드** | Gradle, 소스만 (빌드 산출물 없음), **community 패키지 있음** | Gradle, **빌드 산출물(.class) 포함**, community 백엔드 없음 |
| **프론트엔드** | `App.tsx` + `main.tsx`, `eslint.config.js`, `index.html`, `Dockerfile`, `PAGE_STRUCTURE_REFACTORING.md` | `App.tsx` 라우팅, `Header.tsx` (layout), 문서 1개 |
| **결제 컴포넌트** | `CreditChargeModal.tsx` 만 있음 (PaymentResultModal 삭제됨) | `CreditChargeModal.tsx`, **PaymentResultModal.tsx** 있음 |
| **ai-worker** | 구조 동일 (chatbot, jobs, resumes, employer_chatbot 등) | 구조 동일, 파일 수 더 많음 (스크립트/테스트 등) |

---

## 2. 상세 차이

### 2.1 루트 / 설정

- **fitme (현재)**
  - `.github/workflows/ci.yml`, `.github/ISSUE_TEMPLATE/` → CI·이슈 관리 가능
  - `.env` 커밋 안 함 (보통 `.gitignore` 대상)
  - IDE 설정(.idea) 없음 → 저장소가 더 단순

- **fitme_fixed_v2**
  - `.env`, `.env.docker` 포함 → 로컬/도커 설정 예시로는 유용, 보안상 `.env`는 커밋 제외 권장
  - `.idea/` 포함 → IDE별 설정이 섞일 수 있음
  - **PROJECT_FIX_REPORT.md**, **PROJECT_STRUCTURE.md** → 수정 이력·구조 문서화에 유리

### 2.2 백엔드 (Spring Boot)

| 항목 | fitme (현재) | fitme_fixed_v2 |
|------|--------------|----------------|
| **build.gradle** | 동일 (group `com.woori`, Java 21, Spring Boot 3.3.2) | 동일 |
| **community** | **있음** (controller, dto, entity, repository, service) | **없음** |
| **PaymentAppStatus** | `description` 필드 + `canTransitionTo()` 구현, **REQUESTED → APPROVED, FAILED, CANCELED** 허용 | `description` 없음, `canTransitionTo()` 있으나 **REQUESTED → APPROVED, FAILED만** 허용 (REQUESTED→CANCELED 없음) |
| **기타** | `PProjectApplication.java` 루트, `IndustryUtil` 등 common 유틸, moderation 등 일부 `.gitkeep` | 빌드 산출물(.class)이 `src` 아래 포함됨 |

- **결제 상태 전이:**  
  현재 프로젝트는 결제 **요청(REQUESTED) 상태에서 취소(CANCELED)** 로 가는 전이를 허용하도록 이미 수정되어 있음.  
  fitme_fixed_v2는 `REQUESTED` → `CANCELED` 전이가 없어, “요청 후 취소” 플로우에서 **검증 오류가 날 수 있음**.

### 2.3 프론트엔드 (React + Vite)

| 항목 | fitme (현재) | fitme_fixed_v2 |
|------|--------------|----------------|
| **엔트리** | `main.tsx` → `App.tsx` (React Query, ErrorBoundary, Toaster) | `App.tsx`에서 라우팅·AuthProvider·Sonner 직접 구성 |
| **라우팅** | `App.tsx`에 Routes 정의, **CompanyInterviews** 페이지 있음 | 비슷한 라우팅, Company 관련 페이지 구성 다소 상이 |
| **Layout** | `Header.tsx` (layout), CreditChargeModal 등 | `Header.tsx` (layout), **PaymentResultModal** 추가 |
| **결제 UI** | PaymentResultModal **삭제** 상태, 결제 결과는 별도 페이지 등으로 처리 가능 | PaymentResultModal로 결과 모달 제공 |
| **문서** | `docs/PAGE_STRUCTURE_REFACTORING.md` (페이지 구조·API·커뮤니티 Mock 제거 등) | `docs/` 내 1개 md, 루트에 PROJECT_FIX_REPORT 등 |

- **PaymentResultModal**  
  - fitme_fixed_v2: 결제 성공/실패를 **모달**로 보여줄 수 있음.  
  - fitme (현재): 해당 컴포넌트가 없고, `PaymentSuccessPage` / `PaymentFailPage` 등 **전용 페이지**로 처리하는 구조일 수 있음.  
  → “모달이 꼭 필요하면” fixed_v2의 PaymentResultModal을 현재 쪽으로 가져오는 선택 가능.

### 2.4 Docker / 환경

- **docker-compose.yml**  
  - 두 쪽 모두 postgres(pgvector), redis, pgadmin(profiles), backend, frontend, ai-worker 등 유사한 구성.  
  - 세부 포트·볼륨·env_file 차이는 있을 수 있으나, 큰 틀은 동일.

---

## 3. 어느 쪽이 더 좋은지 비교

### 3.1 fitme (현재 프로젝트)가 유리한 점

1. **CI/협업**
   - `.github/workflows/ci.yml`, 이슈 템플릿으로 빌드·이슈 관리 가능.
   - `.idea` 미포함으로 저장소가 더 중립적.

2. **백엔드**
   - **community** 도메인이 있어 커뮤니티 기능을 백엔드까지 구현할 수 있음.
   - **PaymentAppStatus**가 REQUESTED→CANCELED를 지원해, “결제 요청 후 취소” 시나리오가 정상 동작.
   - `description` 등으로 결제 상태를 화면/로그에 활용하기 좋음.
   - 소스 디렉터리에 `.class`가 없어 관리가 깔끔함.

3. **프론트엔드**
   - `main.tsx`에서 React Query, ErrorBoundary, Toaster를 한 곳에서 설정해 구조가 명확함.
   - **PAGE_STRUCTURE_REFACTORING.md**로 페이지 구조·API·Mock 제거 내용이 문서화됨.
   - `CompanyInterviews` 등 기업용 페이지가 현재 기준으로 더 반영되어 있음.

4. **보안/관리**
   - `.env`를 커밋하지 않는 구조가 일반적인 보안 관례에 맞음.

### 3.2 fitme_fixed_v2가 유리한 점

1. **문서**
   - **PROJECT_FIX_REPORT.md**: TalentRecommendationSection, WideTalentCard, SideJobList, EmployerHeroSection, Talents 모달, AI 챗봇 404, 관리자 페이지 등 **수정 내역이 정리**되어 있음.
   - **PROJECT_STRUCTURE.md**: 디렉터리 구조·라우트·기능 연결 상태가 정리되어 있어 온보딩에 도움됨.

2. **UI/UX**
   - **PaymentResultModal**이 있어, 결제 완료/실패를 모달로 바로 보여주는 플로우를 쓰기 쉬움.

3. **로컬 실행**
   - `.env`, `.env.docker`가 있으면 환경 변수 예시로 참고하기는 편함 (단, 실제 값은 커밋하지 않는 것이 좋음).

### 3.3 결론 및 권장 사항

- **기본 추천: 현재 프로젝트(fitme)를 기준으로 유지**하는 쪽이 좋습니다.
  - CI·이슈 템플릿으로 협업이 가능하고,
  - 백엔드에 community가 있으며,
  - PaymentAppStatus가 “요청 후 취소”까지 지원하고,
  - 프론트는 React Query·ErrorBoundary 등이 정리되어 있습니다.

- **fitme_fixed_v2에서 가져오면 좋은 것**
  1. **문서**: `PROJECT_FIX_REPORT.md`, `PROJECT_STRUCTURE.md`를 현재 프로젝트 `docs/` 등에 복사해 두면, 수정 이력·구조 파악에 유용합니다.
  2. **PaymentResultModal**: 결제 결과를 모달로 쓰고 싶다면 fixed_v2의 `PaymentResultModal.tsx`를 현재 프로젝트에 복사한 뒤, 라우트/페이지와 연동해 사용할 수 있습니다.
  3. **라우트/버튼 수정 사례**: PROJECT_FIX_REPORT에 나온 `/jobs/all` → `/jobs`, `/jobs/new` → `/employer/jobs/create`, WideTalentCard `asChild` 등은 현재 프로젝트에 동일 이슈가 있다면 참고해 적용하면 됩니다.

- **현재 프로젝트에서 유지할 것**
  - `.github/`, `.gitignore` (`.env` 제외), 백엔드의 **PaymentAppStatus**(현재 구현), **community** 패키지, `main.tsx` 구조, `PAGE_STRUCTURE_REFACTORING.md`.

---

## 4. 요약 표

| 기준 | fitme (현재) | fitme_fixed_v2 |
|------|--------------|----------------|
| CI/협업 | ✅ .github | ❌ 없음 |
| 백엔드 community | ✅ 있음 | ❌ 없음 |
| 결제 상태 전이 (REQUESTED→CANCELED) | ✅ 지원 | ❌ 미지원 |
| 수정/구조 문서 | PAGE_STRUCTURE_REFACTORING 위주 | ✅ PROJECT_FIX_REPORT, PROJECT_STRUCTURE |
| PaymentResultModal | ❌ 없음 | ✅ 있음 |
| .env 커밋 | ❌ 없음 (권장) | ⚠️ 있음 (참고용만 권장) |
| **종합** | **기준 코드로 사용 권장** | **문서·모달 참고용으로 활용** |

이 문서는 두 프로젝트의 차이를 정리한 것이며, 실제 반영 시에는 테스트와 단계적 적용을 권장합니다.
