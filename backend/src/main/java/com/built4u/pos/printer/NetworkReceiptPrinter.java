package com.built4u.pos.printer;

import com.built4u.pos.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Sends raw ESC/POS bytes to a LAN thermal printer over a TCP socket (RAW /
 * JetDirect, usually port 9100). Kept deliberately thin so the byte-building
 * ({@link EscPos}) stays independently testable; this is the only piece that
 * touches the network.
 */
@Component
@Slf4j
public class NetworkReceiptPrinter {

    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int WRITE_TIMEOUT_MS = 4000;

    public void send(String host, int port, byte[] data) {
        if (host == null || host.isBlank()) {
            throw new BadRequestException("No printer host configured");
        }
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(WRITE_TIMEOUT_MS);
            socket.connect(new InetSocketAddress(host.trim(), port), CONNECT_TIMEOUT_MS);
            OutputStream os = socket.getOutputStream();
            os.write(data);
            os.flush();
            log.info("Printed {} bytes to {}:{}", data.length, host, port);
        } catch (IOException e) {
            log.warn("Printer send failed to {}:{} — {}", host, port, e.toString());
            throw new BadRequestException("Could not reach the printer at " + host + ":" + port
                + " (" + e.getMessage() + ")");
        }
    }
}
