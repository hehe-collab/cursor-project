package com.drama.mapper;

import com.drama.entity.CallbackConfig;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CallbackConfigMapper {

    List<CallbackConfig> selectFiltered(
            @Param("platform") String platform, @Param("linkIdContains") String linkIdContains);

    CallbackConfig selectById(@Param("id") int id);

    int insert(CallbackConfig row);

    int update(CallbackConfig row);

    int deleteById(@Param("id") int id);

    int deleteByIds(@Param("ids") List<Integer> ids);
}
