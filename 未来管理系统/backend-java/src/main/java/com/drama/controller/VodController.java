package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.BatchTask;
import com.drama.entity.BatchTaskItem;
import com.drama.exception.BusinessException;
import com.drama.service.BatchTaskService;
import com.drama.service.VodService;
import com.drama.task.VodOssImportProcessor;
import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "阿里云VOD", description = "阿里云视频点播上传/播放凭证")
@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
public class VodController {

    private final VodService vodService;
    private final BatchTaskService batchTaskService;
    private final VodOssImportProcessor vodOssImportProcessor;

    @Operation(summary = "获取上传凭证", description = "获取阿里云VOD视频上传凭证")
    @GetMapping("/upload-auth")
    public Result<Map<String, Object>> uploadAuth(
            @Parameter(description = "视频标题") @RequestParam String title,
            @Parameter(description = "文件名") @RequestParam String fileName,
            @Parameter(description = "VOD 分类 ID（可选）") @RequestParam(required = false) Long cateId) {
        return Result.success(vodService.createUploadAuth(title, fileName, cateId));
    }

    @Operation(summary = "获取上传凭证", description = "POST 方式获取阿里云 VOD 视频上传凭证")
    @PostMapping("/upload-auth")
    public Result<Map<String, Object>> uploadAuthPost(@RequestBody Map<String, Object> body) {
        Long cateId = null;
        Object raw = body.get("cateId");
        if (raw instanceof Number n) {
            cateId = n.longValue();
        } else if (raw != null && !String.valueOf(raw).isBlank()) {
            try {
                cateId = Long.parseLong(String.valueOf(raw).trim());
            } catch (NumberFormatException ignore) {
            }
        }
        return Result.success(
                vodService.createUploadAuth(
                        String.valueOf(body.getOrDefault("title", "")),
                        String.valueOf(body.getOrDefault("fileName", "")),
                        cateId));
    }

    @Operation(summary = "列阿里云 VOD 分类", description = "按 parentId 拉子分类；parentId=-1 表示根分类")
    @GetMapping("/categories")
    public Result<Map<String, Object>> listCategories(
            @Parameter(description = "父分类 ID，-1 表示根") @RequestParam(required = false, defaultValue = "-1") Long parentId,
            @Parameter(description = "分类类型，默认 default") @RequestParam(required = false, defaultValue = "default") String type,
            @Parameter(description = "页码，从 1 开始") @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页大小，最大 100") @RequestParam(required = false, defaultValue = "50") Integer pageSize) {
        return Result.success(vodService.listCategories(parentId, type, pageNo, pageSize));
    }

    @Operation(summary = "新建 VOD 子分类", description = "在指定父分类下创建子分类（重名时阿里云会拒绝，建议用 /ensure 接口）")
    @PostMapping("/categories")
    public Result<Map<String, Object>> addCategory(@RequestBody Map<String, Object> body) {
        Long parentId = null;
        Object pid = body.get("parentId");
        if (pid instanceof Number n) {
            parentId = n.longValue();
        } else if (pid != null && !String.valueOf(pid).isBlank()) {
            try {
                parentId = Long.parseLong(String.valueOf(pid).trim());
            } catch (NumberFormatException ignore) {
            }
        }
        String cateName = String.valueOf(body.getOrDefault("cateName", ""));
        String type = String.valueOf(body.getOrDefault("type", "default"));
        return Result.success(vodService.addCategory(parentId, cateName, type));
    }

    @Operation(summary = "保证子分类存在", description = "在父分类下查找同名子分类；不存在则新建。幂等。返回 {cateId}")
    @PostMapping("/categories/ensure")
    public Result<Map<String, Object>> ensureCategory(@RequestBody Map<String, Object> body) {
        Long parentId = null;
        Object pid = body.get("parentId");
        if (pid instanceof Number n) {
            parentId = n.longValue();
        } else if (pid != null && !String.valueOf(pid).isBlank()) {
            try {
                parentId = Long.parseLong(String.valueOf(pid).trim());
            } catch (NumberFormatException ignore) {
            }
        }
        String cateName = String.valueOf(body.getOrDefault("cateName", ""));
        String type = String.valueOf(body.getOrDefault("type", "default"));
        Long cateId = vodService.ensureCategory(parentId, cateName, type);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cateId", cateId);
        data.put("cateName", cateName);
        data.put("parentId", parentId);
        return Result.success(data);
    }

