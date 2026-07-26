package com.built4u.pos.report.export;

import com.built4u.pos.common.exception.BadRequestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

/**
 * Shared helper for the report controller: validates the {@code format} query
 * param and wraps PDF / xlsx bytes in a downloadable {@code ResponseEntity}.
 */
public final class ExportResponses {

    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_PDF  = "pdf";
    public static final String FORMAT_XLSX = "xlsx";

    /** Hard cap on rows pulled for an export. Protects against runaway downloads. */
    public static final int EXPORT_ROW_CAP = 10_000;

    private static final Set<String> ALLOWED = Set.of(FORMAT_JSON, FORMAT_PDF, FORMAT_XLSX);
    private static final MediaType XLSX_MEDIA_TYPE =
        MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ExportResponses() {}

    /** Returns the normalized format ('json' / 'pdf' / 'xlsx'). Rejects anything else with 400. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return FORMAT_JSON;
        String f = raw.trim().toLowerCase();
        if (!ALLOWED.contains(f)) {
            throw new BadRequestException("Unsupported format '" + raw + "'. Allowed: json, pdf, xlsx");
        }
        return f;
    }

    public static boolean isExport(String format) {
        return FORMAT_PDF.equals(format) || FORMAT_XLSX.equals(format);
    }

    /** Build a downloadable response for the given export table in the chosen binary format. */
    public static ResponseEntity<byte[]> binaryResponse(
        ExportTable table, String format, String filenameBase,
        ReportPdfExporter pdfExporter, ReportXlsxExporter xlsxExporter
    ) throws IOException {
        String safeBase = sanitize(filenameBase);
        if (FORMAT_PDF.equals(format)) {
            byte[] body = pdfExporter.export(table);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + safeBase + "-" + LocalDate.now() + ".pdf\"")
                .body(body);
        }
        if (FORMAT_XLSX.equals(format)) {
            byte[] body = xlsxExporter.export(table);
            return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + safeBase + "-" + LocalDate.now() + ".xlsx\"")
                .body(body);
        }
        throw new IllegalArgumentException("binaryResponse called with format=" + format);
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "report";
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
