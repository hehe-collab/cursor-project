package com.drama.mapper;

import com.drama.entity.RechargePlanGroupPlan;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RechargePlanGroupPlanMapper {

    int deleteByGroupId(@Param("groupId") int groupId);

    int insert(RechargePlanGroupPlan row);

    List<RechargePlanGroupPlan> selectByGroupIdOrdered(@Param("groupId") int groupId);
}
