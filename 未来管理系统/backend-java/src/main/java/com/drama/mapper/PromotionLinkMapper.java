package com.drama.mapper;

import com.drama.entity.PromotionLink;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromotionLinkMapper {

    List<PromotionLink> selectAllOrderByIdDesc();

    List<PromotionLink> selectAllWithDramaJoinOrderByIdDesc();

    PromotionLink selectById(@Param("id") int id);

    PromotionLink selectByPromoteId(@Param("promoteId") String promoteId);

    int countByPromoteIdExcludeId(@Param("promoteId") String promoteId, @Param("excludeId") Integer excludeId);

    int insert(PromotionLink row);

    int update(PromotionLink row);

    int deleteById(@Param("id") int id);

    int deleteByIds(@Param("ids") List<Integer> ids);
}
