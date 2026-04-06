package com.drama.mapper;

import com.drama.entity.RechargePlan;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RechargePlanMapper {

    long countFiltered(
            @Param("name") String name,
            @Param("idToken") String idToken,
            @Param("paymentPlatform") String paymentPlatform,
            @Param("status") String status);

    List<RechargePlan> selectFilteredPage(
            @Param("name") String name,
            @Param("idToken") String idToken,
            @Param("paymentPlatform") String paymentPlatform,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<RechargePlan> selectAllActiveOrderByIdDesc();

    RechargePlan selectById(@Param("id") int id);

    List<RechargePlan> selectByIds(@Param("ids") List<Integer> ids);

    int insert(RechargePlan row);

    int update(RechargePlan row);

    int deleteById(@Param("id") int id);
}
