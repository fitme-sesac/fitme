#!/bin/bash
# ============================================
# AWS ECR + ECS 배포 스크립트
# ============================================
set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 사용법
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -b, --backend-only    Backend만 배포"
    echo "  -f, --frontend-only   Frontend만 배포"
    echo "  -a, --all             Backend + Frontend 배포 (기본값)"
    echo "  -l, --local           로컬 테스트 모드 (docker-compose)"
    echo "  -h, --help            도움말 표시"
    echo ""
    echo "Examples:"
    echo "  $0                    # 전체 배포"
    echo "  $0 -b                 # Backend만 배포"
    echo "  $0 -l                 # 로컬 테스트"
}

# 환경 변수 설정
setup_env() {
    log_info "환경 변수 설정 중..."
    
    export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
    if [ -z "$AWS_ACCOUNT_ID" ]; then
        log_error "AWS 자격 증명을 확인할 수 없습니다. 'aws configure'를 실행하세요."
        exit 1
    fi
    
    export AWS_REGION=${AWS_REGION:-ap-northeast-2}
    export ECR_REGISTRY=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
    export IMAGE_TAG=${IMAGE_TAG:-latest}
    
    log_success "AWS Account ID: $AWS_ACCOUNT_ID"
    log_success "AWS Region: $AWS_REGION"
    log_success "ECR Registry: $ECR_REGISTRY"
}

# Docker 확인
check_docker() {
    log_info "Docker 상태 확인 중..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker가 설치되어 있지 않습니다."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker daemon이 실행 중이 아닙니다."
        exit 1
    fi
    
    log_success "Docker 준비 완료"
}

# ECR 로그인
ecr_login() {
    log_info "ECR 로그인 중..."
    
    aws ecr get-login-password --region $AWS_REGION | \
        docker login --username AWS --password-stdin $ECR_REGISTRY
    
    if [ $? -eq 0 ]; then
        log_success "ECR 로그인 성공"
    else
        log_error "ECR 로그인 실패"
        exit 1
    fi
}

# ECR 리포지토리 생성 (없는 경우)
ensure_ecr_repo() {
    local repo_name=$1
    
    if ! aws ecr describe-repositories --repository-names $repo_name --region $AWS_REGION &> /dev/null; then
        log_info "ECR 리포지토리 생성: $repo_name"
        aws ecr create-repository \
            --repository-name $repo_name \
            --region $AWS_REGION \
            --image-scanning-configuration scanOnPush=true \
            --image-tag-mutability MUTABLE
        log_success "리포지토리 생성 완료: $repo_name"
    else
        log_info "리포지토리 존재 확인: $repo_name"
    fi
}

# Backend 빌드 및 푸시
deploy_backend() {
    log_info "===== Backend 배포 시작 ====="
    
    ensure_ecr_repo "fitme-backend"
    
    log_info "Backend 이미지 빌드 중..."
    docker build -t fitme-backend:$IMAGE_TAG ./backend
    
    log_info "Backend 이미지 태깅 중..."
    docker tag fitme-backend:$IMAGE_TAG ${ECR_REGISTRY}/fitme-backend:$IMAGE_TAG
    docker tag fitme-backend:$IMAGE_TAG ${ECR_REGISTRY}/fitme-backend:$(date +%Y%m%d-%H%M%S)
    
    log_info "Backend 이미지 푸시 중..."
    docker push ${ECR_REGISTRY}/fitme-backend:$IMAGE_TAG
    
    log_success "===== Backend 배포 완료 ====="
}

# Frontend 빌드 및 푸시
deploy_frontend() {
    log_info "===== Frontend 배포 시작 ====="
    
    ensure_ecr_repo "fitme-frontend"
    
    log_info "Frontend 이미지 빌드 중..."
    docker build -t fitme-frontend:$IMAGE_TAG ./frontend
    
    log_info "Frontend 이미지 태깅 중..."
    docker tag fitme-frontend:$IMAGE_TAG ${ECR_REGISTRY}/fitme-frontend:$IMAGE_TAG
    docker tag fitme-frontend:$IMAGE_TAG ${ECR_REGISTRY}/fitme-frontend:$(date +%Y%m%d-%H%M%S)
    
    log_info "Frontend 이미지 푸시 중..."
    docker push ${ECR_REGISTRY}/fitme-frontend:$IMAGE_TAG
    
    log_success "===== Frontend 배포 완료 ====="
}

# ECS 서비스 업데이트
update_ecs_service() {
    local service_name=$1
    
    log_info "ECS 서비스 업데이트: $service_name"
    
    if aws ecs describe-services --cluster fitme-cluster --services $service_name --region $AWS_REGION --query 'services[0].status' --output text 2>/dev/null | grep -q "ACTIVE"; then
        aws ecs update-service \
            --cluster fitme-cluster \
            --service $service_name \
            --force-new-deployment \
            --region $AWS_REGION > /dev/null
        log_success "ECS 서비스 업데이트 완료: $service_name"
    else
        log_warn "ECS 서비스가 존재하지 않습니다: $service_name"
        log_info "AWS 콘솔에서 ECS 서비스를 먼저 생성하세요."
    fi
}

# 로컬 테스트
local_test() {
    log_info "===== 로컬 테스트 모드 ====="
    
    log_info "Docker Compose 빌드 및 실행..."
    docker compose up --build -d
    
    log_info "서비스 상태 확인..."
    sleep 5
    docker compose ps
    
    log_success "로컬 테스트 환경이 실행되었습니다."
    log_info "Frontend: http://localhost:80"
    log_info "Backend: http://localhost:8080"
    log_info "종료하려면: docker compose down"
}

# 메인
main() {
    local deploy_backend_flag=false
    local deploy_frontend_flag=false
    local local_mode=false
    
    # 인자 파싱
    while [[ $# -gt 0 ]]; do
        case $1 in
            -b|--backend-only)
                deploy_backend_flag=true
                shift
                ;;
            -f|--frontend-only)
                deploy_frontend_flag=true
                shift
                ;;
            -a|--all)
                deploy_backend_flag=true
                deploy_frontend_flag=true
                shift
                ;;
            -l|--local)
                local_mode=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                log_error "알 수 없는 옵션: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    # 기본값: 전체 배포
    if [ "$deploy_backend_flag" = false ] && [ "$deploy_frontend_flag" = false ] && [ "$local_mode" = false ]; then
        deploy_backend_flag=true
        deploy_frontend_flag=true
    fi
    
    echo ""
    echo "============================================"
    echo "       AWS Docker 배포 스크립트"
    echo "============================================"
    echo ""
    
    if [ "$local_mode" = true ]; then
        check_docker
        local_test
        exit 0
    fi
    
    # AWS 배포
    check_docker
    setup_env
    ecr_login
    
    if [ "$deploy_backend_flag" = true ]; then
        deploy_backend
        update_ecs_service "fitme-backend-service"
    fi
    
    if [ "$deploy_frontend_flag" = true ]; then
        deploy_frontend
        update_ecs_service "fitme-frontend-service"
    fi
    
    echo ""
    log_success "============================================"
    log_success "          배포가 완료되었습니다!"
    log_success "============================================"
    echo ""
}

main "$@"
