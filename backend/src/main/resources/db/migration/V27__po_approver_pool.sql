-- =============================================================================
-- Built4U POS — V27: PO approver pool.
--
-- Which users may be *picked* as a PO approver, as opposed to pos_po_approver
-- which records who each creator routes TO. Business-wide (not site-scoped),
-- matching pos_po_approver.
--
-- Holders of the built-in OWNER role are always eligible and are deliberately
-- NOT stored here: eligibility is "holds the OWNER role, OR has a row in this
-- table". That keeps the owner impossible to remove and means the rule can't
-- drift if the owner account is renamed or replaced.
--
-- VARCHAR2(n CHAR); TIMESTAMP dates.
-- =============================================================================

CREATE TABLE pos_po_approver_pool (
  user_id           NUMBER             NOT NULL,
  creation_date     TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by        VARCHAR2(50 CHAR),
  CONSTRAINT pk_pos_po_approver_pool PRIMARY KEY (user_id)
);

-- Anyone already acting as an approver stays one, so existing routing keeps
-- working (the owner is covered by the role rule and needs no row).
INSERT INTO pos_po_approver_pool (user_id, created_by)
SELECT DISTINCT approver_user_id, 'V27' FROM pos_po_approver;
