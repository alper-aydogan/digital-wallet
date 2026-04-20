package com.alper.digitalwallet.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String CORRELATION_ID = "correlationId";

    @Around("@annotation(com.alper.digitalwallet.infrastructure.logging.LogExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String correlationId = getOrCreateCorrelationId();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("{} - {}.{} - Duration: {}ms - CorrelationId: {}",
                    "SUCCESS", className, methodName, duration, correlationId);

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("{} - {}.{} - Duration: {}ms - CorrelationId: {} - Error: {}",
                    "ERROR", className, methodName, duration, correlationId, e.getMessage());
            throw e;
        }
    }

    private String getOrCreateCorrelationId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String correlationId = attributes.getRequest().getHeader(CORRELATION_ID);
                if (correlationId == null || correlationId.isEmpty()) {
                    correlationId = UUID.randomUUID().toString();
                }
                attributes.getRequest().setAttribute(CORRELATION_ID, correlationId);
                return correlationId;
            }
        } catch (Exception e) {
            log.debug("Could not get request attributes", e);
        }
        return UUID.randomUUID().toString();
    }
}

