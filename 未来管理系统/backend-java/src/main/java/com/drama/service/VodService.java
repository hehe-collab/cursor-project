package com.drama.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.vod.model.v20170321.CreateUploadVideoRequest;
import com.aliyuncs.vod.model.v20170321.CreateUploadVideoResponse;
import com.aliyuncs.vod.model.v20170321.GetPlayInfoRequest;
import com.aliyuncs.vod.model.v20170321.GetPlayInfoResponse;
import com.aliyuncs.vod.model.v20170321.GetVideoInfoRequest;
import com.aliyuncs.vod.model.v20170321.GetVideoInfoResponse;
import com.aliyuncs.vod.model.v20170321.GetVideoPlayAuthRequest;
import com.aliyuncs.vod.model.v20170321.GetVideoPlayAuthResponse;
import com.aliyuncs.vod.model.v20170321.RefreshUploadVideoRequest;
import com.aliyuncs.vod.model.v20170321.RefreshUploadVideoResponse;
import com.drama.exception.BusinessException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class VodService {

    @Value("${aliyun.vod.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.vod.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.vod.region-id:cn-shanghai}")
    private String regionId;

    @Value("${aliyun.vod.template-group-id:}")
    private String templateGroupId;

    @Value("${aliyun.vod.storage-location:}")
    private String storageLocation;

    @Value("${aliyun.vod.domain:}")
    private String domain;

    private IAcsClient clientOrThrow() {
        if (!isConfigured()) {
            throw new BusinessException(500, "请先配置阿里云 VOD 的 AccessKey");
        }
        try {
            DefaultProfile profile =
                    DefaultProfile.getProfile(regionId, accessKeyId.trim(), accessKeySecret.trim());
            return new DefaultAcsClient(profile);
        } catch (Exception e) {
            throw new BusinessException(500, "初始化 VOD 客户端失败：" + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret);
    }

    public String getRegionId() {
        return StringUtils.hasText(regionId) ? regionId.trim() : "cn-shanghai";
    }

    public Map<String, Object> createUploadAuth(String title, String fileName) {
        IAcsClient client = clientOrThrow();
        try {
            CreateUploadVideoRequest request = new CreateUploadVideoRequest();
            request.setTitle(StringUtils.hasText(title) ? title.trim() : fileName);
            request.setFileName(fileName);
            if (StringUtils.hasText(templateGroupId)) {
                request.setTemplateGroupId(templateGroupId.trim());
            }
            if (StringUtils.hasText(storageLocation)) {
                request.setStorageLocation(storageLocation.trim());
            }
            CreateUploadVideoResponse res = client.getAcsResponse(request);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("videoId", res.getVideoId());
            data.put("uploadAuth", res.getUploadAuth());
            data.put("uploadAddress", res.getUploadAddress());
            data.put("regionId", regionId);
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "获取上传凭证失败"));
        }
    }

    public Map<String, Object> refreshUploadAuth(String videoId) {
        IAcsClient client = clientOrThrow();
        try {
            RefreshUploadVideoRequest request = new RefreshUploadVideoRequest();
            request.setVideoId(videoId);
            RefreshUploadVideoResponse res = client.getAcsResponse(request);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("videoId", res.getVideoId());
            data.put("uploadAuth", res.getUploadAuth());
            data.put("uploadAddress", res.getUploadAddress());
            data.put("regionId", regionId);
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "刷新凭证失败"));
        }
    }

    public Map<String, Object> getPlayAuth(String videoId) {
        IAcsClient client = clientOrThrow();
        try {
            GetVideoPlayAuthRequest request = new GetVideoPlayAuthRequest();
            request.setVideoId(videoId);
            GetVideoPlayAuthResponse res = client.getAcsResponse(request);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("playAuth", res.getPlayAuth());
            data.put("videoMeta", res.getVideoMeta());
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "获取播放凭证失败"));
        }
    }

    public Map<String, Object> getPlayUrl(String videoId) {
        IAcsClient client = clientOrThrow();
        try {
            GetPlayInfoRequest request = new GetPlayInfoRequest();
            request.setVideoId(videoId);
            GetPlayInfoResponse response = client.getAcsResponse(request);
            List<Map<String, Object>> playInfoList = extractPlayInfoList(response.getPlayInfoList());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("videoId", videoId);
            data.put("playInfoList", playInfoList);
            data.put("playUrl", choosePreferredPlayUrl(playInfoList));
            data.put("videoBase", response.getVideoBase());
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "获取播放地址失败"));
        }
    }

    public String getPreferredPlayUrl(String videoId) {
        Object playUrl = getPlayUrl(videoId).get("playUrl");
        return playUrl != null ? String.valueOf(playUrl) : "";
    }

    public Map<String, Object> getVideoInfo(String videoId) {
        IAcsClient client = clientOrThrow();
        try {
            GetVideoInfoRequest request = new GetVideoInfoRequest();
            request.setVideoId(videoId);
            GetVideoInfoResponse response = client.getAcsResponse(request);
            Object video = response.getVideo();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("videoId", firstNonBlank(invokeString(video, "getVideoId"), videoId));
            data.put("title", invokeString(video, "getTitle"));
            String rawStatus = invokeString(video, "getStatus");
            data.put("status", normalizeStatus(rawStatus));
            data.put("rawStatus", rawStatus);
            data.put("duration", toInteger(invoke(video, "getDuration")));
            data.put("size", toLong(invoke(video, "getSize")));
            data.put(
                    "coverUrl",
                    firstNonBlank(invokeString(video, "getCoverURL"), invokeString(video, "getCoverUrl")));
            data.put("createTime", invoke(video, "getCreationTime"));
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "获取视频信息失败"));
        }
    }

    private List<Map<String, Object>> extractPlayInfoList(List<?> rawList) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rawList == null) {
            return out;
        }
        for (Object item : rawList) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("format", invokeString(item, "getFormat"));
            row.put("definition", invokeString(item, "getDefinition"));
            row.put("duration", toInteger(invoke(item, "getDuration")));
            row.put("size", toLong(invoke(item, "getSize")));
            row.put("encrypt", invokeString(item, "getEncrypt"));
            row.put("playUrl", rewriteDomain(firstNonBlank(
                    invokeString(item, "getPlayURL"), invokeString(item, "getPlayUrl"))));
            out.add(row);
        }
        return out;
    }

    private String choosePreferredPlayUrl(List<Map<String, Object>> playInfoList) {
        String firstUrl = "";
        for (Map<String, Object> item : playInfoList) {
            String playUrl = str(item.get("playUrl"));
            if (!StringUtils.hasText(playUrl)) {
                continue;
            }
            if (firstUrl.isBlank()) {
                firstUrl = playUrl;
            }
            String format = str(item.get("format"));
            if ("m3u8".equalsIgnoreCase(format)) {
                return playUrl;
            }
        }
        return firstUrl;
    }

    private String rewriteDomain(String playUrl) {
        if (!StringUtils.hasText(playUrl) || !StringUtils.hasText(domain)) {
            return playUrl;
        }
        try {
            URI source = URI.create(playUrl);
            URI targetDomain = URI.create(
                    domain.startsWith("http://") || domain.startsWith("https://")
                            ? domain
                            : "https://" + domain);
            return new URI(
                            targetDomain.getScheme(),
                            source.getUserInfo(),
                            targetDomain.getHost(),
                            targetDomain.getPort(),
                            source.getPath(),
                            source.getQuery(),
                            source.getFragment())
                    .toString();
        } catch (Exception e) {
            log.warn("rewrite vod domain failed: {}", e.getMessage());
            return playUrl;
        }
    }

    private String normalizeStatus(String rawStatus) {
        String value = str(rawStatus).trim();
        if (!StringUtils.hasText(value)) {
            return "manual";
        }
        String lower = value.toLowerCase();
        if ("normal".equals(lower) || "completed".equals(lower) || "success".equals(lower)) {
            return "normal";
        }
        if ("failed".equals(lower)
                || "error".equals(lower)
                || "uploadfail".equals(lower)
                || "transcodefail".equals(lower)
                || "blocked".equals(lower)
                || "illegal".equals(lower)) {
            return "failed";
        }
        if ("uploading".equals(lower) || "upload".equals(lower)) {
            return "uploading";
        }
        if ("transcoding".equals(lower)
                || "processing".equals(lower)
                || "transcode".equals(lower)
                || "snapshotting".equals(lower)) {
            return "transcoding";
        }
        return lower;
    }

    private Object invoke(Object target, String methodName) {
        if (target == null || !StringUtils.hasText(methodName)) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private String invokeString(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value != null ? String.valueOf(value) : "";
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return StringUtils.hasText(str(value)) ? Integer.parseInt(str(value)) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return StringUtils.hasText(str(value)) ? Long.parseLong(str(value)) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String msg(Exception e, String fallback) {
        return e.getMessage() != null ? e.getMessage() : fallback;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
