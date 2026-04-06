package com.drama.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.vod.model.v20170321.CreateUploadVideoRequest;
import com.aliyuncs.vod.model.v20170321.CreateUploadVideoResponse;
import com.aliyuncs.vod.model.v20170321.GetVideoPlayAuthRequest;
import com.aliyuncs.vod.model.v20170321.GetVideoPlayAuthResponse;
import com.aliyuncs.vod.model.v20170321.RefreshUploadVideoRequest;
import com.aliyuncs.vod.model.v20170321.RefreshUploadVideoResponse;
import com.drama.exception.BusinessException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VodService {

    @Value("${aliyun.vod.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.vod.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.vod.region-id:cn-shanghai}")
    private String regionId;

    private IAcsClient clientOrNull() {
        if (accessKeyId == null
                || accessKeyId.isBlank()
                || accessKeySecret == null
                || accessKeySecret.isBlank()) {
            return null;
        }
        try {
            DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId.trim(), accessKeySecret.trim());
            return new DefaultAcsClient(profile);
        } catch (Exception e) {
            throw new BusinessException(500, "初始化 VOD 客户端失败：" + e.getMessage());
        }
    }

    public Map<String, Object> createUploadAuth(String title, String fileName) {
        IAcsClient client = clientOrNull();
        if (client == null) {
            throw new BusinessException(500, "请先配置阿里云 VOD 的 AccessKey");
        }
        try {
            CreateUploadVideoRequest request = new CreateUploadVideoRequest();
            request.setTitle(title);
            request.setFileName(fileName);
            CreateUploadVideoResponse res = client.getAcsResponse(request);
            Map<String, Object> data = new HashMap<>();
            data.put("videoId", res.getVideoId());
            data.put("uploadAuth", res.getUploadAuth());
            data.put("uploadAddress", res.getUploadAddress());
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, e.getMessage() != null ? e.getMessage() : "获取上传凭证失败");
        }
    }

    public Map<String, Object> refreshUploadAuth(String videoId) {
        IAcsClient client = clientOrNull();
        if (client == null) {
            throw new BusinessException(500, "请先配置阿里云 VOD 的 AccessKey");
        }
        try {
            RefreshUploadVideoRequest request = new RefreshUploadVideoRequest();
            request.setVideoId(videoId);
            RefreshUploadVideoResponse res = client.getAcsResponse(request);
            Map<String, Object> data = new HashMap<>();
            data.put("videoId", res.getVideoId());
            data.put("uploadAuth", res.getUploadAuth());
            data.put("uploadAddress", res.getUploadAddress());
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, e.getMessage() != null ? e.getMessage() : "刷新凭证失败");
        }
    }

    public Map<String, Object> getPlayAuth(String videoId) {
        IAcsClient client = clientOrNull();
        if (client == null) {
            throw new BusinessException(500, "请先配置阿里云 VOD 的 AccessKey");
        }
        try {
            GetVideoPlayAuthRequest request = new GetVideoPlayAuthRequest();
            request.setVideoId(videoId);
            GetVideoPlayAuthResponse res = client.getAcsResponse(request);
            Map<String, Object> data = new HashMap<>();
            data.put("playAuth", res.getPlayAuth());
            data.put("videoMeta", res.getVideoMeta());
            return data;
        } catch (Exception e) {
            throw new BusinessException(500, e.getMessage() != null ? e.getMessage() : "获取播放凭证失败");
        }
    }
}
