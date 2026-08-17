package com.built4u.pos.printer;

import com.built4u.pos.docsettings.DocSettings;
import com.built4u.pos.sale.dto.SaleDto;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/**
 * Builds ESC/POS byte streams for a network thermal printer (and the cash drawer
 * wired into it). Pure and side-effect-free — the byte output is deterministic so
 * it can be unit-tested without any hardware. Column width targets a common 80mm
 * printer (48 chars in Font A); amounts are ASCII (no peso glyph — CP437-safe).
 */
public final class EscPos {

    private EscPos() {}

    /** 80mm Font-A line width in characters. */
    public static final int WIDTH = 48;

    private static final Charset CP437 = pickCharset();
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── ESC/POS control codes ────────────────────────────────────────────────
    private static final byte ESC = 0x1B, GS = 0x1D;
    private static final byte[] INIT      = { ESC, '@' };                 // initialize
    private static final byte[] ALIGN_L   = { ESC, 'a', 0 };
    private static final byte[] ALIGN_C   = { ESC, 'a', 1 };
    private static final byte[] BOLD_ON   = { ESC, 'E', 1 };
    private static final byte[] BOLD_OFF  = { ESC, 'E', 0 };
    private static final byte[] DBL_ON    = { GS, '!', 0x11 };            // double width+height
    private static final byte[] DBL_OFF   = { GS, '!', 0x00 };
    private static final byte[] FEED_CUT  = { GS, 'V', 0x42, 0x00 };      // feed + partial cut

    /**
     * Cash-drawer kick: ESC p m t1 t2 — pulse drawer pin 2, ~50ms on / ~200ms off.
     * Sending this to the printer is what physically opens the drawer.
     */
    public static final byte[] DRAWER_KICK = { ESC, 'p', 0x00, 0x19, (byte) 0xFA };

    public static byte[] openDrawer() {
        return DRAWER_KICK.clone();
    }

    /** A short slip used to confirm connectivity + fire the drawer during setup. */
    public static byte[] testSlip(DocSettings brand, boolean openDrawer) {
        Buf b = new Buf();
        b.add(INIT).add(ALIGN_C).add(BOLD_ON).add(DBL_ON)
            .text(businessName(brand)).nl()
            .add(DBL_OFF).add(BOLD_OFF)
            .text("Printer test").nl()
            .text(java.time.LocalDateTime.now().format(DT)).nl().nl()
            .add(ALIGN_L).text("If you can read this, the network").nl()
            .text("printer is reachable.").nl();
        if (openDrawer) b.text("Drawer kick sent.").nl();
        b.nl().nl();
        if (openDrawer) b.add(DRAWER_KICK);
        b.add(FEED_CUT);
        return b.bytes();
    }

