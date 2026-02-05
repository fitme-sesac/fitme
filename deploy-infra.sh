#!/bin/bash
#===============================================================================
# FitMe AWS 인프라 배포 스크립트
# 
# CloudFormation 스택을 순서대로 배포합니다.
#
# 사용법: ./deploy-infra.sh [명령어]
#   명령어: deploy, delete, status
#===============================================================================

set -e

#===============================================================================
# 설정 변수
#===============================================================================
AWS_REGION="ap-northeast-2"
ENVIRONMENT_NAME="fitme"
CLOUDFORMATION_DIR="./aws/cloudformation"

# 스택 배포 순서 (의존성 순서대로)
STACKS=(
    "01-vpc"
    "02-security-groups"
    "03-database"
    "04-elasticache"
    "05-ecs-cluster"
    "06-alb"
    "07-ecs-tasks"
    "08-ecs-services"
)

#===============================================================================
# 색상 코드
#===============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

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
}

print_warning() {
    echo -e "${YELLOW}[경고]${NC} $1"
}

#===============================================================================
# 시크릿 값 입력 받기 (최초 배포 시)
#===============================================================================
get_secrets() {
    print_header "시크릿 값 설정"
    
    echo "다음 값들을 입력해주세요 (기존 .env.docker 파일 참조):"
    echo ""
    
    read -p "DB 비밀번호: " -s DB_PASSWORD
    echo ""
    read -p "Redis AUTH 토큰 (16자 이상): " -s REDIS_PASSWORD
    echo ""
    read -p "JWT Secret: " -s JWT_SECRET
    echo ""
    read -p "OpenAI API Key: " -s OPENAI_API_KEY
    echo ""
    
    export DB_PASSWORD REDIS_PASSWORD JWT_SECRET OPENAI_API_KEY
}

#===============================================================================
# 스택 배포 함수
#===============================================================================
deploy_stack() {
    local stack_name=$1
    local template_file="${CLOUDFORMATION_DIR}/${stack_name}.yml"
    local full_stack_name="${ENVIRONMENT_NAME}-${stack_name}"
    
    print_step "스택 배포: ${full_stack_name}"
    
    # 스택 존재 여부 확인
    if aws cloudformation describe-stacks --stack-name ${full_stack_name} --region ${AWS_REGION} > /dev/null 2>&1; then
        print_warning "스택이 이미 존재합니다. 업데이트 시도..."
        
        aws cloudformation update-stack \
            --stack-name ${full_stack_name} \
            --template-body file://${template_file} \
            --capabilities CAPABILITY_NAMED_IAM \
            --region ${AWS_REGION} \
            --parameters ParameterKey=EnvironmentName,ParameterValue=${ENVIRONMENT_NAME} \
            2>/dev/null || {
                if [ $? -eq 254 ]; then
                    print_warning "변경 사항 없음"
                    return 0
                fi
            }
        
        print_step "업데이트 대기 중..."
        aws cloudformation wait stack-update-complete \
            --stack-name ${full_stack_name} \
            --region ${AWS_REGION}
    else
        # 새 스택 생성
        aws cloudformation create-stack \
            --stack-name ${full_stack_name} \
            --template-body file://${template_file} \
            --capabilities CAPABILITY_NAMED_IAM \
            --region ${AWS_REGION} \
            --parameters ParameterKey=EnvironmentName,ParameterValue=${ENVIRONMENT_NAME}
        
        print_step "스택 생성 대기 중... (몇 분 소요될 수 있습니다)"
        aws cloudformation wait stack-create-complete \
            --stack-name ${full_stack_name} \
            --region ${AWS_REGION}
    fi
    
    print_success "${full_stack_name} 배포 완료"
}

#===============================================================================
# 데이터베이스 스택 배포 (비밀번호 파라미터 포함)
#===============================================================================
deploy_database_stack() {
    local template_file="${CLOUDFORMATION_DIR}/03-database.yml"
    local full_stack_name="${ENVIRONMENT_NAME}-03-database"
    
    print_step "스택 배포: ${full_stack_name}"
    
    if [ -z "$DB_PASSWORD" ]; then
        read -p "DB 비밀번호: " -s DB_PASSWORD
        echo ""
    fi
    
    if aws cloudformation describe-stacks --stack-name ${full_stack_name} --region ${AWS_REGION} > /dev/null 2>&1; then
        print_warning "데이터베이스 스택이 이미 존재합니다."
        return 0
    fi
    
    aws cloudformation create-stack \
        --stack-name ${full_stack_name} \
        --template-body file://${template_file} \
        --region ${AWS_REGION} \
        --parameters \
            ParameterKey=EnvironmentName,ParameterValue=${ENVIRONMENT_NAME} \
            ParameterKey=DBPassword,ParameterValue="${DB_PASSWORD}"
    
    print_step "RDS 생성 대기 중... (약 5-10분 소요)"
    aws cloudformation wait stack-create-complete \
        --stack-name ${full_stack_name} \
        --region ${AWS_REGION}
    
    print_success "${full_stack_name} 배포 완료"
}

