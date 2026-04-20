package com.alper.digitalwallet.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final String CORRELATION_ID = "correlationId";
    private static final Set<String> SENSITIVE_FIELDS = Set.of("balance", "amount", "password", "token");

    @Around("@annotation(com.alper.digitalwallet.infrastructure.logging.LogExecution)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String correlationId = getOrCreateCorrelationId();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] maskedArgs = maskArguments(joinPoint.getArgs());

        log.info("REQUEST - {}.{} - Args: {} - CorrelationId: {}",
                className, methodName, maskedArgs, correlationId);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("{} - {}.{} - Duration: {}ms - CorrelationId: {} - Result: {}",
                    "SUCCESS", className, methodName, duration, correlationId, maskValue(result));

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

    private Object[] maskArguments(Object[] args) {
        if (args == null) {
            return new Object[0];
        }

        Object[] masked = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            masked[i] = maskValue(args[i]);
        }
        return masked;
    }

    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return value;
        }

        String packageName = value.getClass().getPackageName();
        if (packageName.startsWith("java.") || packageName.startsWith("jakarta.") || packageName.startsWith("org.springframework.")) {
            return value.toString();
        }

        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> masked = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                String keyString = key == null ? "" : key.toString().toLowerCase();
                if (isSensitive(keyString)) {
                    masked.put(key, "***");
                } else {
                    masked.put(key, maskValue(entry.getValue()));
                }
            }
            return masked;
        }

        if (value instanceof Iterable<?> iterable) {
            List<Object> maskedList = new ArrayList<>();
            for (Object item : iterable) {
                maskedList.add(maskValue(item));
            }
            return maskedList;
        }

        Map<String, Object> maskedObject = new LinkedHashMap<>();
        for (Field field : value.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object fieldValue = field.get(value);
                if (isSensitive(fieldName.toLowerCase())) {
                    maskedObject.put(fieldName, "***");
                } else {
                    maskedObject.put(fieldName, maskValue(fieldValue));
                }
            } catch (IllegalAccessException ex) {
                maskedObject.put(field.getName(), "<unavailable>");
            }
        }

        if (!maskedObject.isEmpty()) {
            return maskedObject;
        }

        return value.toString();
    }

    private boolean isSensitive(String fieldName) {
        return SENSITIVE_FIELDS.stream().anyMatch(fieldName::contains);
    }
}

