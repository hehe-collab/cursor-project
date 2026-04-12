package com.drama.service;

import com.drama.entity.AdminLog;
import com.drama.mapper.AdminLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogMapper adminLogMapper;

    public Map<String, Object> queryPage(
        Integer adminId,
        String operationType,
        String targetType,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size
    ) {
        int offset = (page - 1) * size;
        List<AdminLog> list = adminLogMapper.selectPage(
            adminId, operationType, targetType, startDate, endDate, offset, size);
        int total = adminLogMapper.count(adminId, operationType, targetType, startDate, endDate);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public List<Map<String, Object>> statsByOperationType(LocalDate startDate, LocalDate endDate) {
        return adminLogMapper.countByOperationType(startDate, endDate);
    }
}
