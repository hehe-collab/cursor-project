package com.drama.mapper;

import com.drama.entity.AdAccount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdAccountMapper {

    List<AdAccount> selectAllOrderByIdDesc();

    List<String> selectDistinctCountries();

    List<String> selectDistinctSubjectNames();

    AdAccount selectById(@Param("id") int id);

    int insert(AdAccount row);

    int update(AdAccount row);

    int deleteById(@Param("id") int id);

    int deleteByIds(@Param("ids") List<Integer> ids);
}
