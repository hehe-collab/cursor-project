package com.drama.aspect;

import com.drama.annotation.RequirePermission;
import com.drama.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;
    private final HttpServletRequest request;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Integer adminId = getAdminId();
        if (adminId == null) {
            throw new RuntimeException("未登录或会话已过期");
        }

        List<String> permissions = permissionService.getPermissionCodesByAdminId(adminId);
        String required = requirePermission.value();

        if (!permissions.contains(required)) {
            String desc = requirePermission.description();
            throw new RuntimeException("权限不足：需要「" + (desc.isEmpty() ? required : desc) + "」权限");
        }

        return joinPoint.proceed();
    }

    private Integer getAdminId() {
        try {
            Object adminId = request.getAttribute("adminId");
            if (adminId instanceof Integer) return (Integer) adminId;
            if (adminId instanceof Number) return ((Number) adminId).intValue();
        } catch (Exception ignored) {}
        return null;
    }
}
