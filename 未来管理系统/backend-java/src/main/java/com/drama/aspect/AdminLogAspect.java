package com.drama.aspect;

import com.drama.annotation.LogOperation;
import com.drama.entity.Admin;
import com.drama.entity.AdminLog;
import com.drama.mapper.AdminLogMapper;
import com.drama.mapper.AdminMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private final AdminLogMapper adminLogMapper;
    private final AdminMapper adminMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint point, LogOperation logOperation) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        Admin admin = null;
        Integer adminId = getAdminId(request);
        if (adminId != null) {
            admin = adminMapper.selectById(adminId);
        }

        AdminLog adminLog = new AdminLog();

        if (admin != null) {
            adminLog.setAdminId(admin.getId());
            adminLog.setAdminUsername(admin.getUsername());
        }

        adminLog.setOperationType(logOperation.type());
        adminLog.setOperationDesc(logOperation.desc());
        adminLog.setTargetType(logOperation.targetType());

        if (request != null) {
            adminLog.setRequestMethod(request.getMethod());
            adminLog.setRequestUrl(request.getRequestURI());
            adminLog.setIpAddress(getIpAddress(request));
            adminLog.setUserAgent(request.getHeader("User-Agent"));

            try {
                Object[] args = point.getArgs();
                String params = objectMapper.writeValueAsString(args);
                if (params.length() > 2000) {
                    params = params.substring(0, 2000) + "...";
                }
                adminLog.setRequestParams(params);
            } catch (Exception ignored) {}
        }

        Object result = null;
        try {
            result = point.proceed();
            adminLog.setResponseStatus(200);
        } catch (Exception e) {
            adminLog.setResponseStatus(500);
            adminLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            adminLog.setExecutionTime((int) (System.currentTimeMillis() - startTime));
            try {
                adminLogMapper.insert(adminLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    private Integer getAdminId(HttpServletRequest request) {
        if (request == null) return null;
        try {
            Object adminId = request.getAttribute("adminId");
            if (adminId instanceof Integer) return (Integer) adminId;
            if (adminId instanceof Number) return ((Number) adminId).intValue();
        } catch (Exception ignored) {}
        return null;
    }

    private String getIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
