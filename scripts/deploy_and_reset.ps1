$ErrorActionPreference = "Stop"

# Configuration
$AWS_REGION = "ap-northeast-2"
$ECR_REPO = "116250269688.dkr.ecr.ap-northeast-2.amazonaws.com/fitme-backend"
$CLUSTER_NAME = "fitme-cluster"
$SERVICE_NAME = "fitme-backend"

Write-Host "1. ECR Login..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO

Write-Host "2. Building Docker Image..."
# Using the backend directory context
docker build -t "$ECR_REPO`:latest" -t "$ECR_REPO`:db-reset" -f backend/Dockerfile backend

Write-Host "3. Pushing Docker Image..."
docker push "$ECR_REPO`:latest"
docker push "$ECR_REPO`:db-reset"

Write-Host "4. Updating ECS Service..."
aws ecs update-service --cluster $CLUSTER_NAME --service $Service_NAME --force-new-deployment

Write-Host "Deployment Triggered Successfully."
