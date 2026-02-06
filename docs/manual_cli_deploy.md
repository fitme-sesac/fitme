# AWS CLI 수동 배포 및 시크릿 업데이트 가이드

이 가이드는 Windows 환경에서 AWS CLI를 사용하여 시크릿을 업데이트하고 서비스를 배포하는 방법을 설명합니다.

## 전제 조건
- AWS CLI가 설치되어 있어야 합니다.
- AWS 자격 증명이 설정되어 있어야 합니다 (프로필 설정 또는 환경 변수).
- `fitme-secrets` 및 `fitme-services` 스택이 이미 생성되어 있다고 가정합니다.

## 1. 시크릿 업데이트 (Secrets Update)

`.env` 파일의 변경 사항이 있다면, AWS Secrets Manager에 반영해야 합니다.

### PowerShell 명령어
`.env` 파일의 내용을 읽어 변수로 설정한 후 업데이트하는 스크립트입니다.

```powershell
# .env 파일에서 필요한 값만 읽어서 변수에 저장 (예시)
$env:POSTGRES_PASSWORD = Get-Content .env | Select-String "POSTGRES_PASSWORD=" | ForEach-Object { $_.ToString().Split('=')[1] }
$env:REDIS_PASSWORD = Get-Content .env | Select-String "REDIS_PASSWORD=" | ForEach-Object { $_.ToString().Split('=')[1] }
$env:TOSS_SECRET_KEY = Get-Content .env | Select-String "TOSS_SECRET_KEY=" | ForEach-Object { $_.ToString().Split('=')[1] }
$env:TOSS_CLIENT_KEY = Get-Content .env | Select-String "TOSS_CLIENT_KEY=" | ForEach-Object { $_.ToString().Split('=')[1] }

# 스택 업데이트 실행
# 주의: 아래 명령어는 예시이며, 실제로는 09-secrets.yml에 정의된 모든 파라미터 중 변경이 필요한 값을 전달해야 합니다.
# 이미 설정된 값은 UsePreviousValue=true로 유지할 수 있으나, CloudFormation 명령줄에서는 각 파라미터를 명시하는 것이 명확합니다.

aws cloudformation deploy `
  --template-file aws/cloudformation/09-secrets.yml `
  --stack-name fitme-09-secrets `
  --parameter-overrides `
    DBPassword="$env:POSTGRES_PASSWORD" `
    RedisPassword="$env:REDIS_PASSWORD" `
    TossSecretKey="$env:TOSS_SECRET_KEY" `
    TossClientKey="$env:TOSS_CLIENT_KEY" `
    EnvironmentName="fitme"
```

## 2. 서비스 설정 업데이트 (Zero Downtime 적용)

방금 수정한 `08-ecs-services.yml` (무중단 배포 설정)을 적용합니다. 이 단계는 **서비스 설정(DesiredCount, DeploymentConfiguration 등)을 변경할 때** 필요합니다.

```powershell
aws cloudformation deploy `
  --template-file aws/cloudformation/08-ecs-services.yml `
  --stack-name fitme-services `
  --parameter-overrides DesiredCount=1 `
  --capabilities CAPABILITY_NAMED_IAM
```

## 3. 서비스 강제 재배포 (Force Deployment)

설정이나 코드는 그대로지만, **새로운 도커 이미지를 받아와서 컨테이너를 재시작**하고 싶을 때 사용합니다.
(이제 `MinimumHealthyPercent: 100`이 적용되어, 새 작업이 뜰 때까지 기존 작업이 죽지 않습니다.)

```powershell
# Frontend 재배포
aws ecs update-service --cluster fitme-cluster --service fitme-frontend --force-new-deployment

# Backend 재배포
aws ecs update-service --cluster fitme-cluster --service fitme-backend --force-new-deployment

# AI Worker 재배포
aws ecs update-service --cluster fitme-cluster --service fitme-ai-worker --force-new-deployment
```

## 4. 모니터링

배포 진행 상황을 확인하려면:

```powershell
# 서비스 상태 확인
aws ecs describe-services --cluster fitme-cluster --services fitme-backend
```