    @Operation(summary = "刷新上传凭证", description = "刷新阿里云VOD视频上传凭证")
    @GetMapping("/refresh-upload-auth")
    public Result<Map<String, Object>> refreshUploadAuth(@Parameter(description = "视频ID") @RequestParam String videoId) {
        return Result.success(vodService.refreshUploadAuth(videoId));
    }

    @Operation(summary = "获取 VOD 公共配置", description = "返回前端可安全使用的 VOD 公共配置")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", vodService.isConfigured());
        data.put("regionId", vodService.getRegionId());
        return Result.success(data);
    }

    @Operation(summary = "获取播放凭证", description = "获取阿里云VOD视频播放凭证")
    @GetMapping("/play-auth/{videoId}")
    public Result<Map<String, Object>> playAuth(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getPlayAuth(videoId));
    }

    @Operation(summary = "获取播放地址", description = "获取阿里云 VOD 可播放地址，优先返回 HLS")
    @GetMapping("/play-url/{videoId}")
    public Result<Map<String, Object>> playUrl(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getPlayUrl(videoId));
    }

    @Operation(summary = "获取视频信息", description = "获取阿里云 VOD 视频状态、时长、封面等信息")
    @GetMapping("/info/{videoId}")
    public Result<Map<String, Object>> info(@PathVariable("videoId") String videoId) {
        return Result.success(vodService.getVideoInfo(videoId));
    }

    /**
     * 从 OSS 文件夹导入视频到指定剧。前端先调 POST /api/oss/scan-videos 拿文件清单，
     * 用户调序/勾选后传给该接口；后端创建 batch_task，立即异步处理（不阻塞 HTTP）。
     *
     * <p>请求体示例：
     * <pre>{
     *   "dramaId": 513,
     *   "cateId": 12345,            // 可选，不传时使用 dramas.vod_cate_id
     *   "mode": "append",           // append（默认）| replace
     *   "files": [                   // 顺序即 episode_num 的相对顺序
     *     { "bucket": "myb", "key": "dramas/show1/01.mp4", "name": "01.mp4" }
     *   ]
     * }</pre>
     */
    @Operation(summary = "从 OSS 导入剧集", description = "扫描结果调序后提交：异步生成预签名 URL → VOD UploadMediaByURL → 入库 drama_episodes")
    @PostMapping("/import-from-oss")
    public Result<Map<String, Object>> importFromOss(
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "adminId", required = false) Integer adminId,
            @RequestAttribute(value = "adminName", required = false) String adminName) {
        Object dramaIdObj = body.get("dramaId");
        Integer dramaId = null;
        if (dramaIdObj instanceof Number n) {
            dramaId = n.intValue();
        } else if (dramaIdObj != null) {
            try {
                dramaId = Integer.parseInt(String.valueOf(dramaIdObj).trim());
            } catch (NumberFormatException ignore) {
            }
        }
        if (dramaId == null || dramaId <= 0) {
            throw new BusinessException(400, "dramaId 不能为空");
        }
        Object filesObj = body.get("files");
        if (!(filesObj instanceof List<?>) || ((List<?>) filesObj).isEmpty()) {
            throw new BusinessException(400, "files 不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> files = (List<Map<String, Object>>) filesObj;

        Long cateId = parseLong(body.get("cateId"));
        Long parentCateId = parseLong(body.get("parentCateId"));
        String mode = String.valueOf(body.getOrDefault("mode", "append"));

        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("dramaId", dramaId);
        configMap.put("cateId", cateId);
        configMap.put("parentCateId", parentCateId);
        configMap.put("mode", mode);
        configMap.put("source", "oss");

        List<BatchTaskItem> items = new ArrayList<>();
        int idx = 0;
        for (Map<String, Object> f : files) {
            BatchTaskItem item = new BatchTaskItem();
            item.setItemIndex(idx++);
            item.setStage("vod-import");
            item.setItemData(JSON.toJSONString(f));
            items.add(item);
        }

        BatchTask task = batchTaskService.createTask(
                VodOssImportProcessor.TASK_TYPE,
                adminId,
                adminName,
                null,
                configMap,
                items);
        vodOssImportProcessor.executeAsync(task.getTaskId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", task.getTaskId());
        data.put("totalCount", items.size());
        data.put("dramaId", dramaId);
        return Result.success(data);
    }

    private static Long parseLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
