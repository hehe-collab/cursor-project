package com.drama.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.vod.model.v20170321.AddCategoryRequest;
import com.aliyuncs.vod.model.v20170321.AddCategoryResponse;
import com.aliyuncs.vod.model.v20170321.CreateUploadVideoRequest;
import com.aliyuncs.vod.model.v20170321.CreateUploadVideoResponse;
import com.aliyuncs.vod.model.v20170321.GetCategoriesRequest;
import com.aliyuncs.vod.model.v20170321.GetCategoriesResponse;
import com.aliyuncs.vod.model.v20170321.GetPlayInfoRequest;
import com.aliyuncs.vod.model.v20170321.GetPlayInfoResponse;
import com.aliyuncs.vod.model.v20170321.GetURLUploadInfosRequest;
import com.aliyuncs.vod.model.v20170321.GetURLUploadInfosResponse;
import com.aliyuncs.vod.model.v20170321.GetVideoInfoRequest;
import com.aliyuncs.vod.model.v20170321.GetVideoInfoResponse;
import com.aliyuncs.vod.model.v20170321.GetVideoPlayAuthRequest;
import com.aliyuncs.vod.model.v20170321.GetVideoPlayAuthResponse;
import com.aliyuncs.vod.model.v20170321.RefreshUploadVideoRequest;
import com.aliyuncs.vod.model.v20170321.RefreshUploadVideoResponse;
import com.aliyuncs.vod.model.v20170321.UploadMediaByURLRequest;
import com.aliyuncs.vod.model.v20170321.UploadMediaByURLResponse;
import com.alibaba.fastjson2.JSON;
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

    public Map<String, Object> createUploadAuth(String title, String fileName, Long cateId) {
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
            if (cateId != null && cateId > 0) {
                request.setCateId(cateId);
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

    /** 列阿里云 VOD 分类（按 parentId）。parentId=-1 表示根分类。 */
    public Map<String, Object> listCategories(Long parentId, String type, int pageNo, int pageSize) {
        IAcsClient client = clientOrThrow();
        try {
            GetCategoriesRequest request = new GetCategoriesRequest();
            if (parentId != null) {
                request.setCateId(parentId);
            }
            if (StringUtils.hasText(type)) {
                request.setType(type.trim());
            }
            request.setPageNo((long) Math.max(pageNo, 1));
            request.setPageSize((long) Math.min(Math.max(pageSize, 1), 100));
            GetCategoriesResponse res = client.getAcsResponse(request);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("subTotal", res.getSubTotal());
            // 当前分类自身（parentId=-1 时为 null）
            GetCategoriesResponse.Category1 self = res.getCategory1();
            if (self != null) {
                Map<String, Object> selfMap = new LinkedHashMap<>();
                selfMap.put("cateId", self.getCateId());
                selfMap.put("cateName", self.getCateName());
                selfMap.put("parentId", self.getParentId());
                selfMap.put("level", self.getLevel());
                selfMap.put("type", self.getType());
                data.put("self", selfMap);
            }
            List<Map<String, Object>> children = new ArrayList<>();
            List<GetCategoriesResponse.Category> subs = res.getSubCategories();
            if (subs != null) {
                for (GetCategoriesResponse.Category c : subs) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cateId", c.getCateId());
                    m.put("cateName", c.getCateName());
                    m.put("parentId", c.getParentId());
                    m.put("level", c.getLevel());
                    m.put("subTotal", c.getSubTotal());
                    m.put("type", c.getType());
                    children.add(m);
                }
            }
            data.put("children", children);
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "获取 VOD 分类失败"));
        }
    }

    /** 在指定父分类下新建子分类，返回新分类信息。 */
    public Map<String, Object> addCategory(Long parentId, String cateName, String type) {
        if (parentId == null) {
            throw new BusinessException(400, "parentId 不能为空");
        }
        if (!StringUtils.hasText(cateName)) {
            throw new BusinessException(400, "cateName 不能为空");
        }
        IAcsClient client = clientOrThrow();
        try {
            AddCategoryRequest request = new AddCategoryRequest();
            request.setParentId(parentId);
            request.setCateName(cateName.trim());
            if (StringUtils.hasText(type)) {
                request.setType(type.trim());
            }
            AddCategoryResponse res = client.getAcsResponse(request);
            AddCategoryResponse.Category cat = res.getCategory();
            Map<String, Object> data = new LinkedHashMap<>();
            if (cat != null) {
                data.put("cateId", cat.getCateId());
                data.put("cateName", cat.getCateName());
                data.put("parentId", cat.getParentId());
                data.put("level", cat.getLevel());
                data.put("type", cat.getType());
            }
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "新建 VOD 分类失败"));
        }
    }

    /**
     * 在指定父分类下保证存在某个名字的子分类，返回该子分类的 cateId。
     * 已存在则直接复用；不存在则新建。
     * 用于"以剧名建子分类"场景，幂等。
     */
    public Long ensureCategory(Long parentId, String cateName, String type) {
        if (parentId == null || parentId <= 0) {
            throw new BusinessException(400, "parentId 不能为空");
        }
        if (!StringUtils.hasText(cateName)) {
            throw new BusinessException(400, "cateName 不能为空");
        }
        String trimmed = cateName.trim();
        // 翻完所有子分类找同名（按需翻页，单页最大 100）
        long pageNo = 1;
        long pageSize = 100;
        while (true) {
            Map<String, Object> page = listCategories(parentId, type, (int) pageNo, (int) pageSize);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) page.get("children");
            if (children != null) {
                for (Map<String, Object> c : children) {
                    if (trimmed.equalsIgnoreCase(String.valueOf(c.get("cateName")))) {
                        Object id = c.get("cateId");
                        if (id instanceof Number n) return n.longValue();
                    }
                }
            }
            Object subTotalObj = page.get("subTotal");
            long subTotal = subTotalObj instanceof Number n ? n.longValue() : 0L;
            if (children == null || children.size() < pageSize || pageNo * pageSize >= subTotal) {
                break;
            }
            pageNo++;
        }
        // 没找到，新建
        Map<String, Object> created = addCategory(parentId, trimmed, type);
        Object id = created.get("cateId");
        if (id instanceof Number n) return n.longValue();
        throw new BusinessException(500, "新建分类后未拿到 cateId");
    }

    /**
     * 通过 URL 拉取媒资到 VOD（异步）。每个 URL 对应阿里云 VOD 的一个 UploadJob。
     *
     * <p>单次最多 20 个；多于 20 自动拆批。返回全部 job 的拉取信息（含 mediaId）。
     *
     * @param items 每个元素需含 url(String) / title(String) / cateId(Long, 可选)
     * @return List&lt;Map&gt;：{ sourceURL, jobId, mediaId, status, errorCode, errorMessage }
     */
    public List<Map<String, Object>> uploadMediaByURL(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        IAcsClient client = clientOrThrow();
        List<Map<String, Object>> all = new ArrayList<>();
        // 阿里云单次接口限制 ≤ 20 条；这里按 10 一批留余量
        final int batchSize = 10;
        for (int from = 0; from < items.size(); from += batchSize) {
            int to = Math.min(from + batchSize, items.size());
            List<Map<String, Object>> batch = items.subList(from, to);
            try {
                UploadMediaByURLRequest request = new UploadMediaByURLRequest();
                List<String> urls = new ArrayList<>(batch.size());
                List<Map<String, Object>> metas = new ArrayList<>(batch.size());
                for (Map<String, Object> it : batch) {
                    String url = String.valueOf(it.getOrDefault("url", ""));
                    String title = String.valueOf(it.getOrDefault("title", ""));
                    Object cateIdObj = it.get("cateId");
                    urls.add(url);
                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("SourceUrl", url);
                    meta.put("Title", title);
                    if (cateIdObj instanceof Number n && n.longValue() > 0) {
                        meta.put("CateId", n.longValue());
                    }
                    metas.add(meta);
                }
                request.setUploadURLs(String.join(",", urls));
                request.setUploadMetadatas(JSON.toJSONString(metas));
                if (StringUtils.hasText(storageLocation)) {
                    request.setStorageLocation(storageLocation.trim());
                }
                if (StringUtils.hasText(templateGroupId)) {
                    request.setTemplateGroupId(templateGroupId.trim());
                }
                UploadMediaByURLResponse res = client.getAcsResponse(request);
                List<UploadMediaByURLResponse.UploadJob> jobs = res.getUploadJobs();
                List<String> jobIds = new ArrayList<>();
                Map<String, String> sourceByJob = new LinkedHashMap<>();
                if (jobs != null) {
                    for (UploadMediaByURLResponse.UploadJob j : jobs) {
                        jobIds.add(j.getJobId());
                        sourceByJob.put(j.getJobId(), j.getSourceURL());
                    }
                }
                // 立刻查一次拿 mediaId（job 创建后即可查；status 通常为 Pending/Analysing）
                List<Map<String, Object>> infos = jobIds.isEmpty()
                        ? new ArrayList<>()
                        : getURLUploadInfos(jobIds);
                if (infos.isEmpty()) {
                    for (Map.Entry<String, String> e : sourceByJob.entrySet()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("sourceURL", e.getValue());
                        row.put("jobId", e.getKey());
                        row.put("mediaId", "");
                        row.put("status", "Pending");
                        all.add(row);
                    }
                } else {
                    all.addAll(infos);
                }
            } catch (Exception e) {
                throw new BusinessException(500, msg(e, "调用 UploadMediaByURL 失败"));
            }
        }
        return all;
    }

    /** 查询 URL 上传任务的当前信息（含 mediaId / status / errorCode）。 */
    public List<Map<String, Object>> getURLUploadInfos(List<String> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return new ArrayList<>();
        }
        IAcsClient client = clientOrThrow();
        try {
            GetURLUploadInfosRequest request = new GetURLUploadInfosRequest();
            request.setJobIds(String.join(",", jobIds));
            GetURLUploadInfosResponse res = client.getAcsResponse(request);
            List<Map<String, Object>> out = new ArrayList<>();
            List<GetURLUploadInfosResponse.UrlUploadJobInfoDTO> list = res.getURLUploadInfoList();
            if (list != null) {
                for (GetURLUploadInfosResponse.UrlUploadJobInfoDTO info : list) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("jobId", info.getJobId());
                    row.put("mediaId", info.getMediaId());
                    row.put("sourceURL", info.getUploadURL());
                    row.put("status", info.getStatus());
                    row.put("errorCode", info.getErrorCode());
                    row.put("errorMessage", info.getErrorMessage());
                    row.put("fileSize", info.getFileSize());
                    out.add(row);
                }
            }
            return out;
        } catch (Exception e) {
            throw new BusinessException(500, msg(e, "查询 URL 上传任务失败"));
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
                || "snapshotting".equals(lower)
                || "uploadsucc".equals(lower)
                || "uploadsuccess".equals(lower)
                || "checking".equals(lower)
                || "reviewing".equals(lower)) {
            return "transcoding";
        }
        if ("deleted".equals(lower)) {
            return "failed";
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
