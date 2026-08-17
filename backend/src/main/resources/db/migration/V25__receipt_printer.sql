-- =============================================================================
-- Built4U POS — V25: network receipt printer + cash-drawer config (per site).
-- =============================================================================
-- A LAN/network ESC-POS thermal printer is reached by opening a TCP socket to
-- host:port (9100 is the standard RAW/JetDirect port) and writing ESC-POS bytes.
-- The cash drawer is wired into the printer and pops open on the ESC-POS "kick"
-- command — so the same channel drives both. Per-site so each branch can point
-- at its own printer. BUILT4U only; FREEPOS is never touched.
-- =============================================================================

ALTER TABLE pos_doc_settings ADD (
  receipt_printer_host    VARCHAR2(100 CHAR),
  receipt_printer_port    NUMBER(10)       DEFAULT 9100 NOT NULL,
  receipt_printer_enabled VARCHAR2(1 CHAR) DEFAULT 'N'  NOT NULL,
  open_drawer_on_sale     VARCHAR2(1 CHAR) DEFAULT 'N'  NOT NULL
);
