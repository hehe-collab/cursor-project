package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Setting;
import com.drama.service.SettingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    /**
     * 获取全部配置（可选 {@code group}：当前表无分组字段，忽略后仍返回全部）。
     */
    @GetMapping
    public Result<Map<String, String>> list(@RequestParam(required = false) String group) {
        Map<String, String> data =
                (group != null && !group.isBlank())
                        ? settingService.getFlatByGroup(group)
                        : settingService.getAllFlat();
        return Result.success(data);
    }

    @GetMapping("/{key}")
    public Result<String> one(@PathVariable("key") String key) {
        Setting row = settingService.getByKey(key);
        if (row == null) {
            return Result.error("设置不存在");
        }
        String v = row.getValue() != null ? row.getValue() : "";
        return Result.success(v);
    }

    /** 批量保存（与历史 Node {@code POST /api/settings} 一致）。 */
    @PostMapping
    public Result<Void> postBatch(@RequestBody Map<String, Object> body) {
        settingService.upsertMany(body);
        return Result.success("保存成功", null);
    }

    /** 批量更新（教程 / 指令中的 PUT）。 */
    @PutMapping
    public Result<Void> putBatch(@RequestBody Map<String, Object> body) {
        settingService.upsertMany(body);
        return Result.success("更新成功", null);
    }

    @PutMapping("/{key}")
    public Result<Void> putOne(@PathVariable("key") String key, @RequestBody Map<String, Object> body) {
        Object raw = body != null ? body.get("value") : null;
        String value = raw != null ? raw.toString() : null;
        if (value == null && (body == null || !body.containsKey("value"))) {
            return Result.error("value 参数不能为空");
        }
        if (value == null) {
            value = "";
        }
        settingService.upsertValue(key, value);
        return Result.success("更新成功", null);
    }
}
