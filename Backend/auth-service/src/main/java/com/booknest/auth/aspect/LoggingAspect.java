package com.booknest.auth.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for logging execution of service methods.
 * Provides a "basic level" of AOP by capturing method entry, parameters,
 * execution time, and success/failure status.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Declares the diagnostic execution pointcut for the authentication service layer.
    @Pointcut("execution(* com.booknest.auth.service.*.*(..))")
    public void serviceLayer() {}

    // Intercepts service-layer calls, logging parameters, execution duration, and success/error outcomes.
    @Around("serviceLayer()")
    public Object logServiceAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        log.info(">>>> [AOP] Entering {}.{} | Args: {}", className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            log.info("<<<< [AOP] Exiting {}.{} | Success | Time: {}ms", className, methodName, executionTime);
            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("!!!! [AOP] Exiting {}.{} | FAILED: {} | Time: {}ms", className, methodName, e.getMessage(), executionTime);
            throw e;
        }
    }
}
