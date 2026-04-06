package com.drama.mapper;

import com.drama.entity.DramaTag;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DramaTagMapper {
    List<DramaTag> selectByDramaId(@Param("dramaId") Integer dramaId);

    int insert(DramaTag row);

    int delete(@Param("dramaId") Integer dramaId, @Param("tagId") Integer tagId);

    int deleteByDramaId(@Param("dramaId") Integer dramaId);
}
