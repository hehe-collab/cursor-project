package com.drama.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容旧库：补齐 dramas.vod_cate_id 列（阿里云 VOD 分类 ID）。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DramaVodCateColumnInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn(
                "vod_cate_id",
                "ALTER TABLE dramas "
                        + "ADD COLUMN vod_cate_id BIGINT NULL "
                        + "COMMENT '阿里云 VOD 三级分类 ID（剧名分类，按需建）' "
                        + "AFTER category_id");
        ensureIndex(
                "idx_dramas_vod_cate_id",
                "CREATE INDEX idx_dramas_vod_cate_id ON dramas (vod_cate_id)");
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            if (hasColumn(columnName)) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("DramaVodCateColumnInitializer: added dramas.{}", columnName);
        } catch (Exception e) {
            log.error(
                    "DramaVodCateColumnInitializer failed for column {}: {}",
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
            log.info("DramaVodCateColumnInitializer: added index {}", indexName);
        } catch (Exception e) {
            log.error(
                    "DramaVodCateColumnInitializer failed for index {}: {}",
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
                                + "AND table_name = 'dramas' "
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
                                + "AND table_name = 'dramas' "
                                + "AND index_name = ?",
                        Integer.class,
                        indexName);
        return count != null && count > 0;
    }
}
