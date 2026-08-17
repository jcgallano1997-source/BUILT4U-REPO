package com.built4u.pos.printer;

import com.built4u.pos.docsettings.DocSettings;
import com.built4u.pos.sale.dto.PaymentLineDto;
import com.built4u.pos.sale.dto.SaleDto;
import com.built4u.pos.sale.dto.SaleLineDto;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure, hardware-free verification of the ESC/POS byte output and the network
 * transport: a loopback {@link ServerSocket} stands in for the printer and
 * captures exactly what the backend would send — so we can assert the receipt
 * text and the cash-drawer kick without a physical printer or drawer.
 */
class EscPosPrinterTest {

    private static DocSettings brand() {
        return DocSettings.builder()
            .siteId(1L).businessName("Aling Nena Hardware")
            .addressLine("123 Rizal St, Laguna").contactLine("0917-000-0000")
            .receiptTitle("SALES RECEIPT").receiptFooter("Thank you! Come again")
            .receiptShowCashier(true).receiptShowCustomer(true)
            .build();
    }

    private static SaleDto sale() {
        SaleLineDto line = new SaleLineDto(57L, "Hex Bolt 10mm", "PC",
            new BigDecimal("3"), BigDecimal.ZERO, new BigDecimal("25.00"),
            new BigDecimal("25.00"), new BigDecimal("18.00"), null, null,
            new BigDecimal("75.00"), BigDecimal.ZERO, new BigDecimal("3"));
        PaymentLineDto pay = new PaymentLineDto("CASH", new BigDecimal("75.00"),
            new BigDecimal("100.00"), new BigDecimal("25.00"), null);
        return new SaleDto("S-2026-0001", new BigDecimal("75.00"), BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("75.00"), new BigDecimal("100.00"), new BigDecimal("25.00"), "CASH",
            null, null, null, "COMPLETED", 0, LocalDateTime.now(), "cashier1",
            List.of(line), List.of(pay));
    }

    @Test
    void drawerKickIsTheStandardEscPosPulse() {
        assertThat(EscPos.openDrawer())
            .containsExactly(0x1B, 0x70, 0x00, 0x19, 0xFA);
    }

    @Test
    void receiptBytesCarryInitBusinessTotalsAndDrawerKick() {
        byte[] bytes = EscPos.receipt(sale(), brand(), true);
        String text = new String(bytes, StandardCharsets.US_ASCII);

        // Initializes the printer, prints identity + line item + totals.
        assertThat(bytes[0]).isEqualTo((byte) 0x1B);   // ESC
        assertThat(bytes[1]).isEqualTo((byte) '@');    // @  → ESC @ init
        assertThat(text).contains("Aling Nena Hardware")
            .contains("Hex Bolt 10mm")
            .contains("TOTAL")
            .contains("Change")
            .contains("Thank you! Come again");

        // Ends with the drawer kick immediately before the feed+cut.
        assertThat(indexOf(bytes, EscPos.DRAWER_KICK)).isGreaterThan(0);
    }

    @Test
    void receiptWithoutDrawerHasNoKick() {
        byte[] bytes = EscPos.receipt(sale(), brand(), false);
        assertThat(indexOf(bytes, EscPos.DRAWER_KICK)).isEqualTo(-1);
    }

    @Test
    void networkPrinterSendsTheExactBytesOverTcp() throws Exception {
        byte[] payload = EscPos.receipt(sale(), brand(), true);

        try (ServerSocket server = new ServerSocket(0)) {   // ephemeral loopback port
            int port = server.getLocalPort();
            CompletableFuture<byte[]> received = CompletableFuture.supplyAsync(() -> {
                try (Socket s = server.accept(); InputStream in = s.getInputStream()) {
                    return in.readAllBytes();
                } catch (Exception e) { throw new RuntimeException(e); }
            });

            new NetworkReceiptPrinter().send("127.0.0.1", port, payload);

            assertThat(received.get(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(payload);
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
