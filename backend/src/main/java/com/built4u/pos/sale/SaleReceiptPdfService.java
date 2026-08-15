package com.built4u.pos.sale;

import com.built4u.pos.docsettings.DocSettings;
import com.built4u.pos.docsettings.DocSettingsService;
import com.built4u.pos.sale.dto.SaleDto;
import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * Renders a printable sales receipt (PDF) for one sale using OpenPDF and the
 * per-site {@link DocSettings} branding/templating: optional logo, header note,
 * field-visibility toggles, and a physical format — {@code THERMAL_80MM} (narrow
 * 80mm slip, the default) or {@code BOND_LETTER} (US Letter for dot-matrix).
 * Amounts are in PHP (no glyph, to stay font-safe).
 */
@Service
@RequiredArgsConstructor
public class SaleReceiptPdfService {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SaleService saleService;
    private final DocSettingsService docSettingsService;

    public byte[] generate(String salesNumber) throws IOException {
        return generate(salesNumber, null);
    }

    /** {@code formatOverride} (THERMAL_80MM / BOND_LETTER) wins over the site default when set. */
    public byte[] generate(String salesNumber, String formatOverride) throws IOException {
        SaleDto sale = saleService.get(salesNumber);
        DocSettings brand = docSettingsService.resolve();

        String format = normalizeFormat(formatOverride, brand.getReceiptFormat());
        boolean bond = "BOND_LETTER".equals(format);
        float scale = bond ? 1.25f : 1f;

        Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11 * scale, Color.BLACK);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 7 * scale, Color.DARK_GRAY);
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 8 * scale, Color.BLACK);
        Font bodyB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9 * scale, Color.BLACK);

        Rectangle page = bond ? PageSize.LETTER : new Rectangle(240f, 640f);
        float margin = bond ? 48f : 12f;

        Document doc = new Document(page, margin, margin, margin, margin);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            if (Boolean.TRUE.equals(brand.getShowLogoReceipt())
                    && brand.getLogoImage() != null && brand.getLogoImage().length > 0) {
                try {
                    Image logo = Image.getInstance(brand.getLogoImage());
                    logo.scaleToFit(bond ? 180 : 120, bond ? 80 : 52);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    doc.add(logo);
                } catch (Exception ignore) { /* bad image bytes — skip logo, keep the receipt */ }
            }

            String name = brand.getBusinessName() == null || brand.getBusinessName().isBlank()
                ? "Built4U POS" : brand.getBusinessName();
            add(doc, center(name, h));
            for (String line : new String[]{ brand.getAddressLine(), brand.getContactLine(),
                    brand.getTin() == null ? null : "TIN: " + brand.getTin() }) {
                if (line != null && !line.isBlank()) add(doc, center(line, small));
            }
            add(doc, center(brand.getReceiptTitle(), bodyB));
            if (brand.getReceiptHeaderNote() != null && !brand.getReceiptHeaderNote().isBlank()) {
                add(doc, center(brand.getReceiptHeaderNote(), small));
            }
            rule(doc);

            add(doc, line("Receipt: " + sale.salesNumber(), body));
            if (sale.creationDate() != null) add(doc, line("Date: " + sale.creationDate().format(DT), body));
            if (Boolean.TRUE.equals(brand.getReceiptShowCashier())) add(doc, line("Cashier: " + nz(sale.createdBy()), body));
            if (Boolean.TRUE.equals(brand.getReceiptShowCustomer()) && sale.customerName() != null) {
                add(doc, line("Customer: " + sale.customerName(), body));
            }
            if (sale.payments() == null || sale.payments().size() <= 1) {
                add(doc, line("Payment: " + nz(sale.modeOfPayment()), body));
            }
            rule(doc);

            for (var l : sale.lines()) {
                add(doc, line(nz(l.itemName()), body));
                add(doc, line("  " + trim(l.quantity()) + " x " + MONEY.format(nz(l.unitCost()))
                    + "   " + MONEY.format(nz(l.subTotal())), body));
            }
            rule(doc);

            add(doc, kv("Subtotal", sale.total(), body));
            if (nz(sale.totalDiscItem()).signum() > 0) add(doc, kv("Line discounts", sale.totalDiscItem().negate(), body));
            if (nz(sale.discountAll()).signum() > 0) add(doc, kv("Discount", sale.discountAll().negate(), body));
            add(doc, kv("TOTAL", sale.grandTotal(), bodyB));
            if (sale.payments() != null && !sale.payments().isEmpty()) {
                for (var p : sale.payments()) add(doc, kv(titleCase(p.mode()), p.tendered(), body));
            } else {
                add(doc, kv("Paid", sale.payment(), body));
            }
            add(doc, kv("Change", sale.change(), body));
            rule(doc);

            add(doc, center("Amounts in PHP", small));
            if (brand.getReceiptFooter() != null && !brand.getReceiptFooter().isBlank()) {
                add(doc, center(brand.getReceiptFooter(), body));
            }
            if ("VOIDED".equals(sale.status())) add(doc, center("*** VOIDED ***", bodyB));
            else if ("REFUNDED".equals(sale.status())) add(doc, center("*** REFUNDED ***", bodyB));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Receipt render failed: " + e.getMessage(), e);
        }
    }

    private static String normalizeFormat(String override, String siteDefault) {
        String v = override != null && !override.isBlank() ? override : siteDefault;
        if (v == null) return "THERMAL_80MM";
        v = v.trim().toUpperCase();
        return "BOND_LETTER".equals(v) ? "BOND_LETTER" : "THERMAL_80MM";
    }

    private static void add(Document doc, Paragraph p) throws DocumentException { doc.add(p); }

    private static Paragraph center(String text, Font f) {
        Paragraph p = new Paragraph(text, f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }
    private static Paragraph line(String text, Font f) { return new Paragraph(text, f); }

    private static Paragraph kv(String k, BigDecimal v, Font f) {
        Paragraph p = new Paragraph(k + ":  " + MONEY.format(nz(v)), f);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static void rule(Document doc) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(new LineSeparator(0.5f, 100f, Color.LIGHT_GRAY, Element.ALIGN_CENTER, -2)));
        doc.add(p);
    }

    private static String trim(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String nz(String s) { return s == null ? "" : s; }

    private static String titleCase(String s) {
        if (s == null || s.isBlank()) return "Paid";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
