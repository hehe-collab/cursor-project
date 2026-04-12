package com.drama.mapper;

import com.drama.entity.TikTokExcelImport;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokExcelImportMapper {

    int insert(TikTokExcelImport row);

    TikTokExcelImport selectById(@Param("id") Long id);

    List<TikTokExcelImport> selectByAdvertiserId(
            @Param("advertiserId") String advertiserId, @Param("limit") int limit, @Param("offset") int offset);

    List<TikTokExcelImport> selectByStatus(
            @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    List<TikTokExcelImport> selectAllPaged(@Param("limit") int limit, @Param("offset") int offset);

    int update(TikTokExcelImport row);

    int deleteById(@Param("id") Long id);

    int countByAdvertiserId(@Param("advertiserId") String advertiserId);

    int countAll();
}
