package com.built4u.pos.report.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Self-contained PDF renderer for an {@link ExportTable}, drawn directly with
 * OpenPDF (no HTML/templating — per-doc branding is a later phase). Landscape A4
 * with a title, subtitle lines, a shaded header row, type-formatted cells
 * (numbers right-aligned), and italic footer notes.
 */
@Component
public class ReportPdfExporter {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color HEADER_BG = new Color(0xE2, 0xE8, 0xF0);

    public byte[] export(ExportTable table) throws IOException {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0x1E, 0x29, 0x3B));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(0x33, 0x41, 0x55));
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            Paragraph title = new Paragraph("Built4U  ·  " + safe(table.title()), titleFont);
            title.setSpacingAfter(4f);
            doc.add(title);
            for (String s : table.subtitle()) doc.add(new Paragraph(safe(s), subFont));

            int cols = Math.max(1, table.headers().size());
            PdfPTable pdf = new PdfPTable(cols);
            pdf.setWidthPercentage(100f);
            pdf.setSpacingBefore(10f);
            pdf.setHeaderRows(1);

            for (String h : table.headers()) {
                PdfPCell c = new PdfPCell(new Phrase(safe(h), headFont));
                c.setBackgroundColor(HEADER_BG);
                c.setPadding(4f);
                c.setBorderColor(Color.LIGHT_GRAY);
                pdf.addCell(c);
            }

            for (List<Object> row : table.rows()) {
                for (int i = 0; i < cols; i++) {
                    Object v = i < row.size() ? row.get(i) : null;
                    PdfPCell c = new PdfPCell(new Phrase(format(v), cellFont));
                    c.setPadding(3f);
                    c.setBorderColor(Color.LIGHT_GRAY);
                    if (v instanceof Number) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    pdf.addCell(c);
                }
            }
            doc.add(pdf);

            if (!table.footer().isEmpty()) {
                Paragraph gap = new Paragraph(" ", footFont);
                gap.setSpacingBefore(6f);
                doc.add(gap);
                for (String s : table.footer()) doc.add(new Paragraph(safe(s), footFont));
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("PDF render failed: " + e.getMessage(), e);
        }
    }

    private static String format(Object v) {
        if (v == null) return "";
        if (v instanceof BigDecimal bd) return MONEY.format(bd);
        if (v instanceof Number n) return String.valueOf(n);
        if (v instanceof LocalDate ld) return ld.format(D);
        if (v instanceof LocalDateTime ldt) return ldt.format(DT);
        return v.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
