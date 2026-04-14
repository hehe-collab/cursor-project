package com.drama.mapper;

import com.drama.entity.AdAccount;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdAccountMapper {

    List<AdAccount> selectAllOrderByIdDesc();

    /** 投放媒体：与 product 所称 platform 对应，取自 media 列（去重、小写） */
    List<String> selectDistinctPlatforms();

    List<String> selectDistinctCountries();

    List<String> selectDistinctSubjectNames();

    List<Map<String, Object>> selectExecutableAccountOptions(
            @Param("media") String media, @Param("oauthStatus") String oauthStatus);

    AdAccount selectById(@Param("id") int id);

    AdAccount selectFirstByAccountId(@Param("accountId") String accountId);

    int insert(AdAccount row);

    int update(AdAccount row);

    int deleteById(@Param("id") int id);

    int deleteByIds(@Param("ids") List<Integer> ids);
}
