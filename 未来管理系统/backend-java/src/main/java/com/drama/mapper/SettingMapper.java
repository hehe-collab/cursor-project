package com.drama.mapper;

import com.drama.entity.Setting;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettingMapper {

    Setting selectById(@Param("id") Integer id);

    Setting selectByKeyName(@Param("keyName") String keyName);

    List<Setting> selectAll();

    /**
     * INSERT … ON DUPLICATE KEY UPDATE，依赖 {@code key_name} 唯一索引。
     */
    int upsert(@Param("keyName") String keyName, @Param("value") String value);

    int insert(Setting row);

    int update(Setting row);
}
