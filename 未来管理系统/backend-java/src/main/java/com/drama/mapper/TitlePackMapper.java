package com.drama.mapper;

import com.drama.entity.TitlePack;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TitlePackMapper {

    List<TitlePack> selectAllOrderByIdDesc();

    TitlePack selectById(@Param("id") int id);

    int insert(TitlePack row);

    int update(TitlePack row);

    int deleteById(@Param("id") int id);

    int deleteByIds(@Param("ids") List<Integer> ids);
}
