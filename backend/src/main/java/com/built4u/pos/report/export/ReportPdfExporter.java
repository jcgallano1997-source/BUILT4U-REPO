package com.built4u.pos.report.export;

import com.built4u.pos.docsettings.DocSettings;
import com.built4u.pos.docsettings.DocSettingsService;
import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * OpenPDF. Branding & layout come from {@link DocSettings}: optional logo,
 * business letterhead, accent colour, paper size / orientation / margins,
 * font scale, zebra-striped rows, and a footer band (page number / timestamp /
 * printed-by) — each individually toggleable.
 */
@Component
@RequiredArgsConstructor
public class ReportPdfExporter {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color DEFAULT_ACCENT = new Color(0x1D, 0x4E, 0xD8);
    private static final Color ZEBRA = new Color(0xF3, 0xF5, 0xF9);

    private final DocSettingsService docSettingsService;

    public byte[] export(ExportTable table) throws IOException {
        DocSettings brand = docSettingsService.resolve();
        Color accent = parseColor(brand.getAccentColor(), DEFAULT_ACCENT);
        float fs = fontScale(brand.getFontScale());
        String businessName = brand.getBusinessName() == null || brand.getBusinessName().isBlank()
            ? "Built4U POS" : brand.getBusinessName();

        Rectangle base = "LETTER".equalsIgnoreCase(brand.getPaperSize()) ? PageSize.LETTER : PageSize.A4;
        Rectangle pageSize = "PORTRAIT".equalsIgnoreCase(brand.getOrientation()) ? base : base.rotate();
        float margin = margin(brand.getMarginPreset());

        Document doc = new Document(pageSize, margin, margin, margin, margin + 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterBand(brand, fs));
            doc.open();

            // Optional logo above the letterhead.
            if (Boolean.TRUE.equals(brand.getShowLogoPdf()) && hasLogo(brand)) {
                try {
                    Image logo = Image.getInstance(brand.getLogoImage());
                    logo.scaleToFit(140, 52);
                    logo.setAlignment(align(brand.getLogoPosition()));
                    doc.add(logo);
                } catch (Exception ignore) { /* bad image bytes — skip logo, keep the report */ }
            }

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15 * fs, accent);
            Font brandSub = FontFactory.getFont(FontFactory.HELVETICA, 8 * fs, Color.DARK_GRAY);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12 * fs, new Color(0x1E, 0x29, 0x3B));
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9 * fs, Color.DARK_GRAY);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8 * fs, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8 * fs, Color.BLACK);
            Font footFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8 * fs, Color.GRAY);

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

            boolean zebra = Boolean.TRUE.equals(brand.getZebraStriping());
            int r = 0;
            for (List<Object> row : table.rows()) {
                boolean shade = zebra && (r % 2 == 1);
                for (int i = 0; i < cols; i++) {
                    Object v = i < row.size() ? row.get(i) : null;
                    PdfPCell c = new PdfPCell(new Phrase(format(v), cellFont));
                    c.setPadding(3f);
                    c.setBorderColor(Color.LIGHT_GRAY);
                    if (shade) c.setBackgroundColor(ZEBRA);
                    if (v instanceof Number) c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    pdf.addCell(c);
                }
                r++;
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

    /** Bottom-of-page band: printed-by (left), timestamp (centre), page number (right). */
    private static final class FooterBand extends PdfPageEventHelper {
        private final DocSettings brand;
        private final Font font;
        private final String printedBy;

        FooterBand(DocSettings brand, float fs) {
            this.brand = brand;
            this.font = FontFactory.getFont(FontFactory.HELVETICA, 7 * fs, Color.GRAY);
            this.printedBy = currentUsername();
        }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            float y = doc.bottom() - 12;
            if (Boolean.TRUE.equals(brand.getShowPrintedBy())) {
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Printed by " + printedBy, font), doc.left(), y, 0);
            }
            if (Boolean.TRUE.equals(brand.getShowTimestamp())) {
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(LocalDateTime.now().format(DT), font),
                    (doc.left() + doc.right()) / 2, y, 0);
            }
            if (Boolean.TRUE.equals(brand.getShowPageNumbers())) {
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), font), doc.right(), y, 0);
            }
        }
    }

    private static boolean hasLogo(DocSettings d) { return d.getLogoImage() != null && d.getLogoImage().length > 0; }

    private static int align(String pos) {
        if ("CENTER".equalsIgnoreCase(pos)) return Element.ALIGN_CENTER;
        if ("RIGHT".equalsIgnoreCase(pos)) return Element.ALIGN_RIGHT;
        return Element.ALIGN_LEFT;
    }

    private static float margin(String preset) {
        if ("NARROW".equalsIgnoreCase(preset)) return 24f;
        if ("WIDE".equalsIgnoreCase(preset)) return 54f;
        return 36f;
    }

    private static float fontScale(String scale) {
        if ("SMALL".equalsIgnoreCase(scale)) return 0.85f;
        if ("LARGE".equalsIgnoreCase(scale)) return 1.15f;
        return 1f;
    }

    private static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SYSTEM" : a.getName();
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
