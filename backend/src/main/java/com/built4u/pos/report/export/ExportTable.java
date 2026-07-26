package com.built4u.pos.report.export;

import java.util.List;

/**
 * Format-neutral representation of a tabular report. Both the PDF exporter and
 * the xlsx exporter consume this same shape.
 *
 * <p>Cells in {@code rows} stay as typed Java values (BigDecimal, Number,
 * String, LocalDate, LocalDateTime). The xlsx exporter dispatches on the type
 * so Excel treats numbers as numbers; the PDF exporter formats each cell.
 */
public record ExportTable(
    String title,
    List<String> subtitle,    // contextual lines printed under the title
    List<String> headers,
    List<List<Object>> rows,
    List<String> footer       // post-table notes / totals
) {
    public ExportTable {
        if (subtitle == null) subtitle = List.of();
        if (footer == null) footer = List.of();
    }
}