    /** Full sale receipt honoring the per-site branding toggles; optional drawer kick. */
    public static byte[] receipt(SaleDto sale, DocSettings brand, boolean openDrawer) {
        Buf b = new Buf();
        b.add(INIT);

        // Header — business identity, centered.
        b.add(ALIGN_C).add(BOLD_ON).text(businessName(brand)).nl().add(BOLD_OFF);
        line(b, brand.getAddressLine());
        line(b, brand.getContactLine());
        line(b, brand.getTin() == null || brand.getTin().isBlank() ? null : "TIN: " + brand.getTin());
        b.add(BOLD_ON).text(orDefault(brand.getReceiptTitle(), "SALES RECEIPT")).nl().add(BOLD_OFF);
        line(b, brand.getReceiptHeaderNote());
        b.add(ALIGN_L);
        rule(b);

        // Meta.
        b.text("Receipt: " + nz(sale.salesNumber())).nl();
        if (sale.creationDate() != null) b.text("Date: " + sale.creationDate().format(DT)).nl();
        if (Boolean.TRUE.equals(brand.getReceiptShowCashier())) b.text("Cashier: " + nz(sale.createdBy())).nl();
        if (Boolean.TRUE.equals(brand.getReceiptShowCustomer()) && sale.customerName() != null) {
            b.text("Customer: " + sale.customerName()).nl();
        }
        if (sale.payments() == null || sale.payments().size() <= 1) {
            b.text("Payment: " + nz(sale.modeOfPayment())).nl();
        }
        rule(b);

        // Line items — name on its own line, then "qty x price" left with amount right.
        for (var l : sale.lines()) {
            b.text(clip(nz(l.itemName()), WIDTH)).nl();
            String left = "  " + trim(l.quantity()) + " x " + MONEY.format(nz(l.unitCost()));
            b.text(twoCol(left, MONEY.format(nz(l.subTotal())))).nl();
        }
        rule(b);

        // Totals.
        b.text(twoCol("Subtotal", MONEY.format(nz(sale.total())))).nl();
        if (nz(sale.totalDiscItem()).signum() > 0) b.text(twoCol("Line discounts", "-" + MONEY.format(sale.totalDiscItem()))).nl();
        if (nz(sale.discountAll()).signum() > 0) b.text(twoCol("Discount", "-" + MONEY.format(sale.discountAll()))).nl();
        b.add(BOLD_ON).text(twoCol("TOTAL", MONEY.format(nz(sale.grandTotal())))).nl().add(BOLD_OFF);
        if (sale.payments() != null && !sale.payments().isEmpty()) {
            for (var p : sale.payments()) b.text(twoCol(titleCase(p.mode()), MONEY.format(nz(p.tendered())))).nl();
        } else {
            b.text(twoCol("Paid", MONEY.format(nz(sale.payment())))).nl();
        }
        b.text(twoCol("Change", MONEY.format(nz(sale.change())))).nl();
        rule(b);

        // Footer.
        b.add(ALIGN_C).text("Amounts in PHP").nl();
        line(b, brand.getReceiptFooter());
        if ("VOIDED".equals(sale.status())) b.add(BOLD_ON).text("*** VOIDED ***").nl().add(BOLD_OFF);
        else if ("REFUNDED".equals(sale.status())) b.add(BOLD_ON).text("*** REFUNDED ***").nl().add(BOLD_OFF);

        b.nl().nl();
        if (openDrawer) b.add(DRAWER_KICK);
        b.add(FEED_CUT);
        return b.bytes();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String businessName(DocSettings brand) {
        return orDefault(brand == null ? null : brand.getBusinessName(), "Built4U POS");
    }

    /** Center-aligned optional line (skips blanks). Assumes caller set alignment. */
    private static void line(Buf b, String text) {
        if (text != null && !text.isBlank()) b.text(clip(text.trim(), WIDTH)).nl();
    }

    private static void rule(Buf b) {
        b.text("-".repeat(WIDTH)).nl();
    }

    /** Left text + right text padded to {@link #WIDTH}; right wins if they collide. */
    static String twoCol(String left, String right) {
        left = left == null ? "" : left;
        right = right == null ? "" : right;
        int pad = WIDTH - left.length() - right.length();
        if (pad < 1) {
            int keep = Math.max(0, WIDTH - right.length() - 1);
            left = clip(left, keep);
            pad = Math.max(1, WIDTH - left.length() - right.length());
        }
        return left + " ".repeat(pad) + right;
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String orDefault(String s, String def) {
        return s == null || s.isBlank() ? def : s.trim();
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

    private static Charset pickCharset() {
        try { return Charset.forName("IBM437"); }
        catch (Exception e) { return java.nio.charset.StandardCharsets.US_ASCII; }
    }

    /** Small append-only byte buffer with an ESC/POS-encoded text writer. */
    private static final class Buf {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Buf add(byte[] bytes) { out.writeBytes(bytes); return this; }
        Buf text(String s) { out.writeBytes(s.getBytes(CP437)); return this; }
        Buf nl() { out.write('\n'); return this; }
        byte[] bytes() { return out.toByteArray(); }
    }
}
