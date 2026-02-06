#!/bin/bash
#===============================================================================
# FitMe AWS ECS Fargate 배포 스크립트
# 
# 사용법: ./deploy.sh [서비스명]
#   서비스명: frontend, backend, ai-worker, all (기본값: all)
#
# 예시:
#   ./deploy.sh              # 모든 서비스 배포
#   ./deploy.sh frontend     # 프론트엔드만 배포
#   ./deploy.sh backend      # 백엔드만 배포
#===============================================================================

set -e  # 에러 발생 시 즉시 중단

#===============================================================================
# 설정 변수 (필요 시 수정)
#===============================================================================
AWS_ACCOUNT_ID="116250269688"
AWS_REGION="ap-northeast-2"
ENVIRONMENT_NAME="fitme"

# ECR 레포지토리 이름
FRONTEND_REPO="fitme-frontend"
BACKEND_REPO="fitme-backend"
AI_WORKER_REPO="fitme-ai-worker"

# ECS 클러스터 및 서비스 이름
ECS_CLUSTER="${ENVIRONMENT_NAME}-cluster"
FRONTEND_SERVICE="${ENVIRONMENT_NAME}-frontend"
BACKEND_SERVICE="${ENVIRONMENT_NAME}-backend"
AI_WORKER_SERVICE="${ENVIRONMENT_NAME}-ai-worker"

# 이미지 태그
IMAGE_TAG="latest"

#===============================================================================
# 색상 코드 (출력 가독성 향상)
#===============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

#===============================================================================
# 유틸리티 함수
#===============================================================================
print_header() {
    echo ""
    echo -e "${CYAN}============================================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}============================================================${NC}"
}

print_step() {
    echo -e "${BLUE}[단계]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[성공]${NC} $1"
}

print_error() {
    echo -e "${RED}[오류]${NC} $1"
    exit 1
}

print_warning() {
    echo -e "${YELLOW}[경고]${NC} $1"
}

#===============================================================================
# ECR 로그인 함수
#===============================================================================
ecr_login() {
    print_header "AWS ECR 로그인"
    
    print_step "ECR 인증 토큰 획득 중..."
    
    aws ecr get-login-password --region ${AWS_REGION} | \
        docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
    
    if [ $? -eq 0 ]; then
        print_success "ECR 로그인 완료"
    else
        print_error "ECR 로그인 실패"
    fi
}

#===============================================================================
# 이미지 빌드 함수
#===============================================================================
build_image() {
    local service_name=$1
    local dockerfile_path=$2
    local repo_name=$3
    
    print_header "${service_name} 이미지 빌드"
    
    print_step "Dockerfile 위치: ${dockerfile_path}"
    print_step "이미지 빌드 시작..."
    
    docker build -t ${repo_name}:${IMAGE_TAG} ${dockerfile_path}
    
    if [ $? -eq 0 ]; then
        print_success "${service_name} 이미지 빌드 완료"
    else
        print_error "${service_name} 이미지 빌드 실패"
    fi
}

#===============================================================================
# 이미지 태그 및 푸시 함수
#===============================================================================
push_image() {
    local service_name=$1
    local repo_name=$2
    
    local ecr_uri="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${repo_name}"
    
    print_header "${service_name} 이미지 푸시"
    
    # 이미지 태그
    print_step "ECR 태그 지정 중..."
    docker tag ${repo_name}:${IMAGE_TAG} ${ecr_uri}:${IMAGE_TAG}
    
    if [ $? -ne 0 ]; then
        print_error "${service_name} 이미지 태그 실패"
    fi
    print_success "이미지 태그 완료: ${ecr_uri}:${IMAGE_TAG}"
    
    # 이미지 푸시
    print_step "ECR로 이미지 푸시 중... (시간이 소요될 수 있습니다)"
    docker push ${ecr_uri}:${IMAGE_TAG}
    
    if [ $? -eq 0 ]; then
        print_success "${service_name} 이미지 푸시 완료"
    else
        print_error "${service_name} 이미지 푸시 실패"
    fi
}

#===============================================================================
# ECS 서비스 배포 함수
#===============================================================================
deploy_ecs_service() {
    local service_name=$1
    local ecs_service=$2
    
    print_header "${service_name} ECS 배포"
    
    print_step "ECS 서비스 강제 재배포 중..."
    
    aws ecs update-service \
        --cluster ${ECS_CLUSTER} \
        --service ${ecs_service} \
        --force-new-deployment \
        --region ${AWS_REGION} \
        --output text > /dev/null 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "${service_name} ECS 배포 트리거 완료"
        print_step "배포 상태 확인 중..."
        
        aws ecs wait services-stable \
            --cluster ${ECS_CLUSTER} \
            --services ${ecs_service} \
            --region ${AWS_REGION} &
        
        local wait_pid=$!
        local counter=0
        
        while kill -0 $wait_pid 2>/dev/null; do
            counter=$((counter + 1))
            echo -ne "\r  대기 중... ${counter}초"
            sleep 1
            
            # 5분(300초) 타임아웃
            if [ $counter -ge 300 ]; then
                print_warning "배포 완료 대기 타임아웃 (5분). 백그라운드에서 계속 진행됩니다."
                break
            fi
        done
        echo ""
        
        print_success "${service_name} 배포 완료"
    else
        print_error "${service_name} ECS 배포 실패"
    fi
}

