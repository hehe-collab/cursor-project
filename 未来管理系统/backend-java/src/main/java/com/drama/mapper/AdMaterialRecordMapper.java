package com.drama.mapper;

import com.drama.entity.AdMaterialRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdMaterialRecordMapper {

    List<AdMaterialRecord> selectAllOrderByCreatedDesc();

    int insert(AdMaterialRecord row);
}
