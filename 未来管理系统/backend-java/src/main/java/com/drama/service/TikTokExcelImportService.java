package com.drama.service;

import com.drama.entity.TikTokAd;
import com.drama.entity.TikTokAdGroup;
import com.drama.entity.TikTokCampaign;
import com.drama.entity.TikTokExcelImport;
import com.drama.mapper.TikTokAccountMapper;
import com.drama.mapper.TikTokExcelImportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** TikTok Excel 导入服务，支持单账户（single）与多账户（multiple）两种模式。
 *
 * <ul>
 *   <li>single：页面选择 advertiserId，Excel 不填该列</li>
 *   <li>multiple：Excel 第一列必须为 advertiser_id，可同时导入多个账户</li>
 * </ul> */
@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokExcelImportService {

    private final TikTokExcelImportMapper excelImportMapper;
    private final TikTokAccountMapper accountMapper;
    private final TikTokCampaignService campaignService;
    private final TikTokAdGroupService adGroupService;
    private final TikTokAdService adService;
    private final ObjectMapper objectMapper;

    private final DataFormatter cellFormatter = new DataFormatter();

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public List<TikTokExcelImport> getImports(
            String advertiserId, String status, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * pageSize;
        if (StringUtils.hasText(status)) {
            return excelImportMapper.selectByStatus(status, pageSize, offset);
        }
        if (StringUtils.hasText(advertiserId)) {
            return excelImportMapper.selectByAdvertiserId(advertiserId, pageSize, offset);
        }
        return excelImportMapper.selectAllPaged(pageSize, offset);
    }

    public int countImports(String advertiserId) {
        if (StringUtils.hasText(advertiserId)) {
            return excelImportMapper.countByAdvertiserId(advertiserId);
        }
        return excelImportMapper.countAll();
    }

    public TikTokExcelImport getImportById(Long id) {
        return getById(id);
    }

    public TikTokExcelImport getById(Long id) {
        TikTokExcelImport row = excelImportMapper.selectById(id);
        if (row == null) {
            throw new IllegalStateException("Import record not found: " + id);
        }
        return row;
    }

    /** 上传并处理 Excel。
     * @param advertiserId 单账户模式必填；多账户模式下为 null
     * @param importMode single | multiple */
    @Transactional
    public TikTokExcelImport uploadAndProcess(
            String advertiserId,
            String importType,
            String importMode,
            MultipartFile file,
            String createdBy)
            throws IOException {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path uploadPath = base.resolve("tiktok").resolve("excel");
        Files.createDirectories(uploadPath);

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        TikTokExcelImport excelImport =
                TikTokExcelImport.builder()
                        .advertiserId(advertiserId)
                        .importType(importType)
                        .filePath(filePath.toString())
                        .originalFilename(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .status("pending")
                        .totalCount(0)
                        .successCount(0)
                        .failedCount(0)
                        .createdBy(createdBy)
                        .build();

        excelImportMapper.insert(excelImport);
        log.info(
                "Created import record: {} mode={} type={} advertiserId={}",
                excelImport.getId(),
                importMode,
                importType,
                advertiserId);

        // 把 importMode 也暂存（通过 extra1 字段），方便 processExcelFile 读取
        processExcelFile(excelImport, importMode, advertiserId);
        return getById(excelImport.getId());
    }

    /** 三层级统一上传：一张 Excel 包含 campaign + adgroup + ad，按 level 列区分。
     * @param advertiserId 单账户模式必填；多账户模式下为 null
     * @param importMode   single | multiple */
    @Transactional
    public TikTokExcelImport uploadAndProcessUnified(
            String advertiserId,
            String importMode,
            MultipartFile file,
            String createdBy)
            throws IOException {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path uploadPath = base.resolve("tiktok").resolve("excel");
        Files.createDirectories(uploadPath);

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        TikTokExcelImport excelImport =
                TikTokExcelImport.builder()
                        .advertiserId(advertiserId)
                        .importType("unified")
                        .filePath(filePath.toString())
                        .originalFilename(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .status("pending")
                        .totalCount(0)
                        .successCount(0)
                        .failedCount(0)
                        .createdBy(createdBy)
                        .build();

        excelImportMapper.insert(excelImport);
        log.info(
                "Created unified import record: {} mode={} advertiserId={}",
                excelImport.getId(),
                importMode,
                advertiserId);

        processExcelFileUnified(excelImport, importMode, advertiserId);
        return getById(excelImport.getId());
    }

    /** 三层级混合处理：按 level 分组，顺序处理 campaign → adgroup → ad。 */
    @Transactional
    public void processExcelFileUnified(
            TikTokExcelImport excelImport, String importMode, String defaultAdvertiserId) {
        try {
            excelImport.setStatus("processing");
            excelImport.setStartedAt(LocalDateTime.now());
            excelImportMapper.update(excelImport);

            File file = new File(excelImport.getFilePath());
            List<Map<String, Object>> errorLogs = new ArrayList<>();
            int totalCount = 0;
            int successCount = 0;
            int failedCount = 0;

            try (FileInputStream fis = new FileInputStream(file);
                    Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);

                // 解析所有有效行，转换为 UnifiedRow 对象
                List<UnifiedRow> unifiedRows = parseUnifiedRows(sheet, importMode, errorLogs);

                // 按 advertiserId 分离处理
                Map<String, List<UnifiedRow>> byAccount = unifiedRows.stream()
                        .collect(Collectors.groupingBy(r -> r.advertiserId));

                for (Map.Entry<String, List<UnifiedRow>> entry : byAccount.entrySet()) {
                    String advId = entry.getKey();
                    List<UnifiedRow> rows = entry.getValue();

                    // 多账户模式校验账户存在
                    if ("multiple".equals(importMode)) {
                        var account = accountMapper.selectByAdvertiserId(advId);
                        if (account == null) {
                            for (UnifiedRow r : rows) {
                                failedCount++;
                                addError(errorLogs, r.rowNum, advId, "广告账户不存在: " + advId, r.rowData);
                                log.error("Unified row {} advertiser {} not found", r.rowNum, advId);
                            }
                            continue;
                        }
                    } else {
                        // 单账户模式，使用 defaultAdvertiserId
                        String effectiveAdvId =
                                StringUtils.hasText(defaultAdvertiserId)
                                        ? defaultAdvertiserId
                                        : excelImport.getAdvertiserId();
                        if (!StringUtils.hasText(effectiveAdvId)) {
                            throw new IllegalStateException("单账户模式缺少 advertiserId");
                        }
                        // 重写所有行的 advertiserId（用新列表替换，因为 record 字段是 final）
                        for (int i = 0; i < rows.size(); i++) {
                            UnifiedRow orig = rows.get(i);
                            rows.set(i, UnifiedRow.from(orig.rowData(), orig.rowNum(), effectiveAdvId, orig.level()));
                        }
                    }

                    // 按 level 分离
                    List<UnifiedRow> campaigns =
                            rows.stream().filter(r -> "campaign".equals(r.level)).toList();
                    List<UnifiedRow> adgroups =
                            rows.stream().filter(r -> "adgroup".equals(r.level)).toList();
                    List<UnifiedRow> ads =
                            rows.stream().filter(r -> "ad".equals(r.level)).toList();

                    // 建立 name → id 映射
                    Map<String, String> campaignNameToId = new HashMap<>();
                    Map<String, String> adgroupNameToId = new HashMap<>();

                    // Step 1: 处理所有 campaign
                    for (UnifiedRow r : campaigns) {
                        totalCount++;
                        try {
                            validateCampaignRequired(r);
                            TikTokCampaign c = TikTokCampaign.builder()
                                    .advertiserId(advId)
                                    .campaignName(r.campaignName)
                                    .objective(r.objective)
                                    .budget(parseDecimal(r.budget))
                                    .budgetMode(r.budgetMode)
                                    .build();
                            TikTokCampaign created = campaignService.createCampaign(c);
                            campaignNameToId.put(r.campaignName, created.getCampaignId());
                            successCount++;
                        } catch (Exception e) {
                            failedCount++;
                            addError(errorLogs, r.rowNum, advId, e.getMessage(), r.rowData);
                            log.error("Unified campaign row {} failed: {}", r.rowNum, e.getMessage());
                        }
                    }

                    // Step 2: 处理所有 adgroup（resolve campaign_id from name）
                    for (UnifiedRow r : adgroups) {
                        totalCount++;
                        try {
                            validateAdGroupRequired(r);
                            String campaignId = resolveCampaignId(r, campaignNameToId);
                            if (campaignId == null) {
                                throw new IllegalStateException(
                                        "未找到对应的广告系列: " + r.campaignName);
                            }
                            TikTokAdGroup g = TikTokAdGroup.builder()
                                    .advertiserId(advId)
                                    .campaignId(campaignId)
                                    .adgroupName(r.adgroupName)
                                    .placements(r.placements)
                                    .bidPrice(parseDecimal(r.bid))
                                    .budget(parseDecimal(r.budget))
                                    .build();
                            TikTokAdGroup created = adGroupService.createAdGroup(g);
                            // key: campaignName + "|" + adgroupName
                            adgroupNameToId.put(r.campaignName + "|" + r.adgroupName, created.getAdgroupId());
                            successCount++;
                        } catch (Exception e) {
                            failedCount++;
                            addError(errorLogs, r.rowNum, advId, e.getMessage(), r.rowData);
                            log.error("Unified adgroup row {} failed: {}", r.rowNum, e.getMessage());
                        }
                    }

                    // Step 3: 处理所有 ad（resolve adgroup_id from names）
                    for (UnifiedRow r : ads) {
                        totalCount++;
                        try {
                            validateAdRequired(r);
                            String adgroupId = resolveAdGroupId(r, campaignNameToId, adgroupNameToId);
                            if (adgroupId == null) {
                                throw new IllegalStateException(
                                        "未找到对应的广告组: " + r.campaignName + " / " + r.adgroupName);
                            }
                            TikTokAd a = TikTokAd.builder()
                                    .advertiserId(advId)
                                    .adgroupId(adgroupId)
                                    .adName(r.adName)
                                    .adText(r.adText)
                                    .landingPageUrl(r.landingPageUrl)
                                    .build();
                            adService.createAd(a);
                            successCount++;
                        } catch (Exception e) {
                            failedCount++;
                            addError(errorLogs, r.rowNum, advId, e.getMessage(), r.rowData);
                            log.error("Unified ad row {} failed: {}", r.rowNum, e.getMessage());
                        }
                    }
                }
            }

            excelImport.setTotalCount(totalCount);
            excelImport.setSuccessCount(successCount);
            excelImport.setFailedCount(failedCount);
            excelImport.setErrorLogs(objectMapper.writeValueAsString(errorLogs));
            excelImport.setCompletedAt(LocalDateTime.now());
            if (failedCount == 0) {
                excelImport.setStatus("success");
            } else if (successCount > 0) {
                excelImport.setStatus("partial");
            } else {
                excelImport.setStatus("failed");
            }
            excelImportMapper.update(excelImport);
            log.info(
                    "Unified Excel import done id={} total={} ok={} fail={}",
                    excelImport.getId(),
                    totalCount,
                    successCount,
                    failedCount);
        } catch (Exception e) {
            log.error("processExcelFileUnified failed: {}", e.getMessage(), e);
            excelImport.setStatus("failed");
            excelImport.setCompletedAt(LocalDateTime.now());
            try {
                excelImport.setErrorLogs(objectMapper.writeValueAsString(
                        Map.of("fatal", e.getMessage())));
            } catch (Exception ignored) {}
            excelImportMapper.update(excelImport);
        }
    }

    /** 解析统一格式 Sheet → UnifiedRow 列表（跳过表头和说明行）。 */
    private List<UnifiedRow> parseUnifiedRows(
            Sheet sheet, String importMode, List<Map<String, Object>> errorLogs) {
        List<UnifiedRow> rows = new ArrayList<>();
        int levelCol = "multiple".equals(importMode) ? 1 : 0;
        int advCol = "multiple".equals(importMode) ? 0 : -1;

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String levelVal = cell(row, levelCol).trim();
            if (!Arrays.asList("campaign", "adgroup", "ad").contains(levelVal)) {
                continue;
            }
            String advId = advCol >= 0 ? cell(row, advCol).trim() : "";
            rows.add(toUnifiedRow(i + 1, advId, row, levelCol, advCol));
        }
        return rows;
    }

    private UnifiedRow toUnifiedRow(int rowNum, String advId, Row row, int levelCol, int advCol) {
        Map<String, String> data = getUnifiedRowData(row, levelCol, advCol);
        String level = data.getOrDefault("level", "");
        return UnifiedRow.from(data, rowNum, advId, level);
    }

    /** 统一行数据（基于模板列顺序索引取值）。 */
    private record UnifiedRow(
            int rowNum,
            String advertiserId,
            String level,
            Map<String, String> rowData,
            String campaignName,
            String adgroupName,
            String adName,
            String objective,
            String budget,
            String budgetMode,
            String placements,
            String bid,
            String adText,
            String callToAction,
            String landingPageUrl,
            String videoId,
            String status) {
        // 静态工厂，从列索引映射构建
        static UnifiedRow from(Map<String, String> data, int rowNum, String advId, String level) {
            return new UnifiedRow(
                    rowNum, advId, level, data,
                    data.get("campaign_name"),
                    data.get("adgroup_name"),
                    data.get("ad_name"),
                    data.get("objective"),
                    data.get("budget"),
                    data.get("budget_mode"),
                    data.get("placements"),
                    data.get("bid"),
                    data.get("ad_text"),
                    data.get("call_to_action"),
                    data.get("landing_page_url"),
                    data.get("video_id"),
                    data.get("status"));
        }
    }

    private Map<String, String> getUnifiedRowData(Row row, int levelCol, int advCol) {
        // 模板列顺序索引 → 字段名
        Map<Integer, String> colMap = new HashMap<>();
        colMap.put(levelCol, "level");
        colMap.put(levelCol + 1, "campaign_name");
        colMap.put(levelCol + 2, "adgroup_name");
        colMap.put(levelCol + 3, "ad_name");
        colMap.put(levelCol + 4, "objective");
        colMap.put(levelCol + 5, "budget");
        colMap.put(levelCol + 6, "budget_mode");
        colMap.put(levelCol + 7, "placements");
        colMap.put(levelCol + 8, "bid");
        colMap.put(levelCol + 9, "ad_text");
        colMap.put(levelCol + 10, "call_to_action");
        colMap.put(levelCol + 11, "landing_page_url");
        colMap.put(levelCol + 12, "video_id");
        colMap.put(levelCol + 13, "status");
        if (advCol >= 0) {
            colMap.put(advCol, "advertiser_id");
        }

        Map<String, String> rowData = new HashMap<>();
        short last = row.getLastCellNum();
        for (int i = 0; i < last; i++) {
            String fieldName = colMap.get(i);
            if (fieldName != null) {
                rowData.put(fieldName, cell(row, i));
            }
        }
        return rowData;
    }

    private void validateCampaignRequired(UnifiedRow r) throws Exception {
        if (!StringUtils.hasText(r.campaignName)) {
            throw new IllegalStateException("campaign_name 必填");
        }
        if (!StringUtils.hasText(r.objective)) {
            throw new IllegalStateException("objective 必填");
        }
    }

    private void validateAdGroupRequired(UnifiedRow r) throws Exception {
        if (!StringUtils.hasText(r.campaignName)) {
            throw new IllegalStateException("campaign_name 必填（用于关联广告系列）");
        }
        if (!StringUtils.hasText(r.adgroupName)) {
            throw new IllegalStateException("adgroup_name 必填");
        }
    }

    private void validateAdRequired(UnifiedRow r) throws Exception {
        if (!StringUtils.hasText(r.campaignName)) {
            throw new IllegalStateException("campaign_name 必填（用于关联广告系列）");
        }
        if (!StringUtils.hasText(r.adgroupName)) {
            throw new IllegalStateException("adgroup_name 必填（用于关联广告组）");
        }
        if (!StringUtils.hasText(r.adName)) {
            throw new IllegalStateException("ad_name 必填");
        }
    }

    private String resolveCampaignId(UnifiedRow r, Map<String, String> nameToId) {
        if (StringUtils.hasText(r.campaignName)) {
            return nameToId.get(r.campaignName);
        }
        return null;
    }

    private String resolveAdGroupId(
            UnifiedRow r,
            Map<String, String> campaignNameToId,
            Map<String, String> adgroupNameToId) {
        if (StringUtils.hasText(r.campaignName) && StringUtils.hasText(r.adgroupName)) {
            return adgroupNameToId.get(r.campaignName + "|" + r.adgroupName);
        }
        return null;
    }

    private void addError(
            List<Map<String, Object>> errorLogs,
            int rowNum,
            String advId,
            String msg,
            Map<String, String> rowData) {
        Map<String, Object> err = new HashMap<>();
        err.put("row_number", rowNum);
        if (StringUtils.hasText(advId)) err.put("advertiser_id", advId);
        err.put("error_message", msg);
        err.put("row_data", rowData);
        errorLogs.add(err);
    }

    @Transactional
    public void processExcelFile(TikTokExcelImport excelImport, String importMode, String defaultAdvertiserId) {
        try {
            excelImport.setStatus("processing");
            excelImport.setStartedAt(LocalDateTime.now());
            excelImportMapper.update(excelImport);

            File file = new File(excelImport.getFilePath());
            List<Map<String, Object>> errorLogs = new ArrayList<>();
            int totalCount = 0;
            int successCount = 0;
            int failedCount = 0;

            try (FileInputStream fis = new FileInputStream(file);
                    Workbook workbook = new XSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0);

                if ("multiple".equals(importMode)) {
                    // 多账户：按 advertiser_id 分组处理
                    Map<String, List<RowContext>> grouped = parseMultipleModeRows(sheet, errorLogs);
                    for (Map.Entry<String, List<RowContext>> entry : grouped.entrySet()) {
                        String advId = entry.getKey();
                        List<RowContext> rows = entry.getValue();

                        // 校验账户是否存在
                        var account = accountMapper.selectByAdvertiserId(advId);
                        if (account == null) {
                            for (RowContext rc : rows) {
                                failedCount++;
                                Map<String, Object> err = new HashMap<>();
                                err.put("row_number", rc.rowNum);
                                err.put("advertiser_id", advId);
                                err.put("error_message", "广告账户不存在: " + advId);
                                err.put("row_data", rc.rowData);
                                errorLogs.add(err);
                                log.error("Row {} advertiser {} not found", rc.rowNum, advId);
                            }
                            continue;
                        }

                        for (RowContext rc : rows) {
                            totalCount++;
                            try {
                                processRow(advId, excelImport.getImportType(), importMode, rc.row);
                                successCount++;
                            } catch (Exception e) {
                                failedCount++;
                                Map<String, Object> err = new HashMap<>();
                                err.put("row_number", rc.rowNum);
                                err.put("advertiser_id", advId);
                                err.put("error_message", e.getMessage());
                                err.put("row_data", rc.rowData);
                                errorLogs.add(err);
                                log.error("Row {} (adv={}) failed: {}", rc.rowNum, advId, e.getMessage());
                            }
                        }
                    }
                    // 纯数据行数
                    totalCount = grouped.values().stream().mapToInt(List::size).sum();
                } else {
                    // 单账户：从页面传入的 defaultAdvertiserId
                    String advertiserId =
                            StringUtils.hasText(defaultAdvertiserId)
                                    ? defaultAdvertiserId
                                    : excelImport.getAdvertiserId();
                    if (!StringUtils.hasText(advertiserId)) {
                        throw new IllegalStateException("单账户模式缺少 advertiserId");
                    }

                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) {
                            continue;
                        }
                        totalCount++;
                        try {
                            processRow(advertiserId, excelImport.getImportType(), importMode, row);
                            successCount++;
                        } catch (Exception e) {
                            failedCount++;
                            Map<String, Object> err = new HashMap<>();
                            err.put("row_number", i + 1);
                            err.put("error_message", e.getMessage());
                            err.put("row_data", getRowData(row));
                            errorLogs.add(err);
                            log.error("Excel row {} failed: {}", i + 1, e.getMessage());
                        }
                    }
                }
            }

            excelImport.setTotalCount(totalCount);
            excelImport.setSuccessCount(successCount);
            excelImport.setFailedCount(failedCount);
            excelImport.setErrorLogs(objectMapper.writeValueAsString(errorLogs));
            excelImport.setCompletedAt(LocalDateTime.now());
            if (failedCount == 0) {
                excelImport.setStatus("success");
            } else if (successCount > 0) {
                excelImport.setStatus("partial");
            } else {
                excelImport.setStatus("failed");
            }
            excelImportMapper.update(excelImport);
            log.info(
                    "Excel import done id={} total={} ok={} fail={}",
                    excelImport.getId(),
                    totalCount,
                    successCount,
                    failedCount);
        } catch (Exception e) {
            log.error("processExcelFile failed: {}", e.getMessage(), e);
            excelImport.setStatus("failed");
            excelImport.setCompletedAt(LocalDateTime.now());
            try {
                excelImport.setErrorLogs(objectMapper.writeValueAsString(
                        Map.of("fatal", e.getMessage())));
            } catch (Exception ignored) {}
            excelImportMapper.update(excelImport);
        }
    }

    /** 多账户模式：解析所有行并按 advertiser_id 分组，返回"有效行"的分组（不含空行/跳过行）。 */
    private Map<String, List<RowContext>> parseMultipleModeRows(
            Sheet sheet, List<Map<String, Object>> errorLogs) {
        Map<String, List<RowContext>> grouped = new HashMap<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String advId = cell(row, 0);
            if (!StringUtils.hasText(advId)) {
                // 空行，跳过
                continue;
            }
            grouped.computeIfAbsent(advId, k -> new ArrayList<>())
                    .add(new RowContext(i + 1, row, getRowData(row)));
        }
        return grouped;
    }

    private record RowContext(int rowNum, Row row, Map<String, String> rowData) {}

    /** 根据 importType 分发处理单行。 */
    private void processRow(String advertiserId, String importType, String importMode, Row row) throws Exception {
        switch (importType) {
            case "campaigns":
                createCampaign(advertiserId, importMode, row);
                break;
            case "adgroups":
                createAdGroup(advertiserId, importMode, row);
                break;
            case "ads":
                createAd(advertiserId, importMode, row);
                break;
            default:
                throw new IllegalStateException("Unknown import type: " + importType);
        }
    }

    /** 多账户模式从第 1 列开始（跳过 advertiser_id 第 0 列）；单账户从第 0 列开始。 */
    private void createCampaign(String advertiserId, String importMode, Row row) {
        int base = "multiple".equals(importMode) ? 1 : 0;
        TikTokCampaign c =
                TikTokCampaign.builder()
                        .advertiserId(advertiserId)
                        .campaignName(cell(row, base))
                        .objective(cell(row, base + 1))
                        .budget(parseDecimal(cell(row, base + 2)))
                        .budgetMode(cell(row, base + 3))
                        .build();
        campaignService.createCampaign(c);
    }

    private void createAdGroup(String advertiserId, String importMode, Row row) {
        int base = "multiple".equals(importMode) ? 1 : 0;
        TikTokAdGroup g =
                TikTokAdGroup.builder()
                        .advertiserId(advertiserId)
                        .campaignId(cell(row, base))
                        .adgroupName(cell(row, base + 1))
                        .placements(cell(row, base + 2))
                        .bidPrice(parseDecimal(cell(row, base + 3)))
                        .budget(parseDecimal(cell(row, base + 4)))
                        .build();
        adGroupService.createAdGroup(g);
    }

    private void createAd(String advertiserId, String importMode, Row row) {
        int base = "multiple".equals(importMode) ? 1 : 0;
        TikTokAd ad =
                TikTokAd.builder()
                        .advertiserId(advertiserId)
                        .adgroupId(cell(row, base))
                        .adName(cell(row, base + 1))
                        .adText(cell(row, base + 2))
                        .landingPageUrl(cell(row, base + 3))
                        .build();
        adService.createAd(ad);
    }

    private String cell(Row row, int idx) {
        Cell c = row.getCell(idx);
        if (c == null) {
            return "";
        }
        return cellFormatter.formatCellValue(c).trim();
    }

    private Map<String, String> getRowData(Row row) {
        Map<String, String> rowData = new HashMap<>();
        short last = row.getLastCellNum();
        for (int i = 0; i < last; i++) {
            rowData.put("col_" + i, cell(row, i));
        }
        return rowData;
    }

    private static BigDecimal parseDecimal(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return new BigDecimal(raw.replace(",", "").trim());
    }

    @Transactional
    public TikTokExcelImport createImport(TikTokExcelImport row) {
        if (row.getStatus() == null) {
            row.setStatus("pending");
        }
        excelImportMapper.insert(row);
        return excelImportMapper.selectById(row.getId());
    }

    public List<TikTokExcelImport> listByAdvertiser(String advertiserId, int limit, int offset) {
        return excelImportMapper.selectByAdvertiserId(advertiserId, limit, offset);
    }

    public List<TikTokExcelImport> listByStatus(String status, int limit, int offset) {
        return excelImportMapper.selectByStatus(status, limit, offset);
    }

    public List<TikTokExcelImport> listAllPaged(int limit, int offset) {
        return excelImportMapper.selectAllPaged(limit, offset);
    }

    @Transactional
    public TikTokExcelImport updateImport(TikTokExcelImport row) {
        excelImportMapper.update(row);
        return getById(row.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        getById(id);
        excelImportMapper.deleteById(id);
    }

    @Transactional
    public void deleteImport(Long id) {
        TikTokExcelImport excelImport = getById(id);
        try {
            Files.deleteIfExists(Paths.get(excelImport.getFilePath()));
        } catch (IOException e) {
            log.error("Failed to delete file: {}", excelImport.getFilePath(), e);
        }
        excelImportMapper.deleteById(id);
        log.info("Deleted import record: {}", id);
    }
}
