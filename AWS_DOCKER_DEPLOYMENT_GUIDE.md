# AWS CLI + Docker 배포 가이드

이 가이드는 AWS CLI와 Docker를 연결하여 애플리케이션을 배포하는 전체 과정을 단계별로 설명합니다.

---

## 📋 목차

1. [사전 준비](#1-사전-준비)
2. [AWS CLI 설치 및 설정](#2-aws-cli-설치-및-설정)
3. [Docker 설치 및 확인](#3-docker-설치-및-확인)
4. [AWS ECR 설정](#4-aws-ecr-설정)
5. [Docker 이미지 빌드](#5-docker-이미지-빌드)
6. [ECR에 이미지 푸시](#6-ecr에-이미지-푸시)
7. [AWS ECS 배포](#7-aws-ecs-배포)
8. [배포 확인 및 모니터링](#8-배포-확인-및-모니터링)
9. [트러블슈팅](#9-트러블슈팅)

---

## 1. 사전 준비

### 필요한 것들
- AWS 계정
- IAM 사용자 (적절한 권한 필요)
- Docker 설치
- AWS CLI 설치

### 필요한 IAM 권한
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:*",
                "ecs:*",
                "ec2:*",
                "elasticloadbalancing:*",
                "logs:*",
                "iam:PassRole"
            ],
            "Resource": "*"
        }
    ]
}
```

---

## 2. AWS CLI 설치 및 설정

### 2.1 AWS CLI 설치

**macOS:**
```bash
# Homebrew 사용
brew install awscli

# 또는 공식 설치 파일 사용
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /
```

**Linux (Ubuntu/Debian):**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

**Windows:**
```powershell
# PowerShell에서 실행
msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi
```

### 2.2 AWS CLI 버전 확인
```bash
aws --version
# 출력 예: aws-cli/2.x.x Python/3.x.x ...
```

### 2.3 AWS 자격 증명 설정
```bash
aws configure
```
입력 항목:
- **AWS Access Key ID:** IAM에서 발급받은 Access Key
- **AWS Secret Access Key:** IAM에서 발급받은 Secret Key
- **Default region name:** ap-northeast-2 (서울 리전)
- **Default output format:** json

### 2.4 설정 확인
```bash
# 설정된 자격 증명 확인
aws configure list

# AWS 계정 정보 확인
aws sts get-caller-identity
```

출력 예시:
```json
{
    "UserId": "AIDAXXXXXXXXXXXXXXXXX",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/your-username"
}
```

---

## 3. Docker 설치 및 확인

### 3.1 Docker 설치

**macOS:**
```bash
# Docker Desktop 다운로드 및 설치
# https://www.docker.com/products/docker-desktop
```

**Linux (Ubuntu):**
```bash
# 기존 Docker 제거
sudo apt-get remove docker docker-engine docker.io containerd runc

# 필수 패키지 설치
sudo apt-get update
sudo apt-get install ca-certificates curl gnupg lsb-release

# Docker GPG 키 추가
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker 설치
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
```

### 3.2 Docker 버전 확인
```bash
docker --version
docker compose version
```

### 3.3 Docker 실행 상태 확인
```bash
docker info
```

---

## 4. AWS ECR 설정

ECR(Elastic Container Registry)은 AWS의 Docker 이미지 저장소입니다.

### 4.1 ECR 리포지토리 생성

**Backend 리포지토리:**
```bash
aws ecr create-repository \
    --repository-name fitme-backend \
    --region ap-northeast-2 \
    --image-scanning-configuration scanOnPush=true \
    --image-tag-mutability MUTABLE
```

**Frontend 리포지토리:**
```bash
aws ecr create-repository \
    --repository-name fitme-frontend \
    --region ap-northeast-2 \
    --image-scanning-configuration scanOnPush=true \
    --image-tag-mutability MUTABLE
```

### 4.2 ECR 리포지토리 확인
```bash
aws ecr describe-repositories --region ap-northeast-2
```

### 4.3 ECR 로그인 (Docker와 연결)

⭐ **핵심 단계: Docker를 ECR에 연결**

```bash
# ECR 로그인 명령어 가져오기
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com
```

> **참고:** `<AWS_ACCOUNT_ID>`를 실제 AWS 계정 ID로 교체하세요.

**계정 ID 확인 방법:**
```bash
aws sts get-caller-identity --query Account --output text
```

**한 줄로 ECR 로그인:**
```bash
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.ap-northeast-2.amazonaws.com
```

성공 시 출력:
```
Login Succeeded
```

---

## 5. Docker 이미지 빌드

### 5.1 프로젝트 디렉토리 구조 확인
```
fitme/
├── backend/
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
└── docker-compose.yml
```

### 5.2 Backend 이미지 빌드
```bash
# 프로젝트 루트 디렉토리에서 실행
cd backend

# Docker 이미지 빌드
docker build -t fitme-backend:latest .

# 빌드 확인
docker images | grep fitme-backend
```

### 5.3 Frontend 이미지 빌드
```bash
cd ../frontend

# Docker 이미지 빌드
docker build -t fitme-frontend:latest .

# 빌드 확인
docker images | grep fitme-frontend
```

### 5.4 로컬에서 테스트 (선택사항)
```bash
# 프로젝트 루트로 이동
cd ..

# docker-compose로 로컬 테스트
docker compose up -d

# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f

# 테스트 후 종료
docker compose down
```

---

## 6. ECR에 이미지 푸시

### 6.1 환경 변수 설정
```bash
# AWS 계정 ID 저장
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=ap-northeast-2
export ECR_REGISTRY=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# 확인
echo "ECR Registry: $ECR_REGISTRY"
```

### 6.2 이미지 태깅

**Backend:**
```bash
docker tag fitme-backend:latest ${ECR_REGISTRY}/fitme-backend:latest
docker tag fitme-backend:latest ${ECR_REGISTRY}/fitme-backend:$(date +%Y%m%d-%H%M%S)
```

**Frontend:**
```bash
docker tag fitme-frontend:latest ${ECR_REGISTRY}/fitme-frontend:latest
docker tag fitme-frontend:latest ${ECR_REGISTRY}/fitme-frontend:$(date +%Y%m%d-%H%M%S)
```

### 6.3 ECR에 푸시

**Backend:**
```bash
docker push ${ECR_REGISTRY}/fitme-backend:latest
```

**Frontend:**
```bash
docker push ${ECR_REGISTRY}/fitme-frontend:latest
```

### 6.4 푸시 확인
```bash
# Backend 이미지 목록
aws ecr list-images --repository-name fitme-backend --region ap-northeast-2

# Frontend 이미지 목록
aws ecr list-images --repository-name fitme-frontend --region ap-northeast-2
```

---

## 7. AWS ECS 배포

ECS(Elastic Container Service)를 사용하여 컨테이너를 실행합니다.

### 7.1 ECS 클러스터 생성
```bash
aws ecs create-cluster \
    --cluster-name fitme-cluster \
    --region ap-northeast-2 \
    --capacity-providers FARGATE FARGATE_SPOT \
    --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1
```

### 7.2 VPC 및 서브넷 확인
```bash
# 기본 VPC ID 확인
aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text

# 서브넷 확인
aws ec2 describe-subnets --filters "Name=vpc-id,Values=<VPC_ID>" --query "Subnets[*].[SubnetId,AvailabilityZone]" --output table
```

### 7.3 보안 그룹 생성
```bash
# 보안 그룹 생성
aws ec2 create-security-group \
    --group-name fitme-sg \
    --description "Security group for Fitme application" \
    --vpc-id <VPC_ID>

# HTTP 인바운드 규칙 추가 (포트 80)
aws ec2 authorize-security-group-ingress \
    --group-id <SECURITY_GROUP_ID> \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

# Backend 포트 추가 (포트 8080)
aws ec2 authorize-security-group-ingress \
    --group-id <SECURITY_GROUP_ID> \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0
```

### 7.4 IAM 역할 생성 (ECS Task Execution Role)

**신뢰 정책 파일 생성 (trust-policy.json):**
```bash
cat > trust-policy.json << 'EOF'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "ecs-tasks.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
EOF
```

**역할 생성:**
```bash
aws iam create-role \
    --role-name ecsTaskExecutionRole \
    --assume-role-policy-document file://trust-policy.json

aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

### 7.5 Task Definition 생성

**Backend Task Definition (backend-task-def.json):**
```bash
cat > backend-task-def.json << EOF
{
    "family": "fitme-backend",
    "networkMode": "awsvpc",
    "requiresCompatibilities": ["FARGATE"],
    "cpu": "512",
    "memory": "1024",
    "executionRoleArn": "arn:aws:iam::${AWS_ACCOUNT_ID}:role/ecsTaskExecutionRole",
    "containerDefinitions": [
        {
            "name": "fitme-backend",
            "image": "${ECR_REGISTRY}/fitme-backend:latest",
            "portMappings": [
                {
                    "containerPort": 8080,
                    "protocol": "tcp"
                }
            ],
            "environment": [
                {"name": "SPRING_PROFILES_ACTIVE", "value": "prod"},
                {"name": "AWS_REGION", "value": "ap-northeast-2"}
            ],
            "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "/ecs/fitme-backend",
                    "awslogs-region": "ap-northeast-2",
                    "awslogs-stream-prefix": "ecs"
                }
            },
            "healthCheck": {
                "command": ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1"],
                "interval": 30,
                "timeout": 10,
                "retries": 3,
                "startPeriod": 60
            }
        }
    ]
}
EOF
```

**CloudWatch 로그 그룹 생성:**
```bash
aws logs create-log-group --log-group-name /ecs/fitme-backend --region ap-northeast-2
aws logs create-log-group --log-group-name /ecs/fitme-frontend --region ap-northeast-2
```

**Task Definition 등록:**
```bash
aws ecs register-task-definition --cli-input-json file://backend-task-def.json
```

### 7.6 ECS Service 생성
```bash
aws ecs create-service \
    --cluster fitme-cluster \
    --service-name fitme-backend-service \
    --task-definition fitme-backend \
    --desired-count 1 \
    --launch-type FARGATE \
    --network-configuration "awsvpcConfiguration={subnets=[<SUBNET_ID_1>,<SUBNET_ID_2>],securityGroups=[<SECURITY_GROUP_ID>],assignPublicIp=ENABLED}" \
    --region ap-northeast-2
```

---

## 8. 배포 확인 및 모니터링

### 8.1 서비스 상태 확인
```bash
# ECS 서비스 상태
aws ecs describe-services \
    --cluster fitme-cluster \
    --services fitme-backend-service \
    --region ap-northeast-2

# 실행 중인 태스크 확인
aws ecs list-tasks \
    --cluster fitme-cluster \
    --service-name fitme-backend-service \
    --region ap-northeast-2
```

### 8.2 태스크 상세 정보
```bash
aws ecs describe-tasks \
    --cluster fitme-cluster \
    --tasks <TASK_ARN> \
    --region ap-northeast-2
```

### 8.3 CloudWatch 로그 확인
```bash
# 최근 로그 확인
aws logs tail /ecs/fitme-backend --follow --region ap-northeast-2
```

### 8.4 Public IP 확인
```bash
# 태스크의 네트워크 인터페이스 ID 확인
TASK_ARN=$(aws ecs list-tasks --cluster fitme-cluster --service-name fitme-backend-service --query 'taskArns[0]' --output text --region ap-northeast-2)

ENI_ID=$(aws ecs describe-tasks --cluster fitme-cluster --tasks $TASK_ARN --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text --region ap-northeast-2)

# Public IP 확인
aws ec2 describe-network-interfaces --network-interface-ids $ENI_ID --query 'NetworkInterfaces[0].Association.PublicIp' --output text --region ap-northeast-2
```

---

## 9. 트러블슈팅

### 9.1 ECR 로그인 실패
```bash
# 자격 증명 확인
aws sts get-caller-identity

# 다시 로그인 시도
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin ${ECR_REGISTRY}
```

### 9.2 이미지 푸시 실패
```bash
# 이미지 태그 확인
docker images

# 리포지토리 존재 확인
aws ecr describe-repositories --repository-names fitme-backend
```

### 9.3 ECS 태스크 실패
```bash
# 태스크 실패 이유 확인
aws ecs describe-tasks \
    --cluster fitme-cluster \
    --tasks <TASK_ARN> \
    --query 'tasks[0].stoppedReason' \
    --output text

# CloudWatch 로그 확인
aws logs get-log-events \
    --log-group-name /ecs/fitme-backend \
    --log-stream-name <LOG_STREAM_NAME> \
    --region ap-northeast-2
```

### 9.4 일반적인 문제 해결

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| Docker daemon not running | Docker 미실행 | `systemctl start docker` 또는 Docker Desktop 실행 |
| Login credentials expired | 토큰 만료 | ECR 로그인 다시 실행 |
| Image not found | 이미지 태그 오류 | 이미지 이름 및 태그 확인 |
| Task not starting | 메모리/CPU 부족 | Task Definition 리소스 증가 |
| Health check failing | 앱 시작 지연 | startPeriod 값 증가 |

---

## 📝 빠른 참조: 전체 배포 스크립트

```bash
#!/bin/bash
set -e

# 변수 설정
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=ap-northeast-2
export ECR_REGISTRY=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

echo "1. ECR 로그인..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

echo "2. Backend 이미지 빌드..."
docker build -t fitme-backend:latest ./backend

echo "3. Frontend 이미지 빌드..."
docker build -t fitme-frontend:latest ./frontend

echo "4. 이미지 태깅..."
docker tag fitme-backend:latest ${ECR_REGISTRY}/fitme-backend:latest
docker tag fitme-frontend:latest ${ECR_REGISTRY}/fitme-frontend:latest

echo "5. ECR에 푸시..."
docker push ${ECR_REGISTRY}/fitme-backend:latest
docker push ${ECR_REGISTRY}/fitme-frontend:latest

echo "6. ECS 서비스 업데이트..."
aws ecs update-service \
    --cluster fitme-cluster \
    --service fitme-backend-service \
    --force-new-deployment \
    --region $AWS_REGION

echo "✅ 배포 완료!"
```

---

## 🔗 유용한 링크

- [AWS CLI Documentation](https://docs.aws.amazon.com/cli/)
- [Amazon ECR User Guide](https://docs.aws.amazon.com/AmazonECR/latest/userguide/)
- [Amazon ECS Developer Guide](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/)
- [Docker Documentation](https://docs.docker.com/)
