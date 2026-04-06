package com.drama.mapper;

import com.drama.entity.AdMaterial;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdMaterialMapper {

    List<AdMaterial> selectAllOrderByIdDesc();

    AdMaterial selectById(@Param("id") int id);

    int insert(AdMaterial row);

    int update(AdMaterial row);

    int deleteById(@Param("id") int id);
}
