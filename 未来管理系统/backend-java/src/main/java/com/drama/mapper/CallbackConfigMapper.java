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

    /**
     * 按候选 link_id 匹配（TRIM 后相等），用于复制投放链接时克隆回传配置。候选含：投放链接主键字符串、
     * promote_id（与 {@code callback_configs.link_id} 存储方式对齐）。
     */
    List<CallbackConfig> selectForPromotionLinkCloneByKeys(@Param("keys") List<String> keys);

    int insert(CallbackConfig row);

    int update(CallbackConfig row);

    int deleteById(@Param("id") int id);

    int deleteByIds(@Param("ids") List<Integer> ids);
}
