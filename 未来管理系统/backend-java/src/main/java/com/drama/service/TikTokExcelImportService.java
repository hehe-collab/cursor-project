package com.drama.service;

import com.drama.entity.TikTokAd;
import com.drama.entity.TikTokAdGroup;
import com.drama.entity.TikTokCampaign;
import com.drama.entity.TikTokExcelImport;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TikTokExcelImportService {

    private final TikTokExcelImportMapper excelImportMapper;
    private final TikTokCampaignService campaignService;
    private final TikTokAdGroupService adGroupService;
    private final TikTokAdService adService;
    private final ObjectMapper objectMapper;

    private final DataFormatter cellFormatter = new DataFormatter();

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public List<TikTokExcelImport> getImports(String advertiserId, String status, int page, int pageSize) {
        int offset = (Math.max(page, 1) - 1) * pageSize;
        if (StringUtils.hasText(status)) {
            return excelImportMapper.selectByStatus(status, pageSize, offset);
        }
        if (StringUtils.hasText(advertiserId)) {
            return excelImportMapper.selectByAdvertiserId(advertiserId, pageSize, offset);
        }
        return excelImportMapper.selectAllPaged(pageSize, offset);
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

    @Transactional
    public TikTokExcelImport uploadAndProcess(
            String advertiserId, String importType, MultipartFile file, String createdBy) throws IOException {
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
        log.info("Created import record: {} type={}", excelImport.getId(), importType);

        processExcelFile(excelImport);
        return getById(excelImport.getId());
    }

    @Transactional
    public void processExcelFile(TikTokExcelImport excelImport) {
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

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) {
                        continue;
                    }
                    totalCount++;
                    try {
                        switch (excelImport.getImportType()) {
                            case "campaigns":
                                processCampaignRow(excelImport.getAdvertiserId(), row);
                                break;
                            case "adgroups":
                                processAdGroupRow(excelImport.getAdvertiserId(), row);
                                break;
                            case "ads":
                                processAdRow(excelImport.getAdvertiserId(), row);
                                break;
                            default:
                                throw new IllegalStateException(
                                        "Unknown import type: " + excelImport.getImportType());
                        }
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
            excelImportMapper.update(excelImport);
        }
    }

    private void processCampaignRow(String advertiserId, Row row) {
        TikTokCampaign c =
                TikTokCampaign.builder()
                        .advertiserId(advertiserId)
                        .campaignName(cell(row, 0))
                        .objective(cell(row, 1))
                        .budget(parseDecimal(cell(row, 2)))
                        .budgetMode(cell(row, 3))
                        .build();
        campaignService.createCampaign(c);
    }

    private void processAdGroupRow(String advertiserId, Row row) {
        TikTokAdGroup g =
                TikTokAdGroup.builder()
                        .advertiserId(advertiserId)
                        .campaignId(cell(row, 0))
                        .adgroupName(cell(row, 1))
                        .placements(cell(row, 2))
                        .bidPrice(parseDecimal(cell(row, 3)))
                        .budget(parseDecimal(cell(row, 4)))
                        .build();
        adGroupService.createAdGroup(g);
    }

    private void processAdRow(String advertiserId, Row row) {
        TikTokAd ad =
                TikTokAd.builder()
                        .advertiserId(advertiserId)
                        .adgroupId(cell(row, 0))
                        .adName(cell(row, 1))
                        .adText(cell(row, 2))
                        .landingPageUrl(cell(row, 3))
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

    public int countImports(String advertiserId) {
        return excelImportMapper.countByAdvertiserId(advertiserId);
    }
}
