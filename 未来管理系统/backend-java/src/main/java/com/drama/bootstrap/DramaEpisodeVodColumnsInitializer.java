package com.drama.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容旧库：补齐 drama_episodes 的 VOD 相关列。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DramaEpisodeVodColumnsInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn(
                "vod_video_id",
                "ALTER TABLE drama_episodes "
                        + "ADD COLUMN vod_video_id VARCHAR(100) NULL COMMENT '阿里云 VOD 视频 ID' "
                        + "AFTER video_url");
        ensureColumn(
                "vod_status",
                "ALTER TABLE drama_episodes "
                        + "ADD COLUMN vod_status VARCHAR(32) NOT NULL DEFAULT 'manual' "
                        + "COMMENT 'VOD 状态：manual/uploading/transcoding/normal/failed' "
                        + "AFTER vod_video_id");
        ensureColumn(
                "video_size",
                "ALTER TABLE drama_episodes "
                        + "ADD COLUMN video_size BIGINT NOT NULL DEFAULT 0 COMMENT '视频大小（字节）' "
                        + "AFTER vod_status");
        ensureColumn(
                "vod_cover_url",
                "ALTER TABLE drama_episodes "
                        + "ADD COLUMN vod_cover_url VARCHAR(500) NULL COMMENT 'VOD 封面地址' "
                        + "AFTER video_size");
        ensureIndex(
                "idx_episodes_vod_video_id",
                "CREATE INDEX idx_episodes_vod_video_id ON drama_episodes (vod_video_id)");
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            if (hasColumn(columnName)) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("DramaEpisodeVodColumnsInitializer: added drama_episodes.{}", columnName);
        } catch (Exception e) {
            log.error(
                    "DramaEpisodeVodColumnsInitializer failed for column {}: {}",
                    columnName,
                    e.getMessage(),
                    e);
        }
    }

    private void ensureIndex(String indexName, String ddl) {
        try {
            if (hasIndex(indexName)) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("DramaEpisodeVodColumnsInitializer: added index {}", indexName);
        } catch (Exception e) {
            log.error(
                    "DramaEpisodeVodColumnsInitializer failed for index {}: {}",
                    indexName,
                    e.getMessage(),
                    e);
        }
    }

    private boolean hasColumn(String columnName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) "
                                + "FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() "
                                + "AND table_name = 'drama_episodes' "
                                + "AND column_name = ?",
                        Integer.class,
                        columnName);
        return count != null && count > 0;
    }

    private boolean hasIndex(String indexName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) "
                                + "FROM information_schema.statistics "
                                + "WHERE table_schema = DATABASE() "
                                + "AND table_name = 'drama_episodes' "
                                + "AND index_name = ?",
                        Integer.class,
                        indexName);
        return count != null && count > 0;
    }
}
