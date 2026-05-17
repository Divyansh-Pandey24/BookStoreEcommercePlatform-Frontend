@echo off
title BookNest - Start All Services
color 0A

echo =====================================================
echo      BOOKNEST MICROSERVICES LAUNCHER
echo =====================================================
echo.
echo Starting all services in the correct boot order...
echo Each service will open in its own console window.
echo.

SET BASE_DIR=%~dp0

REM -------------------------------------------------------
REM 1. Eureka Server (Service Registry - MUST start first)
REM -------------------------------------------------------
echo [1/10] Starting Eureka Server...
start "BookNest :: Eureka Server" cmd /k "cd /d %BASE_DIR%eureka-server && mvnw.cmd spring-boot:run"
echo      Waiting 20 seconds for Eureka to initialize...
timeout /t 20 /nobreak > nul

REM -------------------------------------------------------
REM 2. Spring Boot Admin Server (start early — monitors all)
REM -------------------------------------------------------
echo [2/10] Starting Admin Server...
start "BookNest :: Admin Server" cmd /k "cd /d %BASE_DIR%admin-server && mvnw.cmd spring-boot:run"
echo      Waiting 10 seconds for Admin Server to initialize...
timeout /t 10 /nobreak > nul

REM -------------------------------------------------------
REM 3. Auth Service
REM -------------------------------------------------------
echo [3/10] Starting Auth Service...
start "BookNest :: Auth Service" cmd /k "cd /d %BASE_DIR%auth-service && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 4. Book Service
REM -------------------------------------------------------
echo [4/10] Starting Book Service...
start "BookNest :: Book Service" cmd /k "cd /d %BASE_DIR%book-service-1 && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 5. Cart Service
REM -------------------------------------------------------
echo [5/10] Starting Cart Service...
start "BookNest :: Cart Service" cmd /k "cd /d %BASE_DIR%cart-service && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 6. Order Service
REM -------------------------------------------------------
echo [6/10] Starting Order Service...
start "BookNest :: Order Service" cmd /k "cd /d %BASE_DIR%order-service && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 7. Review Service
REM -------------------------------------------------------
echo [7/10] Starting Review Service...
start "BookNest :: Review Service" cmd /k "cd /d %BASE_DIR%review-service && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 8. Wallet Service
REM -------------------------------------------------------
echo [8/10] Starting Wallet Service...
start "BookNest :: Wallet Service" cmd /k "cd /d %BASE_DIR%wallet-service && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 9. Notification Service
REM -------------------------------------------------------
echo [9/10] Starting Notification Service...
start "BookNest :: Notification Service" cmd /k "cd /d %BASE_DIR%notification-service && mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

REM -------------------------------------------------------
REM 10. API Gateway (start last)
REM -------------------------------------------------------
echo [10/10] Starting API Gateway...
start "BookNest :: API Gateway" cmd /k "cd /d %BASE_DIR%api-gateway && mvnw.cmd spring-boot:run"

echo.
echo =====================================================
echo  All services launched! Check individual windows.
echo =====================================================
echo.
echo  Service Registry (Eureka):  http://localhost:8761
echo  Admin Dashboard:            http://localhost:9090
echo  Login: admin / admin123
echo  API Gateway:                http://localhost:8080
echo  Swagger UI (via Gateway):   http://localhost:8080/swagger-ui.html
echo.
pause