#===============================================================================
# 서비스별 전체 배포 함수
#===============================================================================
deploy_frontend() {
    build_image "Frontend" "./frontend" "${FRONTEND_REPO}"
    push_image "Frontend" "${FRONTEND_REPO}"
    deploy_ecs_service "Frontend" "${FRONTEND_SERVICE}"
}

deploy_backend() {
    build_image "Backend" "./backend" "${BACKEND_REPO}"
    push_image "Backend" "${BACKEND_REPO}"
    deploy_ecs_service "Backend" "${BACKEND_SERVICE}"
}

deploy_ai_worker() {
    build_image "AI Worker" "./ai-worker" "${AI_WORKER_REPO}"
    push_image "AI Worker" "${AI_WORKER_REPO}"
    deploy_ecs_service "AI Worker" "${AI_WORKER_SERVICE}"
}

deploy_all() {
    deploy_frontend
    deploy_backend
    deploy_ai_worker
}

#===============================================================================
# 빠른 빌드 및 푸시만 (ECS 배포 스킵)
#===============================================================================
push_only() {
    local target=${1:-all}
    
    ecr_login
    
    case ${target} in
        frontend)
            build_image "Frontend" "./frontend" "${FRONTEND_REPO}"
            push_image "Frontend" "${FRONTEND_REPO}"
            ;;
        backend)
            build_image "Backend" "./backend" "${BACKEND_REPO}"
            push_image "Backend" "${BACKEND_REPO}"
            ;;
        ai-worker)
            build_image "AI Worker" "./ai-worker" "${AI_WORKER_REPO}"
            push_image "AI Worker" "${AI_WORKER_REPO}"
            ;;
        all)
            build_image "Frontend" "./frontend" "${FRONTEND_REPO}"
            push_image "Frontend" "${FRONTEND_REPO}"
            build_image "Backend" "./backend" "${BACKEND_REPO}"
            push_image "Backend" "${BACKEND_REPO}"
            build_image "AI Worker" "./ai-worker" "${AI_WORKER_REPO}"
            push_image "AI Worker" "${AI_WORKER_REPO}"
            ;;
    esac
}

#===============================================================================
# 메인 실행 로직
#===============================================================================
main() {
    local command=${1:-deploy}
    local target=${2:-all}
    
    # 첫 번째 인자가 서비스명인 경우 처리
    case ${command} in
        frontend|backend|ai-worker|all)
            target=${command}
            command="deploy"
            ;;
        push)
            # push 명령어는 빌드+푸시만 수행
            print_header "FitMe 이미지 빌드 및 푸시"
            echo -e "  대상: ${YELLOW}${target}${NC}"
            echo -e "  리전: ${AWS_REGION}"
            echo -e "  계정: ${AWS_ACCOUNT_ID}"
            echo ""
            push_only ${target}
            print_header "빌드 및 푸시 완료!"
            exit 0
            ;;
    esac
    
    print_header "FitMe ECS Fargate 배포 시작"
    echo -e "  대상: ${YELLOW}${target}${NC}"
    echo -e "  리전: ${AWS_REGION}"
    echo -e "  클러스터: ${ECS_CLUSTER}"
    echo -e "  태그: ${IMAGE_TAG}"
    echo ""
    
    # ECR 로그인 (모든 배포에 필요)
    ecr_login
    
    # 대상 서비스 배포
    case ${target} in
        frontend)
            deploy_frontend
            ;;
        backend)
            deploy_backend
            ;;
        ai-worker)
            deploy_ai_worker
            ;;
        all)
            deploy_all
            ;;
        *)
            print_error "알 수 없는 서비스: ${target}"
            echo "사용 가능한 옵션: frontend, backend, ai-worker, all"
            exit 1
            ;;
    esac
    
    print_header "배포 완료!"
    echo -e "${GREEN}모든 작업이 성공적으로 완료되었습니다.${NC}"
    echo ""
    echo "다음 단계:"
    echo "  1. AWS 콘솔에서 ECS 서비스 상태를 확인하세요."
    echo "  2. ALB DNS로 접속하여 서비스를 확인하세요."
    echo ""
    echo "ALB URL 확인:"
    echo "  aws cloudformation describe-stacks --stack-name fitme-alb --query \"Stacks[0].Outputs[?OutputKey=='LoadBalancerURL'].OutputValue\" --output text"
    echo ""
}

# 스크립트 실행
main "$@"
