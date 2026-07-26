package com.built4u.pos.report.export;

import com.built4u.pos.docsettings.DocSettings;
import com.built4u.pos.docsettings.DocSettingsService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
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
 * OpenPDF. Landscape A4 with a branded letterhead (business name/address/
 * contact/TIN + accent colour from {@link DocSettings}), the report title,
 * subtitle lines, an accent-shaded header row, type-formatted cells (numbers
 * right-aligned), and italic footer notes.
 */
@Component
@RequiredArgsConstructor
public class ReportPdfExporter {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color DEFAULT_ACCENT = new Color(0x1D, 0x4E, 0xD8);

    private final DocSettingsService docSettingsService;

    public byte[] export(ExportTable table) throws IOException {
        DocSettings brand = docSettingsService.resolve();
        Color accent = parseColor(brand.getAccentColor(), DEFAULT_ACCENT);
        String businessName = brand.getBusinessName() == null || brand.getBusinessName().isBlank()
            ? "Built4U POS" : brand.getBusinessName();

        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, accent);
            Font brandSub = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(0x1E, 0x29, 0x3B));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            // Branded letterhead.
            Paragraph brandLine = new Paragraph(businessName, brandFont);
            brandLine.setSpacingAfter(1f);
            doc.add(brandLine);
            for (String line : new String[]{ brand.getAddressLine(), brand.getContactLine(),
                    brand.getTin() == null ? null : "TIN: " + brand.getTin() }) {
                if (line != null && !line.isBlank()) doc.add(new Paragraph(line, brandSub));
            }

            Paragraph title = new Paragraph(safe(table.title()), titleFont);
            title.setSpacingBefore(8f);
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
                c.setBackgroundColor(accent);
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
            if (brand.getFooterNote() != null && !brand.getFooterNote().isBlank()) {
                Paragraph note = new Paragraph(brand.getFooterNote(), footFont);
                note.setSpacingBefore(8f);
                doc.add(note);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("PDF render failed: " + e.getMessage(), e);
        }
    }

    private static Color parseColor(String hex, Color fallback) {
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) return fallback;
        return new Color(Integer.parseInt(hex.substring(1, 3), 16),
                         Integer.parseInt(hex.substring(3, 5), 16),
                         Integer.parseInt(hex.substring(5, 7), 16));
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