#===============================================================================
# ElastiCache 스택 배포 (비밀번호 파라미터 포함)
#===============================================================================
deploy_elasticache_stack() {
    local template_file="${CLOUDFORMATION_DIR}/04-elasticache.yml"
    local full_stack_name="${ENVIRONMENT_NAME}-04-elasticache"
    
    print_step "스택 배포: ${full_stack_name}"
    
    if [ -z "$REDIS_PASSWORD" ]; then
        read -p "Redis AUTH 토큰 (16자 이상): " -s REDIS_PASSWORD
        echo ""
    fi
    
    if aws cloudformation describe-stacks --stack-name ${full_stack_name} --region ${AWS_REGION} > /dev/null 2>&1; then
        print_warning "ElastiCache 스택이 이미 존재합니다."
        return 0
    fi
    
    aws cloudformation create-stack \
        --stack-name ${full_stack_name} \
        --template-body file://${template_file} \
        --region ${AWS_REGION} \
        --parameters \
            ParameterKey=EnvironmentName,ParameterValue=${ENVIRONMENT_NAME} \
            ParameterKey=RedisAuthToken,ParameterValue="${REDIS_PASSWORD}"
    
    print_step "ElastiCache 생성 대기 중... (약 5-10분 소요)"
    aws cloudformation wait stack-create-complete \
        --stack-name ${full_stack_name} \
        --region ${AWS_REGION}
    
    print_success "${full_stack_name} 배포 완료"
}

#===============================================================================
# 시크릿 스택 배포
#===============================================================================
deploy_secrets_stack() {
    local template_file="${CLOUDFORMATION_DIR}/09-secrets.yml"
    local full_stack_name="${ENVIRONMENT_NAME}-09-secrets"
    
    print_step "스택 배포: ${full_stack_name}"
    
    # RDS 엔드포인트 가져오기
    local rds_endpoint=$(aws cloudformation describe-stacks \
        --stack-name ${ENVIRONMENT_NAME}-03-database \
        --query "Stacks[0].Outputs[?OutputKey=='RDSEndpoint'].OutputValue" \
        --output text \
        --region ${AWS_REGION} 2>/dev/null || echo "pending")
    
    # Redis 엔드포인트 가져오기
    local redis_endpoint=$(aws cloudformation describe-stacks \
        --stack-name ${ENVIRONMENT_NAME}-04-elasticache \
        --query "Stacks[0].Outputs[?OutputKey=='RedisEndpoint'].OutputValue" \
        --output text \
        --region ${AWS_REGION} 2>/dev/null || echo "pending")
    
    if [ -z "$DB_PASSWORD" ]; then
        read -p "DB 비밀번호: " -s DB_PASSWORD
        echo ""
    fi
    if [ -z "$REDIS_PASSWORD" ]; then
        read -p "Redis AUTH 토큰: " -s REDIS_PASSWORD
        echo ""
    fi
    if [ -z "$JWT_SECRET" ]; then
        read -p "JWT Secret: " -s JWT_SECRET
        echo ""
    fi
    if [ -z "$OPENAI_API_KEY" ]; then
        read -p "OpenAI API Key: " -s OPENAI_API_KEY
        echo ""
    fi
    
    aws cloudformation create-stack \
        --stack-name ${full_stack_name} \
        --template-body file://${template_file} \
        --region ${AWS_REGION} \
        --parameters \
            ParameterKey=EnvironmentName,ParameterValue=${ENVIRONMENT_NAME} \
            ParameterKey=DBHost,ParameterValue="${rds_endpoint}" \
            ParameterKey=DBPassword,ParameterValue="${DB_PASSWORD}" \
            ParameterKey=RedisHost,ParameterValue="${redis_endpoint}" \
            ParameterKey=RedisPassword,ParameterValue="${REDIS_PASSWORD}" \
            ParameterKey=JWTSecret,ParameterValue="${JWT_SECRET}" \
            ParameterKey=OpenAIAPIKey,ParameterValue="${OPENAI_API_KEY}" \
        2>/dev/null || print_warning "시크릿 스택 생성 실패 또는 이미 존재"
    
    aws cloudformation wait stack-create-complete \
        --stack-name ${full_stack_name} \
        --region ${AWS_REGION} 2>/dev/null || true
    
    print_success "시크릿 배포 완료"
}

