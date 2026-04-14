package com.drama.mapper;

import com.drama.entity.AdMaterial;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdMaterialMapper {

    List<AdMaterial> selectAllOrderByIdDesc();

    List<Map<String, Object>> selectDistinctAccountOptions();

    AdMaterial selectById(@Param("id") int id);

    AdMaterial selectByMaterialId(@Param("materialId") String materialId);

    int insert(AdMaterial row);

    int update(AdMaterial row);

    int deleteById(@Param("id") int id);
}
