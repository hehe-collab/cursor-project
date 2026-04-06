package com.drama.mapper;

import com.drama.entity.RechargePlanGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RechargePlanGroupMapper {

    List<RechargePlanGroup> selectFiltered(
            @Param("groupName") String groupName,
            @Param("groupPublicIdToken") String groupPublicIdToken,
            @Param("idToken") String idToken);

    RechargePlanGroup selectById(@Param("id") int id);

    int insert(RechargePlanGroup row);

    int update(RechargePlanGroup row);

    int deleteById(@Param("id") int id);

    List<RechargePlanGroup> selectActiveOrderBySort();

    int deleteByIds(@Param("ids") List<Integer> ids);
}
