package com.drama.mapper;

import com.drama.entity.BatchTaskItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchTaskItemMapper {

    List<BatchTaskItem> selectByTaskId(@Param("taskId") String taskId);

    List<BatchTaskItem> selectByTaskIdAndStatus(@Param("taskId") String taskId, @Param("status") String status);

    int insertBatch(@Param("list") List<BatchTaskItem> list);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("resultId") String resultId, @Param("errorMessage") String errorMessage);

    int updateStatusByTaskIdAndStatus(@Param("taskId") String taskId,
                                       @Param("oldStatus") String oldStatus,
                                       @Param("newStatus") String newStatus);
}
