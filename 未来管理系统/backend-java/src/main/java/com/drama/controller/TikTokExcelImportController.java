package com.drama.controller;

import com.drama.common.Result;
import com.drama.entity.Admin;
import com.drama.entity.TikTokExcelImport;
import com.drama.mapper.AdminMapper;
import com.drama.service.TikTokExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** TikTok Excel 导入（{@code /api/tiktok/excel-imports}，需 Bearer）。
 *  支持两种导入模式：
 *  <ul>
 *    <li>single（单账户）：页面选择 advertiserId，Excel 不填</li>
 *    <li>multiple（多账户）：Excel 第一列必须为 advertiser_id</li>
 *  </ul> */
@Tag(name = "TikTok导入", description = "TikTok Excel 批量导入与模板下载")
@Slf4j
@RestController
@RequestMapping("/api/tiktok/excel-imports")
@RequiredArgsConstructor
public class TikTokExcelImportController {

    private final TikTokExcelImportService excelImportService;
    private final AdminMapper adminMapper;

    @Operation(summary = "获取导入记录列表", description = "获取TikTok导入记录列表")
    @GetMapping
    public Result<Map<String, Object>> getImports(
            @Parameter(description = "广告主ID") @RequestParam(required = false) String advertiserId,
            @Parameter(description = "导入状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        try {
            List<TikTokExcelImport> imports =
                    excelImportService.getImports(advertiserId, status, page, pageSize);
            int total = excelImportService.countImports(advertiserId);
            Map<String, Object> data = new HashMap<>();
            data.put("list", imports);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("total", total);
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to get imports: {}", e.getMessage(), e);
            return Result.error("获取导入记录列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取导入记录详情", description = "根据ID获取导入记录详细信息")
    @GetMapping("/{id:\\d+}")
    public Result<TikTokExcelImport> getImportById(@PathVariable Long id) {
        try {
            TikTokExcelImport row = excelImportService.getImportById(id);
            return Result.success(row);
        } catch (Exception e) {
            log.error("Failed to get import {}: {}", id, e.getMessage(), e);
            return Result.error("获取导入记录失败: " + e.getMessage());
        }
    }

    /** 上传 Excel 文件并处理（三层级混合模式，不再需要 importType）。
     * @param file           Excel 文件（三层级统一表）
     * @param importMode     single（单账户）/ multiple（多账户）
     * @param advertiserId   单账户模式必填；多账户模式可不传
     * @param adminId        当前登录管理员 ID（由 JwtAuthenticationFilter 注入） */
    @Operation(summary = "上传并处理Excel导入", description = "上传Excel文件并批量导入TikTok广告数据")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<TikTokExcelImport> uploadAndProcess(
            @Parameter(description = "Excel文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "导入模式：single/multiple") @RequestParam(defaultValue = "single") String importMode,
            @Parameter(description = "广告主ID") @RequestParam(value = "advertiserId", required = false) String advertiserId,
            @RequestAttribute(value = "adminId", required = false) Integer adminId) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            String filename = file.getOriginalFilename();
            if (filename == null
                    || (!filename.toLowerCase().endsWith(".xlsx")
                            && !filename.toLowerCase().endsWith(".xls"))) {
                return Result.error("只支持 .xlsx/.xls 格式的文件");
            }
            if (!Arrays.asList("single", "multiple").contains(importMode)) {
                return Result.error("不支持的导入模式: " + importMode);
            }
            if ("single".equals(importMode) && (advertiserId == null || advertiserId.isBlank())) {
                return Result.error("单账户模式必须选择广告账户");
            }
            String createdBy = resolveCreator(adminId);
            log.info(
                    "Excel upload (unified 3-level): mode={} advertiserId={} by={}",
                    importMode,
                    advertiserId,
                    createdBy);
            TikTokExcelImport result =
                    excelImportService.uploadAndProcessUnified(
                            advertiserId, importMode, file, createdBy);
            return Result.success(result);
        } catch (IOException e) {
            log.error("Failed to upload excel: {}", e.getMessage(), e);
            return Result.error("上传 Excel 失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process excel: {}", e.getMessage(), e);
            return Result.error("处理 Excel 失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除导入记录", description = "删除指定的导入记录")
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> deleteImport(@PathVariable Long id) {
        try {
            excelImportService.deleteImport(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to delete import {}: {}", id, e.getMessage(), e);
            return Result.error("删除导入记录失败: " + e.getMessage());
        }
    }

    /** 动态生成 Excel 模板（统一三层级模板，所有层级同一张表）。
     *  single 模式：不含 advertiser_id 列
     *  multiple 模式：含 advertiser_id 首列 */
    @Operation(summary = "下载导入模板", description = "下载TikTok广告导入Excel模板")
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate(
            @Parameter(description = "导入模式：single/multiple") @RequestParam(defaultValue = "single") String importMode) {
        try {
            if (!Arrays.asList("single", "multiple").contains(importMode)) {
                return ResponseEntity.badRequest().build();
            }
            byte[] bytes = generateUnifiedTemplate(importMode);
            String displayName =
                    "tiktok_ad_import_template_"
                            + ("single".equals(importMode) ? "单账户" : "多账户")
                            + ".xlsx";
            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + new String(displayName.getBytes("UTF-8"), "ISO-8859-1")
                                    + "\"")
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to generate template: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 生成统一三层级模板（所有层级同一张表）。 */
    private byte[] generateUnifiedTemplate(String importMode) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("广告导入");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle exampleStyle = wb.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            exampleStyle.setBorderBottom(BorderStyle.THIN);
            exampleStyle.setBorderTop(BorderStyle.THIN);
            exampleStyle.setBorderLeft(BorderStyle.THIN);
            exampleStyle.setBorderRight(BorderStyle.THIN);

            CellStyle noteStyle = wb.createCellStyle();
            noteStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            noteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            noteStyle.setBorderBottom(BorderStyle.THIN);
            noteStyle.setBorderTop(BorderStyle.THIN);
            noteStyle.setBorderLeft(BorderStyle.THIN);
            noteStyle.setBorderRight(BorderStyle.THIN);
            Font noteFont = wb.createFont();
            noteFont.setItalic(true);
            noteFont.setColor(IndexedColors.DARK_YELLOW.getIndex());
            noteStyle.setFont(noteFont);

            List<String> headers = getUnifiedHeaders(importMode);
            List<String> notes = getUnifiedNotes(importMode);

            // 第 0 行：表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            // 第 1 行：字段说明
            Row noteRow = sheet.createRow(1);
            for (int i = 0; i < notes.size(); i++) {
                Cell c = noteRow.createCell(i);
                c.setCellValue(notes.get(i));
                c.setCellStyle(noteStyle);
            }

            // 第 2～4 行：示例数据（campaign / adgroup / ad 各一行）
            Object[][] examples = getUnifiedExamples(importMode);
            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(2 + r);
                Object[] vals = examples[r];
                for (int c = 0; c < vals.length; c++) {
                    Cell cell = row.createCell(c);
                    if (vals[c] instanceof Number) {
                        cell.setCellValue(((Number) vals[c]).doubleValue());
                    } else if (vals[c] != null) {
                        cell.setCellValue(vals[c].toString());
                    }
                    cell.setCellStyle(exampleStyle);
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 统一三层级模板表头。 */
    private List<String> getUnifiedHeaders(String importMode) {
        if ("multiple".equals(importMode)) {
            return Arrays.asList(
                    "advertiser_id*",
                    "level*",
                    "campaign_name",
                    "adgroup_name",
                    "ad_name",
                    "objective",
                    "budget",
                    "budget_mode",
                    "placements",
                    "bid",
                    "ad_text",
                    "call_to_action",
                    "landing_page_url",
                    "video_id",
                    "status");
        } else {
            return Arrays.asList(
                    "level*",
                    "campaign_name",
                    "adgroup_name",
                    "ad_name",
                    "objective",
                    "budget",
                    "budget_mode",
                    "placements",
                    "bid",
                    "ad_text",
                    "call_to_action",
                    "landing_page_url",
                    "video_id",
                    "status");
        }
    }

    /** 统一三层级字段说明。 */
    private List<String> getUnifiedNotes(String importMode) {
        if ("multiple".equals(importMode)) {
            return Arrays.asList(
                    "广告账户ID（必填）",
                    "层级：campaign/adgroup/ad（必填）",
                    "广告系列名称（level=campaign必填）",
                    "广告组名称（level=adgroup必填）",
                    "广告名称（level=ad必填）",
                    "目标（campaign必填）",
                    "预算金额",
                    "BUDGET_MODE_DAY/TOTAL",
                    "投放位置",
                    "出价",
                    "广告文案",
                    "行动号召",
                    "落地页URL",
                    "视频素材ID",
                    "ENABLE/DISABLE");
        } else {
            return Arrays.asList(
                    "层级：campaign/adgroup/ad（必填）",
                    "广告系列名称（level=campaign必填）",
                    "广告组名称（level=adgroup必填）",
                    "广告名称（level=ad必填）",
                    "目标（campaign必填）",
                    "预算金额",
                    "BUDGET_MODE_DAY/TOTAL",
                    "投放位置",
                    "出价",
                    "广告文案",
                    "行动号召",
                    "落地页URL",
                    "视频素材ID",
                    "ENABLE/DISABLE");
        }
    }

    /** 统一三层级示例数据（3行：campaign/adgroup/ad 各一条）。 */
    private Object[][] getUnifiedExamples(String importMode) {
        if ("multiple".equals(importMode)) {
            return new Object[][] {
                new Object[] {
                    "7123456789012345678",
                    "campaign",
                    "春季促销活动",
                    "",
                    "",
                    "TRAFFIC",
                    1000.0,
                    "BUDGET_MODE_DAY",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "ENABLE"
                },
                new Object[] {
                    "7123456789012345678",
                    "adgroup",
                    "春季促销活动",
                    "18-24岁女性-北京",
                    "",
                    "",
                    500.0,
                    "",
                    "PLACEMENT_TIKTOK",
                    1.5,
                    "",
                    "",
                    "",
                    "",
                    "ENABLE"
                },
                new Object[] {
                    "7123456789012345678",
                    "ad",
                    "春季促销活动",
                    "18-24岁女性-北京",
                    "视频1",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "限时优惠！立即购买",
                    "SHOP_NOW",
                    "https://example.com/product",
                    "v1234567890",
                    "ENABLE"
                }
            };
        } else {
            return new Object[][] {
                new Object[] {
                    "campaign",
                    "春季促销活动",
                    "",
                    "",
                    "TRAFFIC",
                    1000.0,
                    "BUDGET_MODE_DAY",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "ENABLE"
                },
                new Object[] {
                    "adgroup",
                    "春季促销活动",
                    "18-24岁女性-北京",
                    "",
                    "",
                    500.0,
                    "",
                    "PLACEMENT_TIKTOK",
                    1.5,
                    "",
                    "",
                    "",
                    "",
                    "ENABLE"
                },
                new Object[] {
                    "ad",
                    "春季促销活动",
                    "18-24岁女性-北京",
                    "视频1",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "限时优惠！立即购买",
                    "SHOP_NOW",
                    "https://example.com/product",
                    "v1234567890",
                    "ENABLE"
                }
            };
        }
    }

    private String resolveCreator(Integer adminId) {
        if (adminId == null) {
            return "system";
        }
        Admin a = adminMapper.selectById(adminId);
        if (a == null) {
            return "system";
        }
        if (a.getUsername() != null && !a.getUsername().isBlank()) {
            return a.getUsername();
        }
        if (a.getNickname() != null && !a.getNickname().isBlank()) {
            return a.getNickname();
        }
        return String.valueOf(adminId);
    }
}
