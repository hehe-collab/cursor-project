package com.drama.mapper;

import com.drama.dto.DramaStatsRow;
import com.drama.entity.Drama;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DramaMapper {
    Drama selectById(@Param("id") Integer id);

    Drama selectByIdWithCategory(@Param("id") int id);

    java.util.List<Drama> selectPublicIdPairsByIds(@Param("ids") java.util.List<Integer> ids);

    long countByPublicId(@Param("publicId") String publicId);

    long countByParam(
            @Param("title") String title,
            @Param("categoryId") Integer categoryId,
            @Param("status") String status,
            @Param("id") Integer id,
            @Param("publicId") String publicId);

    java.util.List<Drama> selectByParam(
            @Param("title") String title,
            @Param("categoryId") Integer categoryId,
            @Param("status") String status,
            @Param("id") Integer id,
            @Param("publicId") String publicId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countAll();

    DramaStatsRow selectStatsAggregate(
            @Param("title") String title,
            @Param("categoryId") Integer categoryId,
            @Param("status") String status,
            @Param("id") Integer id,
            @Param("publicId") String publicId);

    int insert(Drama row);

    int update(Drama row);

    int updateTaskStatus(@Param("id") Integer id, @Param("taskStatus") String taskStatus);

    int deleteById(@Param("id") Integer id);
}
