package com.drama.mapper;

import com.drama.dto.CallbackLogStatsRow;
import com.drama.entity.CallbackLog;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CallbackLogMapper {

    CallbackLogStatsRow selectStats();

    List<CallbackLog> selectFiltered(
            @Param("status") String status,
            @Param("eventType") String eventType,
            @Param("orderId") String orderId,
            @Param("userId") String userId,
            @Param("promotionId") String promotionId,
            @Param("dateStart") String dateStart,
            @Param("dateEnd") String dateEnd,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countFiltered(
            @Param("status") String status,
            @Param("eventType") String eventType,
            @Param("orderId") String orderId,
            @Param("userId") String userId,
            @Param("promotionId") String promotionId,
            @Param("dateStart") String dateStart,
            @Param("dateEnd") String dateEnd);
}
