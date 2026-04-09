package com.drama.mapper;

import com.drama.entity.TikTokPixel;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokPixelMapper {

    int insert(TikTokPixel row);

    TikTokPixel selectById(@Param("id") Long id);

    TikTokPixel selectByPixelId(@Param("pixelId") String pixelId);

    List<TikTokPixel> selectByAdvertiserId(@Param("advertiserId") String advertiserId);

    List<TikTokPixel> selectAll();

    int update(TikTokPixel row);

    int updateStatus(@Param("pixelId") String pixelId, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    int deleteByPixelId(@Param("pixelId") String pixelId);
}
