package com.drama.mapper;

import com.drama.entity.TikTokAccount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TikTokAccountMapper {

    int upsert(TikTokAccount row);

    TikTokAccount selectByAdvertiserId(@Param("advertiserId") String advertiserId);

    List<TikTokAccount> selectByStatus(@Param("status") String status);

    int updateTokens(TikTokAccount row);

    TikTokAccount selectById(@Param("id") Long id);

    List<TikTokAccount> selectAllOrderByIdAsc();

    int updateById(TikTokAccount row);

    int updateBalance(@Param("advertiserId") String advertiserId, @Param("balance") java.math.BigDecimal balance);

    int deleteById(@Param("id") Long id);

    int deleteByAdvertiserId(@Param("advertiserId") String advertiserId);
}
