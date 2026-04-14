package com.drama.mapper;

import com.drama.entity.DramaEpisode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DramaEpisodeMapper {
    DramaEpisode selectById(@Param("id") Integer id);

    java.util.List<DramaEpisode> selectByDramaId(@Param("dramaId") Integer dramaId);

    java.util.List<DramaEpisode> selectByDramaIds(@Param("dramaIds") java.util.List<Integer> dramaIds);

    java.util.List<DramaEpisode> selectByVodStatuses(@Param("statuses") java.util.List<String> statuses);

    int insert(DramaEpisode row);

    int update(DramaEpisode row);

    int deleteById(@Param("id") Integer id);

    int deleteByDramaId(@Param("dramaId") Integer dramaId);
}
