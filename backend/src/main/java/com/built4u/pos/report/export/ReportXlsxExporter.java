package com.built4u.pos.report.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic single-sheet xlsx renderer for an {@link ExportTable}: title, subtitle
 * lines, a bold header row, type-aware data (BigDecimal/Number stay numeric,
 * dates stay dates), and italic footer notes. XSSFWorkbook (in-memory) — the
 * controller caps datasets at {@link ExportResponses#EXPORT_ROW_CAP}.
 */
@Component
public class ReportXlsxExporter {

    public byte[] export(ExportTable table) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Report");
            int colCount = Math.max(1, table.headers().size());

            CellStyle titleStyle = titleStyle(wb);
            CellStyle subtitleStyle = subtitleStyle(wb);
            CellStyle headerStyle = headerStyle(wb);
            CellStyle moneyStyle = moneyStyle(wb);
            CellStyle dateStyle = dateStyle(wb);
            CellStyle dateTimeStyle = dateTimeStyle(wb);
            CellStyle italicStyle = italicStyle(wb);

            int rowIdx = 0;

            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.createCell(0).setCellValue("Built4U  ·  " + safe(table.title()));
            titleRow.getCell(0).setCellStyle(titleStyle);
            if (colCount > 1) sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));

            for (String s : table.subtitle()) {
                int r = rowIdx++;
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue(safe(s));
                row.getCell(0).setCellStyle(subtitleStyle);
                if (colCount > 1) sheet.addMergedRegion(new CellRangeAddress(r, r, 0, colCount - 1));
            }

            rowIdx++; // spacer

            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            for (List<Object> dataRow : table.rows()) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < dataRow.size() && i < colCount; i++) {
                    Cell cell = row.createCell(i);
                    setCellValue(cell, dataRow.get(i), moneyStyle, dateStyle, dateTimeStyle);
                }
            }

            if (!table.footer().isEmpty()) {
                rowIdx++;
                for (String s : table.footer()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(safe(s));
                    row.getCell(0).setCellStyle(italicStyle);
                    if (colCount > 1) {
                        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, colCount - 1));
                    }
                }
            }

            for (int i = 0; i < colCount; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                if (width > 60 * 256) sheet.setColumnWidth(i, 60 * 256);
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void setCellValue(Cell cell, Object v, CellStyle moneyStyle, CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (v == null) { cell.setBlank(); return; }
        if (v instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
            cell.setCellStyle(moneyStyle);
        } else if (v instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (v instanceof LocalDate ld) {
            cell.setCellValue(java.sql.Date.valueOf(ld));
            cell.setCellStyle(dateStyle);
        } else if (v instanceof LocalDateTime ldt) {
            cell.setCellValue(java.sql.Timestamp.valueOf(ldt));
            cell.setCellStyle(dateTimeStyle);
        } else {
            cell.setCellValue(v.toString());
        }
    }

    private static CellStyle titleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14); s.setFont(f);
        return s;
    }
    private static CellStyle subtitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setFontHeightInPoints((short) 11); s.setFont(f);
        return s;
    }
    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 11); s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }
    private static CellStyle moneyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }
    private static CellStyle dateStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
        return s;
    }
    private static CellStyle dateTimeStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
        return s;
    }
    private static CellStyle italicStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setItalic(true); f.setFontHeightInPoints((short) 10); s.setFont(f);
        return s;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