#===============================================================================
# 전체 배포
#===============================================================================
deploy_all() {
    print_header "FitMe AWS 인프라 배포"
    
    # 1. 네트워크 (VPC)
    deploy_stack "01-vpc"
    
    # 2. 보안 그룹
    deploy_stack "02-security-groups"
    
    # 3. 데이터베이스 (RDS)
    deploy_database_stack
    
    # 4. ElastiCache (Redis)
    deploy_elasticache_stack
    
    # 5. 시크릿
    deploy_secrets_stack
    
    # 6. ECS 클러스터
    deploy_stack "05-ecs-cluster"
    
    # 7. ALB
    deploy_stack "06-alb"
    
    # 8. Task Definitions
    deploy_stack "07-ecs-tasks"
    
    # 9. ECS Services
    deploy_stack "08-ecs-services"
    
    print_header "배포 완료!"
    
    # ALB URL 출력
    local alb_url=$(aws cloudformation describe-stacks \
        --stack-name ${ENVIRONMENT_NAME}-06-alb \
        --query "Stacks[0].Outputs[?OutputKey=='LoadBalancerURL'].OutputValue" \
        --output text \
        --region ${AWS_REGION})
    
    echo -e "${GREEN}서비스 URL: ${alb_url}${NC}"
}

#===============================================================================
# 스택 상태 확인
#===============================================================================
check_status() {
    print_header "스택 상태 확인"
    
    for stack in "${STACKS[@]}"; do
        local full_stack_name="${ENVIRONMENT_NAME}-${stack}"
        local status=$(aws cloudformation describe-stacks \
            --stack-name ${full_stack_name} \
            --query "Stacks[0].StackStatus" \
            --output text \
            --region ${AWS_REGION} 2>/dev/null || echo "NOT_FOUND")
        
        case ${status} in
            *COMPLETE*)
                echo -e "${GREEN}✓${NC} ${full_stack_name}: ${status}"
                ;;
            *IN_PROGRESS*)
                echo -e "${YELLOW}○${NC} ${full_stack_name}: ${status}"
                ;;
            NOT_FOUND)
                echo -e "${BLUE}○${NC} ${full_stack_name}: 미배포"
                ;;
            *)
                echo -e "${RED}✗${NC} ${full_stack_name}: ${status}"
                ;;
        esac
    done
    
    # 시크릿 스택도 확인
    local secrets_status=$(aws cloudformation describe-stacks \
        --stack-name ${ENVIRONMENT_NAME}-09-secrets \
        --query "Stacks[0].StackStatus" \
        --output text \
        --region ${AWS_REGION} 2>/dev/null || echo "NOT_FOUND")
    echo -e "${GREEN}✓${NC} ${ENVIRONMENT_NAME}-09-secrets: ${secrets_status}"
}

#===============================================================================
# 전체 삭제
#===============================================================================
delete_all() {
    print_header "FitMe AWS 인프라 삭제"
    print_warning "모든 리소스가 삭제됩니다!"
    
    read -p "정말 삭제하시겠습니까? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "취소되었습니다."
        exit 0
    fi
    
    # 역순으로 삭제
    local reverse_stacks=($(echo "${STACKS[@]}" | tr ' ' '\n' | tac | tr '\n' ' '))
    
    # 시크릿 먼저 삭제
    aws cloudformation delete-stack --stack-name ${ENVIRONMENT_NAME}-09-secrets --region ${AWS_REGION} 2>/dev/null || true
    
    for stack in "${reverse_stacks[@]}"; do
        local full_stack_name="${ENVIRONMENT_NAME}-${stack}"
        print_step "스택 삭제: ${full_stack_name}"
        
        aws cloudformation delete-stack \
            --stack-name ${full_stack_name} \
            --region ${AWS_REGION} 2>/dev/null || true
        
        aws cloudformation wait stack-delete-complete \
            --stack-name ${full_stack_name} \
            --region ${AWS_REGION} 2>/dev/null || true
    done
    
    print_success "모든 스택 삭제 완료"
}

#===============================================================================
# 메인
#===============================================================================
case ${1:-deploy} in
    deploy)
        deploy_all
        ;;
    status)
        check_status
        ;;
    delete)
        delete_all
        ;;
    *)
        echo "사용법: $0 [deploy|status|delete]"
        exit 1
        ;;
esac
