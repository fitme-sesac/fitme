# FitMe 프로젝트 배포 가이드

## 📚 개요
이 프로젝트는 **AWS ECS (Fargate)** 배포, **GitHub Actions** CI/CD, 그리고 **Secrets Manager**를 통한 보안 관리를 사용합니다.

인프라 구성:
- **Frontend**: React (Vite) + Nginx (포트 80)
- **Backend**: Spring Boot (포트 8080)
- **AI Worker**: FastAPI (포트 8000)
- **Database**: AWS RDS (PostgreSQL)
- **Redis**: AWS ElastiCache (선택 사항) 또는 컨테이너

---

## 🚀 배포 방법
GitHub Actions를 통해 완전히 자동화된 배포가 이루어집니다.

### 1. **배포 트리거 (시작하기)**
**`dev` 브랜치에 코드를 푸시**하면 배포가 시작됩니다:
```bash
git checkout dev
git add .
git commit -m "기능 추가 및 수정"
git push origin dev
```
GitHub Actions가 자동으로 다음 작업을 수행합니다:
1. Frontend, Backend, AI Worker의 Docker 이미지를 빌드합니다.
2. 이미지를 AWS ECR에 푸시합니다.
3. ECS Task Definition(작업 정의)을 업데이트합니다.
4. ECS 서비스를 강제로 재배포하여 변경 사항을 적용합니다.

### 2. **배포 상태 확인**
- **GitHub Actions**: 이 저장소의 "Actions" 탭에서 빌드/배포 로그를 실시간으로 확인할 수 있습니다.
- **AWS ECS**: AWS 콘솔 > ECS > 클러스터 > `fitme-cluster` > 서비스 탭에서 상태를 확인하세요.

---

## 🔐 설정 및 Secrets 관리 (중요)
**절대로 `.env` 파일이나 키 파일을 Git에 올리지 마세요.**

### 1. **로컬 개발 환경 설정**
Docker Compose 등을 실행하려면 로컬에 `secret.json` 또는 `.env` 파일을 직접 생성해야 합니다.
**예시 (`.env.example`):**
```ini
POSTGRES_DB=fitme_project
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change_me
REDIS_HOST=localhost
```

### 2. **운영 환경 Secrets (AWS)**
운영 환경의 비밀번호는 **AWS Secrets Manager** (`fitme/database/credentials` 등)에서 관리됩니다.
- **Backend**: `application.yml`이 ECS에서 주입된 환경변수 또는 기본값을 사용하도록 설정되어 있습니다.
- **AI Worker**: `config.py`가 ECS 환경변수를 읽어옵니다.
- **Frontend**: Docker 빌드 시 `VITE_API_BASE_URL`이 주입됩니다.

**운영 Secrets 변경 방법 (예: DB 비밀번호):**
```bash
aws secretsmanager update-secret \
  --secret-id fitme/database/credentials \
  --secret-string '{"username":"postgres","password":"새로운_비밀번호",...}' \
  --region ap-northeast-2
```
*주의: Secrets를 변경한 후에는 반드시 ECS 서비스를 재배포해야 적용됩니다.*

---

## 🛠️ 자주 발생하는 문제 해결 (Troubleshooting)

### 1. **503 Service Temporarily Unavailable 에러**
- **원인**: 백엔드 또는 AI Worker가 정상적으로 실행되지 않음 (Unhealthy).
- **해결**: ECS 로그를 확인하세요.
  ```bash
  aws logs tail "/ecs/fitme-backend" --since 10m --region ap-northeast-2
  ```

### 2. **"TaskFailedToStart" 또는 무한 Pending**
- **원인**: 주로 네트워크 문제(NAT Gateway 부재, 보안 그룹 차단) 또는 필수 환경변수 누락.
- **해결**: 중지된 작업(Stopped Task)의 이유를 확인하세요.
  ```bash
  aws ecs list-tasks --cluster fitme-cluster --desired-status STOPPED --max-items 1
  # Task ID 복사 후 상세 조회
  aws ecs describe-tasks --tasks <TASK_ID> ...
  ```

### 3. **데이터베이스 인증 실패 (`FATAL: password authentication failed`)**
- **원인**: `AWS Secrets Manager`에 저장된 비밀번호와 실제 `RDS` 비밀번호가 다름.
- **해결**: 두 값을 일치시켜야 합니다.
  1. Secrets Manager 값을 확인합니다.
  2. RDS 비밀번호를 모른다면, Secrets Manager 값과 똑같이 리셋합니다:
     ```bash
     aws rds modify-db-instance --db-instance-identifier fitme-postgres --master-user-password "새로운_비밀번호" --apply-immediately
     ```
  3. ECS 서비스를 재배포합니다.

---

## 📂 배포 관련 프로젝트 구조
- `.github/workflows/deploy.yml`: CI/CD 파이프라인 설정 파일.
- `backend/Dockerfile`: 멀티 스테이지 빌드 (Gradle 빌드 -> JRE 실행).
- `frontend/Dockerfile`: 멀티 스테이지 빌드 (Node 빌드 -> Nginx 실행).
- `frontend/nginx.conf`: Nginx 설정 (React SPA 라우팅 처리).
- `ai-worker/Dockerfile`: Python 환경 및 시스템 의존성(tesseract 등) 설치.
