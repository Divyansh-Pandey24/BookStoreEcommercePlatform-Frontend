#!/bin/bash
REGION="us-east-1"
PREFIX="booknest"

SERVICES=(
    "api-gateway"
    "eureka-server"
    "auth-service"
    "book-service"
    "cart-service"
    "order-service"
    "wallet-service"
    "notification-service"
    "review-service"
    "admin-server"
    "frontend"
)

for SERVICE in "${SERVICES[@]}"
do
    echo "Creating repository: ${PREFIX}-${SERVICE}"
    aws ecr create-repository --repository-name "${PREFIX}-${SERVICE}" --region "${REGION}" || echo "Repo already exists"
done

echo "All ECR repositories checked/created."
