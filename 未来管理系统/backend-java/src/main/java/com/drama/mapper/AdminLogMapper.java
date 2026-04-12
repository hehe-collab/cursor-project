package com.drama.mapper;

import com.drama.entity.AdminLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminLogMapper {

    void insert(AdminLog log);

    List<AdminLog> selectPage(
        @Param("adminId") Integer adminId,
        @Param("operationType") String operationType,
        @Param("targetType") String targetType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit
    );

    int count(
        @Param("adminId") Integer adminId,
        @Param("operationType") String operationType,
        @Param("targetType") String targetType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    List<java.util.Map<String, Object>> countByOperationType(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
