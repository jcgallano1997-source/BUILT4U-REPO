-- =============================================================================
-- Built4U POS — V28: drop the PO approver pool (reverts V27).
--
-- The configurable approver list added in V27 was rolled back; PO approver
-- routing is once again "any active user may be picked as an approver", handled
-- entirely by pos_po_approver. Nothing reads pos_po_approver_pool any more.
--
-- V27 is deliberately left in place rather than deleted: it has already been
-- applied on existing databases, and removing the file would fail Flyway's
-- validation ("applied migration not resolved locally") on the next start.
-- =============================================================================

DROP TABLE pos_po_approver_pool;
