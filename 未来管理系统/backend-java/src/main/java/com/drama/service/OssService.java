package com.drama.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.drama.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 阿里云 OSS 服务（用于「OSS 文件夹扫描 → VOD 拉取」导入功能）。
 *
 * <p>共用 VodService 的 AccessKey（同一个 RAM 子账号 svc-vod-for-backend）。
 * 该子账号需要追加 oss:ListObjects + oss:GetObject 两项权限。
 */
@Slf4j
@Service
public class OssService {

    /** 视频文件后缀白名单（小写）。 */
    private static final Set<String> VIDEO_EXTS =
            Set.of("mp4", "mov", "m3u8", "mpd", "flv", "mkv", "ts", "webm", "avi", "wmv");

    /** 与 VodService 共用同一对 AK（svc-vod-for-backend）。 */
    @Value("${aliyun.vod.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.vod.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket:}")
    private String defaultBucket;

    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;

    @Value("${aliyun.oss.region:}")
    private String region;

    @Value("${aliyun.oss.bucket-private:true}")
    private boolean bucketPrivate;

    @Value("${aliyun.oss.presign-expire-sec:21600}")
    private int presignExpireSec;

    private volatile OSS client;

    public boolean isConfigured() {
        return StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(accessKeySecret)
                && StringUtils.hasText(endpoint)
                && StringUtils.hasText(defaultBucket);
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public boolean isBucketPrivate() {
        return bucketPrivate;
    }

    public int getPresignExpireSec() {
        return presignExpireSec;
    }

    private OSS clientOrThrow() {
        if (!isConfigured()) {
            throw new BusinessException(
                    500,
                    "请先配置阿里云 OSS（ALIYUN_OSS_BUCKET / ALIYUN_OSS_ENDPOINT 等环境变量）");
        }
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        client =
                                new OSSClientBuilder()
                                        .build(
                                                endpoint.trim(),
                                                accessKeyId.trim(),
                                                accessKeySecret.trim());
                    } catch (Exception e) {
                        throw new BusinessException(500, "初始化 OSS 客户端失败：" + e.getMessage());
                    }
                }
            }
        }
        return client;
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception ignore) {
                // ignore
            }
            client = null;
        }
    }

    /**
     * 解析智能 OSS 路径。
     *
     * <ul>
     *   <li>{@code oss://bucket/path/to/folder/} → 跨 bucket（用指定 bucket）
     *   <li>{@code path/to/folder/} 或 {@code /path/to/folder/} → 用默认 bucket
     *   <li>空字符串 → prefix=""，扫 bucket 根
     * </ul>
     */
    public ParsedOssPath parsePath(String input) {
        String raw = input == null ? "" : input.trim();
        String bucket = defaultBucket;
        String prefix;
        if (raw.startsWith("oss://")) {
            String stripped = raw.substring("oss://".length());
            int slash = stripped.indexOf('/');
            if (slash < 0) {
                bucket = stripped;
                prefix = "";
            } else {
                bucket = stripped.substring(0, slash);
                prefix = stripped.substring(slash + 1);
            }
        } else {
            prefix = raw;
        }
        // prefix 不允许以 / 开头
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        // prefix 用于"文件夹"语义时建议以 / 结尾；但允许用户传文件路径前缀
        return new ParsedOssPath(bucket, prefix);
    }

    /**
     * 列指定 prefix 下的视频文件，只扫一层（不递归子文件夹）。
     *
     * @return 文件列表（已按文件名排序），不包含目录占位
     */
    public List<Map<String, Object>> listVideosOneLevel(String bucket, String prefix) {
        if (!StringUtils.hasText(bucket)) {
            throw new BusinessException(400, "bucket 不能为空");
        }
        OSS oss = clientOrThrow();
        // 规范 prefix：若用户传的是文件夹但缺尾 /，自动补
        String normalizedPrefix = prefix == null ? "" : prefix;
        if (StringUtils.hasText(normalizedPrefix) && !normalizedPrefix.endsWith("/")) {
            // 看起来像文件路径就不补，看起来像目录（不含 .）才补
            int lastSlash = normalizedPrefix.lastIndexOf('/');
            String tail = lastSlash >= 0 ? normalizedPrefix.substring(lastSlash + 1) : normalizedPrefix;
            if (!tail.contains(".")) {
                normalizedPrefix = normalizedPrefix + "/";
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String continuationToken = null;
            do {
                ListObjectsV2Request req = new ListObjectsV2Request(bucket);
                req.setPrefix(normalizedPrefix);
                req.setDelimiter("/"); // 只扫一层
                req.setMaxKeys(1000);
                if (continuationToken != null) {
                    req.setContinuationToken(continuationToken);
                }
                ListObjectsV2Result res = oss.listObjectsV2(req);
                List<OSSObjectSummary> summaries = res.getObjectSummaries();
                if (summaries != null) {
                    for (OSSObjectSummary s : summaries) {
                        String key = s.getKey();
                        if (key == null || key.endsWith("/")) {
                            continue; // 目录占位
                        }
                        if (!isVideoExt(key)) {
                            continue;
                        }
                        Map<String, Object> file = new LinkedHashMap<>();
                        file.put("key", key);
                        file.put("name", baseName(key));
                        file.put("size", s.getSize());
                        file.put("sizeText", humanSize(s.getSize()));
                        Date lm = s.getLastModified();
                        file.put("lastModified", lm != null ? lm.getTime() : null);
                        file.put("etag", s.getETag());
                        result.add(file);
                    }
                }
                continuationToken = res.isTruncated() ? res.getNextContinuationToken() : null;
            } while (continuationToken != null);
        } catch (Exception e) {
            throw new BusinessException(500, "扫描 OSS 失败：" + e.getMessage());
        }
        // 按文件名 localeCompare-ish（Java Collator 区分大小写不太准，先简单用 compareTo）
        result.sort(
                (a, b) ->
                        String.valueOf(a.get("name"))
                                .compareToIgnoreCase(String.valueOf(b.get("name"))));
        return result;
    }

    /**
     * 为 object 生成可被 VOD 跨账号/跨服务拉取的 URL。
     *
     * <p>私有 bucket 返回预签名 URL（带签名 + 过期时间）；公共读直接返回 https://bucket.endpoint/key。
     */
    public String buildAccessUrl(String bucket, String key) {
        if (!StringUtils.hasText(bucket) || !StringUtils.hasText(key)) {
            throw new BusinessException(400, "bucket / key 不能为空");
        }
        if (bucketPrivate) {
            return presignGetUrl(bucket, key, presignExpireSec);
        }
        return "https://" + bucket + "." + endpoint + "/" + key;
    }

    public String presignGetUrl(String bucket, String key, int expireSec) {
        OSS oss = clientOrThrow();
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSec * 1000L);
            GeneratePresignedUrlRequest req =
                    new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            req.setExpiration(expiration);
            URL url = oss.generatePresignedUrl(req);
            return url.toString();
        } catch (Exception e) {
            throw new BusinessException(500, "生成 OSS 预签名 URL 失败：" + e.getMessage());
        }
    }

    private static boolean isVideoExt(String key) {
        int dot = key.lastIndexOf('.');
        if (dot < 0 || dot == key.length() - 1) {
            return false;
        }
        return VIDEO_EXTS.contains(key.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String baseName(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb);
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }

    public static class ParsedOssPath {
        public final String bucket;
        public final String prefix;

        public ParsedOssPath(String bucket, String prefix) {
            this.bucket = bucket;
            this.prefix = prefix;
        }
    }
}
