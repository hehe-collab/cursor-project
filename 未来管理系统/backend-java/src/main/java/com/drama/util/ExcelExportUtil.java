package com.drama.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelExportUtil {

    private ExcelExportUtil() {}

    public static byte[] buildXlsx(String sheetName, String[] headers, List<Object[]> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(sheetName);
            int ri = 0;
            Row h = sh.createRow(ri++);
            for (int i = 0; i < headers.length; i++) {
                h.createCell(i).setCellValue(headers[i]);
            }
            for (Object[] cells : rows) {
                Row xr = sh.createRow(ri++);
                for (int i = 0; i < cells.length; i++) {
                    Cell c = xr.createCell(i);
                    Object v = i < cells.length ? cells[i] : null;
                    if (v == null) {
                        c.setBlank();
                    } else if (v instanceof Number n) {
                        c.setCellValue(n.doubleValue());
                    } else {
                        c.setCellValue(String.valueOf(v));
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
