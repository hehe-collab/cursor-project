package com.drama.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.drama.entity.BatchTask;
import com.drama.entity.BatchTaskItem;
import com.drama.entity.Drama;
import com.drama.entity.DramaEpisode;
import com.drama.mapper.DramaEpisodeMapper;
import com.drama.mapper.DramaMapper;
import com.drama.service.BatchTaskService;
import com.drama.service.DramaService;
import com.drama.service.OssService;
import com.drama.service.VodService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 异步处理「OSS 文件夹 → VOD UploadMediaByURL → 入库 drama_episodes」导入任务。
 *
 * <p>由 {@code POST /api/vod/import-from-oss} 创建 batch_task 后立即提交到 taskExecutor 线程池，
 * 不阻塞 HTTP 请求线程。状态推进依赖 {@link DramaEpisodeVodSyncTask}（60s 轮询）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VodOssImportProcessor {

    public static final String TASK_TYPE = "vod_oss_import";

    private final BatchTaskService batchTaskService;
    private final OssService ossService;
    private final VodService vodService;
    private final DramaEpisodeMapper dramaEpisodeMapper;
    private final DramaMapper dramaMapper;
    private final DramaService dramaService;

    @Async("taskExecutor")
    public void executeAsync(String taskId) {
        log.info("[VodOssImport] 开始处理：taskId={}", taskId);
        BatchTask task = batchTaskService.getByTaskId(taskId);
        if (task == null) {
            log.error("[VodOssImport] 任务不存在：taskId={}", taskId);
            return;
        }
        if (!"pending".equals(task.getStatus())) {
            log.warn("[VodOssImport] 状态非 pending 跳过：taskId={}, status={}", taskId, task.getStatus());
            return;
        }
        batchTaskService.markStarted(taskId);

        Map<String, Object> config;
        try {
            config = JSON.parseObject(task.getConfigJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("[VodOssImport] 解析 configJson 失败：taskId={}", taskId, e);
            batchTaskService.markCompleted(taskId, "failed", null);
            return;
        }

        Integer dramaId = toInt(config.get("dramaId"));
        if (dramaId == null || dramaId <= 0) {
            log.error("[VodOssImport] configJson 缺少 dramaId：taskId={}", taskId);
            batchTaskService.markCompleted(taskId, "failed", null);
            return;
        }
        String mode = String.valueOf(config.getOrDefault("mode", "append"));
        Long cateId = toLong(config.get("cateId"));

        Drama drama = dramaMapper.selectById(dramaId);
        if (drama == null) {
            log.error("[VodOssImport] 短剧不存在：dramaId={}", dramaId);
            batchTaskService.markCompleted(taskId, "failed", null);
            return;
        }

        if (cateId == null || cateId <= 0) {
            cateId = drama.getVodCateId();
        }

        // 兜底：cateId 仍为空但 config 带了 parentCateId，按剧名 ensure 三级子分类（幂等）
        Long parentCateId = toLong(config.get("parentCateId"));
        if ((cateId == null || cateId <= 0) && parentCateId != null && parentCateId > 0) {
            String dramaTitle = drama.getTitle();
            if (StringUtils.hasText(dramaTitle)) {
                try {
                    Long ensured = vodService.ensureCategory(parentCateId, dramaTitle.trim(), "default");
                    if (ensured != null && ensured > 0) {
                        cateId = ensured;
                        try {
                            dramaMapper.updateVodCateId(dramaId, ensured);
                            drama.setVodCateId(ensured);
                            log.info("[VodOssImport] 自动建/复用三级分类 cateId={} 已绑定 dramaId={}", ensured, dramaId);
                        } catch (Exception writeE) {
                            log.warn("[VodOssImport] 写回 dramas.vod_cate_id 失败（不影响导入继续）: dramaId={}", dramaId, writeE);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[VodOssImport] ensureCategory 失败 parentId={} dramaTitle={}（视频将归未分类）", parentCateId, dramaTitle, e);
                }
            }
        }

        if (batchTaskService.isCancelled(taskId)) {
            log.info("[VodOssImport] 任务已取消：taskId={}", taskId);
            return;
        }

        if ("replace".equalsIgnoreCase(mode)) {
            try {
                dramaEpisodeMapper.deleteByDramaId(dramaId);
                log.info("[VodOssImport] mode=replace，已清空 dramaId={} 旧分集", dramaId);
            } catch (Exception e) {
                log.error("[VodOssImport] 清空旧分集失败：dramaId={}", dramaId, e);
                batchTaskService.markCompleted(taskId, "failed", null);
                return;
            }
        }

        List<BatchTaskItem> items = batchTaskService.getItems(taskId);
        if (items == null || items.isEmpty()) {
            log.warn("[VodOssImport] 任务无 items：taskId={}", taskId);
            batchTaskService.markCompleted(taskId, "completed", JSON.toJSONString(Map.of("note", "no items")));
            return;
        }
        // 按 itemIndex 升序
        items.sort((a, b) -> {
            int ai = a.getItemIndex() == null ? 0 : a.getItemIndex();
            int bi = b.getItemIndex() == null ? 0 : b.getItemIndex();
            return Integer.compare(ai, bi);
        });

        // 计算起始 episode_num：append 模式从现有最大 +1，replace 从 1
        int startEpisodeNum = 1;
        if (!"replace".equalsIgnoreCase(mode)) {
            try {
                List<DramaEpisode> existing = dramaEpisodeMapper.selectByDramaId(dramaId);
                int max = 0;
                for (DramaEpisode ep : existing) {
                    if (ep.getEpisodeNum() != null && ep.getEpisodeNum() > max) {
                        max = ep.getEpisodeNum();
                    }
                }
                startEpisodeNum = max + 1;
            } catch (Exception ignore) {
                // 拿不到就保留 1
            }
        }

        // 准备 UploadMediaByURL 入参（生成预签名 URL）
        List<Map<String, Object>> vodItems = new ArrayList<>();
        List<ItemContext> ctxList = new ArrayList<>();
        int seq = 0;
        for (BatchTaskItem it : items) {
            ItemContext ctx = parseItem(it, startEpisodeNum + seq);
            ctxList.add(ctx);
            if (ctx.error != null) {
                seq++;
                continue;
            }
            String url;
            try {
                url = ossService.buildAccessUrl(ctx.bucket, ctx.key);
            } catch (Exception e) {
                ctx.error = "生成 OSS URL 失败：" + e.getMessage();
                seq++;
                continue;
            }
            ctx.signedUrl = url;
            Map<String, Object> vodItem = new LinkedHashMap<>();
            vodItem.put("url", url);
            // 给 VOD 的 title：剧名_集号_文件名（不含扩展名）
            String dramaTitle = drama.getTitle() != null ? drama.getTitle() : "";
            String niceTitle = (dramaTitle.isBlank() ? "" : dramaTitle + "_") + "EP" + ctx.episodeNum + "_" + ctx.fileNameNoExt;
            vodItem.put("title", niceTitle);
            if (cateId != null && cateId > 0) {
                vodItem.put("cateId", cateId);
            }
            vodItems.add(vodItem);
            seq++;
        }

        if (batchTaskService.isCancelled(taskId)) {
            log.info("[VodOssImport] 准备阶段后任务被取消：taskId={}", taskId);
            return;
        }

        // 批量提交到 VOD（内部按 10 一批）
        List<Map<String, Object>> vodResults;
        try {
            vodResults = vodItems.isEmpty()
                    ? new ArrayList<>()
                    : vodService.uploadMediaByURL(vodItems);
        } catch (Exception e) {
            log.error("[VodOssImport] UploadMediaByURL 异常：taskId={}", taskId, e);
            for (ItemContext ctx : ctxList) {
                if (ctx.error == null) ctx.error = "UploadMediaByURL 失败：" + e.getMessage();
            }
            vodResults = new ArrayList<>();
        }

        // 用 OSS key 反查 vodResults：把每个 row.sourceURL 解码后取 path 部分跟 ctx.key 比较
        // 阿里云 VOD GetURLUploadInfos 返回的 UploadURL 与提交时字符串可能不一致（OSS 预签名
        // 每次重新生成 + URL 编码差异），因此用 OSS key（路径无签名部分）作为稳定主键。
        Map<String, Map<String, Object>> resultByKey = new LinkedHashMap<>();
        for (Map<String, Object> r : vodResults) {
            String src = String.valueOf(r.getOrDefault("sourceURL", ""));
            String key = extractOssKeyFromUrl(src);
            if (!key.isEmpty()) {
                resultByKey.put(key, r);
            }
        }

        int success = 0;
        int failed = 0;
        for (ItemContext ctx : ctxList) {
            if (ctx.error != null) {
                batchTaskService.markItemFailed(taskId, ctx.itemId, ctx.error);
                failed++;
                continue;
            }
            Map<String, Object> r = resultByKey.get(ctx.key);
            String mediaId = r != null ? str(r.get("mediaId")) : "";
            String errorMsg = r != null ? str(r.get("errorMessage")) : "";
            if (mediaId.isBlank()) {
                String reason = errorMsg.isBlank() ? "VOD 未返回 mediaId" : errorMsg;
                batchTaskService.markItemFailed(taskId, ctx.itemId, reason);
                failed++;
                continue;
            }
            try {
                DramaEpisode ep = new DramaEpisode();
                ep.setDramaId(dramaId);
                ep.setEpisodeNum(ctx.episodeNum);
                ep.setTitle("第" + ctx.episodeNum + "集");
                ep.setVodVideoId(mediaId);
                ep.setVodStatus("uploading");
                ep.setVideoSize(0L);
                ep.setDuration(0);
                dramaEpisodeMapper.insert(ep);
                batchTaskService.markItemSuccess(taskId, ctx.itemId, mediaId);
                success++;
            } catch (Exception e) {
                log.error("[VodOssImport] 写 drama_episodes 失败：dramaId={}, episodeNum={}", dramaId, ctx.episodeNum, e);
                batchTaskService.markItemFailed(taskId, ctx.itemId, "写库失败：" + e.getMessage());
                failed++;
            }
        }

        Map<String, Object> resultJson = new LinkedHashMap<>();
        resultJson.put("dramaId", dramaId);
        resultJson.put("totalCount", ctxList.size());
        resultJson.put("successCount", success);
        resultJson.put("failedCount", failed);
        resultJson.put("mode", mode);

        String finalStatus = (failed == 0 && success > 0) ? "completed" : (success == 0 ? "failed" : "completed");
        batchTaskService.markCompleted(taskId, finalStatus, JSON.toJSONString(resultJson));

        try {
            dramaService.refreshDramaTaskStatus(dramaId);
        } catch (Exception e) {
            log.warn("[VodOssImport] 刷新剧任务状态失败：dramaId={}", dramaId, e);
        }
        log.info("[VodOssImport] 完成 taskId={} success={} failed={}", taskId, success, failed);
    }

    private static ItemContext parseItem(BatchTaskItem it, int episodeNum) {
        ItemContext ctx = new ItemContext();
        ctx.itemId = it.getId();
        ctx.episodeNum = episodeNum;
        try {
            Map<String, Object> data = JSON.parseObject(it.getItemData(), new TypeReference<Map<String, Object>>() {});
            ctx.bucket = String.valueOf(data.getOrDefault("bucket", ""));
            ctx.key = String.valueOf(data.getOrDefault("key", ""));
            ctx.name = String.valueOf(data.getOrDefault("name", ""));
            ctx.fileNameNoExt = stripExt(ctx.name);
            if (ctx.bucket.isBlank() || ctx.key.isBlank()) {
                ctx.error = "item 缺少 bucket / key";
            }
        } catch (Exception e) {
            ctx.error = "解析 itemData 失败：" + e.getMessage();
        }
        return ctx;
    }

    private static String stripExt(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * 从一个 URL 字符串中解出 OSS key（path 部分，去除前导 /，URLDecode）。
     *
     * <p>用于把 VOD GetURLUploadInfos 返回的 UploadURL 反归一化到原始 OSS key，便于按 key 反查。
     * 兼容：(a) 完整 https://bucket.oss-xxx.aliyuncs.com/path/file?signature=...
     * (b) 任何 java.net.URI 能解析的 URL；(c) 解析失败时退化为字符串处理。
     */
    static String extractOssKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        String rawPath;
        try {
            URI uri = URI.create(url);
            rawPath = uri.getRawPath();
            if (rawPath == null) return "";
        } catch (Exception e) {
            // 非合法 URI，退化处理：截取 :// 后第一个 / 到 ? 之间
            int schemeEnd = url.indexOf("://");
            int start = schemeEnd >= 0 ? url.indexOf('/', schemeEnd + 3) : 0;
            int q = url.indexOf('?', Math.max(start, 0));
            rawPath = (start < 0)
                    ? (q >= 0 ? url.substring(0, q) : url)
                    : (q >= 0 ? url.substring(start, q) : url.substring(start));
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = rawPath;
        }
        while (decoded.startsWith("/")) {
            decoded = decoded.substring(1);
        }
        return decoded;
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    /** 单 item 的执行上下文（避免 Map 反复 cast）。 */
    private static class ItemContext {
        Long itemId;
        int episodeNum;
        String bucket;
        String key;
        String name;
        String fileNameNoExt;
        String signedUrl;
        String error; // null 表示无错误
    }
}
