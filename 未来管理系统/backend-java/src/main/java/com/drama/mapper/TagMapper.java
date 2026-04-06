package com.drama.mapper;

import com.drama.dto.TagStatsRow;
import com.drama.entity.Tag;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper {
    Tag selectById(@Param("id") Integer id);

    List<Tag> selectAll();

    long countByParam(@Param("name") String name);

    List<Tag> selectByParam(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    TagStatsRow selectStats();

    List<String> selectNamesByDramaId(@Param("dramaId") Integer dramaId);

    int insert(Tag row);

    int update(Tag row);

    int deleteById(@Param("id") Integer id);
}
