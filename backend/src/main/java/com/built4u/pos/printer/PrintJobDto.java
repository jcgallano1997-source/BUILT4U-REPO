package com.built4u.pos.printer;

/**
 * A print job handed to the browser instead of being sent from here: the printer
 * target plus the ESC/POS bytes, base64-encoded.
 *
 * <p>Needed once the backend is hosted off-site. The printer sits on the shop's
 * LAN behind a private address, which a cloud server has no route to — but the
 * browser is in the shop, so it can relay the bytes to a local print agent.
 * The bytes are identical either way; only the last hop changes.
 */
public record PrintJobDto(
    String host,
    int port,
    String dataBase64
) {}
