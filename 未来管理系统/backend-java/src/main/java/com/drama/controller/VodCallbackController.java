package com.drama.controller;

import com.alibaba.fastjson2.JSON;
import com.drama.entity.DramaEpisode;
import com.drama.mapper.DramaEpisodeMapper;
import com.drama.service.DramaService;
import com.drama.service.VodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阿里云 VOD 事件回调端点。
 *
 * <p>阿里云 VOD 在视频上传完成、转码完成等事件后，主动 POST 推送到本端点。
 * 不需要 JWT，通过 HMAC-SHA1 签名验证来源真实性。
 */
@Slf4j
@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
@Tag(name = "阿里云VOD回调", description = "阿里云 VOD 事件回调（无需 JWT，用签名验证）")
public class VodCallbackController {

    private final DramaEpisodeMapper dramaEpisodeMapper;
    private final DramaService dramaService;
    private final VodService vodService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${aliyun.vod.callback-secret:}")
    private String callbackSecret;

    @PostConstruct
    public void ensureCallbackLogTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS vod_callback_log (
                  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
                  event_id     VARCHAR(200) NOT NULL,
                  video_id     VARCHAR(64)  NOT NULL,
                  event_type   VARCHAR(64)  NOT NULL,
                  raw_status   VARCHAR(64)  DEFAULT NULL,
                  payload      TEXT,
                  received_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
                  UNIQUE KEY uk_event_id (event_id),
                  INDEX idx_video_id (video_id),
                  INDEX idx_received_at (received_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        } catch (Exception e) {
            log.warn("[VodCallback] 创建 vod_callback_log 表失败（可能已存在）: {}", e.getMessage());
        }
    }

    @Operation(summary = "阿里云 VOD 事件回调", description = "阿里云主动推送，无需 JWT。通过签名验证来源。")
    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody String rawBody) {
        log.info("[VodCallback] 收到回调，body 长度={}", rawBody != null ? rawBody.length() : 0);

        Map<String, Object> body;
        try {
            body = JSON.parseObject(rawBody);
        } catch (Exception e) {
            log.warn("[VodCallback] JSON 解析失败: {}", e.getMessage());
            return ResponseEntity.ok("{\"code\":0}");
        }
        if (body == null || body.isEmpty()) {
            return ResponseEntity.ok("{\"code\":0}");
        }

        String eventType = str(body.get("EventType"));
        String videoId = str(body.get("VideoId"));
        log.info("[VodCallback] EventType={}, VideoId={}", eventType, videoId);

        // 幂等：构造 eventId 去重
        String eventId = str(body.get("EventTime")) + "_" + videoId + "_" + eventType;
        if (isDuplicate(eventId)) {
            log.info("[VodCallback] 重复事件跳过: eventId={}", eventId);
            return ResponseEntity.ok("{\"code\":0}");
        }

        // 写日志
        saveLog(eventId, videoId, eventType, str(body.get("Status")), rawBody);

        // 处理有意义的事件
        try {
            handleEvent(body, eventType, videoId);
        } catch (Exception e) {
            log.error("[VodCallback] 处理异常: eventType={}, videoId={}", eventType, videoId, e);
        }

        return ResponseEntity.ok("{\"code\":0}");
    }

    private void handleEvent(Map<String, Object> body, String eventType, String videoId) {
        if (!StringUtils.hasText(videoId)) return;

        DramaEpisode episode = dramaEpisodeMapper.selectByVodVideoId(videoId);
        if (episode == null) {
            log.info("[VodCallback] videoId={} 不在 drama_episodes 中，跳过", videoId);
            return;
        }

        switch (eventType) {
            case "FileUploadComplete", "UploadByURLComplete" -> {
                updateEpisodeStatus(episode, "transcoding", body);
                log.info("[VodCallback] {} → transcoding, dramaId={}, ep={}", eventType, episode.getDramaId(), episode.getEpisodeNum());
            }
            case "StreamTranscodeComplete" -> {
                if (!"normal".equals(episode.getVodStatus())) {
                    updateEpisodeStatus(episode, "transcoding", body);
                }
            }
            case "TranscodeComplete" -> {
                String status = str(body.get("Status"));
                if ("success".equalsIgnoreCase(status)) {
                    updateEpisodeStatus(episode, "normal", body);
                    log.info("[VodCallback] TranscodeComplete → normal, dramaId={}, ep={}", episode.getDramaId(), episode.getEpisodeNum());
                } else {
                    updateEpisodeStatus(episode, "failed", body);
                    log.warn("[VodCallback] TranscodeComplete 失败, status={}, dramaId={}, ep={}", status, episode.getDramaId(), episode.getEpisodeNum());
                }
            }
            case "AIMediaAuditComplete" -> {
                log.info("[VodCallback] AI审核完成 videoId={}, status={}", videoId, str(body.get("Status")));
            }
            default -> log.info("[VodCallback] 未处理的事件类型: {}", eventType);
        }
    }

    private void updateEpisodeStatus(DramaEpisode episode, String newStatus, Map<String, Object> body) {
        episode.setVodStatus(newStatus);
        Object duration = body.get("Duration");
        if (duration != null && (episode.getDuration() == null || episode.getDuration() <= 0)) {
            try {
                episode.setDuration(Double.valueOf(String.valueOf(duration)).intValue());
            } catch (Exception ignore) {}
        }
        Object size = body.get("Size");
        if (size != null && (episode.getVideoSize() == null || episode.getVideoSize() <= 0)) {
            try {
                episode.setVideoSize(Long.parseLong(String.valueOf(size)));
            } catch (Exception ignore) {}
        }
        Object coverUrl = body.get("CoverUrl");
        if (coverUrl != null && !str(coverUrl).isBlank() && (episode.getVodCoverUrl() == null || episode.getVodCoverUrl().isBlank())) {
            episode.setVodCoverUrl(str(coverUrl));
        }
        dramaEpisodeMapper.update(episode);

        if (episode.getDramaId() != null) {
            try {
                dramaService.refreshDramaTaskStatus(episode.getDramaId());
            } catch (Exception e) {
                log.warn("[VodCallback] refreshDramaTaskStatus 失败: dramaId={}", episode.getDramaId(), e);
            }
        }
    }

    private boolean isDuplicate(String eventId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vod_callback_log WHERE event_id = ?",
                    Integer.class, eventId);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveLog(String eventId, String videoId, String eventType, String rawStatus, String payload) {
        try {
            String truncatedPayload = payload != null && payload.length() > 4000 ? payload.substring(0, 4000) : payload;
            jdbcTemplate.update(
                    "INSERT IGNORE INTO vod_callback_log (event_id, video_id, event_type, raw_status, payload) VALUES (?, ?, ?, ?, ?)",
                    eventId, videoId, eventType, rawStatus, truncatedPayload);
        } catch (Exception e) {
            log.warn("[VodCallback] 保存回调日志失败: {}", e.getMessage());
        }
    }

    private static String str(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return "null".equalsIgnoreCase(s) ? "" : s;
    }
}
