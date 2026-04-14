package com.drama.task;

import com.drama.entity.DramaEpisode;
import com.drama.mapper.DramaEpisodeMapper;
import com.drama.service.DramaService;
import com.drama.service.VodService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 基础轮询：在未配置阿里云回调前，定时刷新 VOD 转码状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DramaEpisodeVodSyncTask {

    private final DramaEpisodeMapper dramaEpisodeMapper;
    private final VodService vodService;
    private final DramaService dramaService;

    @Scheduled(fixedDelayString = "${aliyun.vod.sync-delay-ms:60000}")
    public void syncVodStatus() {
        if (!vodService.isConfigured()) {
            return;
        }
        List<DramaEpisode> rows =
                dramaEpisodeMapper.selectByVodStatuses(List.of("uploading", "transcoding"));
        Set<Integer> changedDramaIds = new LinkedHashSet<>();
        for (DramaEpisode row : rows) {
            String vodVideoId =
                    firstNonBlank(row.getVodVideoId(), row.getVideoId());
            if (vodVideoId.isBlank()) {
                continue;
            }
            try {
                Map<String, Object> info = vodService.getVideoInfo(vodVideoId);
                row.setVodStatus(firstNonBlank(str(info.get("status")), row.getVodStatus(), "uploading"));
                if ((row.getDuration() == null || row.getDuration() <= 0) && info.get("duration") != null) {
                    row.setDuration(toInt(info.get("duration")));
                }
                if ((row.getVideoSize() == null || row.getVideoSize() <= 0) && info.get("size") != null) {
                    row.setVideoSize(toLong(info.get("size")));
                }
                if (!firstNonBlank(row.getVodCoverUrl()).isBlank() || info.get("coverUrl") == null) {
                    // keep existing cover
                } else {
                    row.setVodCoverUrl(str(info.get("coverUrl")));
                }
                dramaEpisodeMapper.update(row);
                if (row.getDramaId() != null) {
                    changedDramaIds.add(row.getDramaId());
                }
            } catch (Exception e) {
                log.warn("sync vod episode status failed episodeId={} videoId={}: {}", row.getId(), vodVideoId, e.getMessage());
            }
        }
        for (Integer dramaId : changedDramaIds) {
            dramaService.refreshDramaTaskStatus(dramaId);
        }
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value).trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String str(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
