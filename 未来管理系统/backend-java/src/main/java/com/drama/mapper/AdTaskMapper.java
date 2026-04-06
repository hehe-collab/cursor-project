package com.drama.mapper;

import com.drama.entity.AdTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdTaskMapper {

    List<AdTask> selectAllOrderByCreatedDesc();

    AdTask selectByTaskId(@Param("taskId") String taskId);

    AdTask selectByNumericId(@Param("id") int id);

    int insert(AdTask row);

    int update(AdTask row);

    int deleteByTaskId(@Param("taskId") String taskId);
}
