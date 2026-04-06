package com.drama.mapper;

import com.drama.dto.UserStatsRow;
import com.drama.entity.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /** 下一个 8 位数字展示编码（需在库中已 backfill user_code） */
    String selectNextUserCode();

    long countAll();

    long countByParam(
            @Param("userId") String userId,
            @Param("username") String username,
            @Param("token") String token,
            @Param("promotionId") String promotionId,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    List<User> selectByParam(
            @Param("userId") String userId,
            @Param("username") String username,
            @Param("token") String token,
            @Param("promotionId") String promotionId,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("offset") int offset,
            @Param("limit") int limit);

    User selectById(@Param("id") int id);

    int insert(User row);

    int updateById(User row);

    int deleteById(@Param("id") int id);

    UserStatsRow selectStatsAggregate(
            @Param("userId") String userId,
            @Param("username") String username,
            @Param("token") String token,
            @Param("promotionId") String promotionId,
            @Param("country") String country,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
