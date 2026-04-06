package com.drama.service;

import com.drama.entity.Setting;
import com.drama.exception.BusinessException;
import com.drama.mapper.SettingMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingMapper settingMapper;

    /**
     * 扁平 key → value（字符串），与历史 Node {@code GET /api/settings} 一致。
     */
    public Map<String, String> getAllFlat() {
        List<Setting> rows = settingMapper.selectAll();
        Map<String, String> m = new LinkedHashMap<>();
        for (Setting s : rows) {
            if (s.getKeyName() != null) {
                m.put(s.getKeyName(), s.getValue() != null ? s.getValue() : "");
            }
        }
        return m;
    }

    /**
     * 库表无分组列：{@code group} 非空时仍返回全部，避免误用时报空。
     */
    public Map<String, String> getFlatByGroup(String group) {
        return getAllFlat();
    }

    public Setting getByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return settingMapper.selectByKeyName(key.trim());
    }

    @Transactional
    public void upsertValue(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(400, "配置键不能为空");
        }
        String v = value != null ? value : "";
        settingMapper.upsert(key.trim(), v);
    }

    /**
     * 批量 upsert：Body 为扁平对象，如 {@code {"site_name":"x","icp":"y"}}。
     */
    @Transactional
    public void upsertMany(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> e : body.entrySet()) {
            String k = e.getKey();
            if (k == null || k.isBlank()) {
                continue;
            }
            settingMapper.upsert(k.trim(), stringify(e.getValue()));
        }
    }

    private static String stringify(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Number || o instanceof Boolean) {
            return Objects.toString(o);
        }
        return Objects.toString(o);
    }
}
