package com.drama.mapper;

import com.drama.dto.CategoryStatsRow;
import com.drama.entity.Category;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {

    Category selectById(@Param("id") Integer id);

    List<Category> selectAll();

    long countByParam(@Param("name") String name);

    List<Category> selectByParam(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    CategoryStatsRow selectStats();

    int insert(Category row);

    int update(Category row);

    int deleteById(@Param("id") Integer id);
}
