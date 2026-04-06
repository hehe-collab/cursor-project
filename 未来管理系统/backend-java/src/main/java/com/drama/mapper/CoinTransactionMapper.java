package com.drama.mapper;

import com.drama.entity.CoinTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CoinTransactionMapper {
    CoinTransaction selectById(@Param("id") Long id);

    int insert(CoinTransaction row);
}
