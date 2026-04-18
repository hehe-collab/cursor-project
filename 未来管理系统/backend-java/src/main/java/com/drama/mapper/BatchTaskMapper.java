package com.drama.mapper;

import com.drama.entity.BatchTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BatchTaskMapper {

    BatchTask selectByTaskId(@Param("taskId") String taskId);

    List<BatchTask> selectByUserId(@Param("userId") Integer userId, @Param("limit") int limit, @Param("offset") int offset);

    List<BatchTask> selectByStatus(@Param("status") String status, @Param("limit") int limit);

    int insert(BatchTask row);

    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    int updateStarted(@Param("taskId") String taskId);

    int updateCompleted(@Param("taskId") String taskId, @Param("status") String status, @Param("resultJson") String resultJson);

    int incrementSuccess(@Param("taskId") String taskId);

    int incrementFailed(@Param("taskId") String taskId);

    int updateProgress(@Param("taskId") String taskId);

    int updateErrorMessage(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);

    List<BatchTask> selectStuckProcessing(@Param("minutes") int minutes);
}
