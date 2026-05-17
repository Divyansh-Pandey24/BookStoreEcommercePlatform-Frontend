package com.booknest.cart.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.booknest.cart.service.*.*(..))")
    public void serviceLayer() {
    }

    // ProceedingJoinPoint: This is the most powerful tool in AOP. It is a "Pause
    // Button." When a service method is called, Spring pauses the execution and
    // gives you this joinPoint object. It contains all the info about the paused
    // method.
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
            log.error("!!!! [AOP] Exiting {}.{} | FAILED: {} | Time: {}ms", className, methodName, e.getMessage(),
                    executionTime);
            throw e;
        }
    }
}
