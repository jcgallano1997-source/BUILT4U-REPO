-- =============================================================================
-- Built4U POS (single-business) — V1 core baseline.
-- =============================================================================
-- De-tenanted fork of FreePOS. There is ONE implicit business, so:
--   * NO app_entity_pos / entity_id anywhere — site_id is the top isolation key.
--   * username and site.code are globally unique (one business owns them all).
--   * Objects use the `pos_` prefix (isolated in the BUILT4U schema; the live
--     FreePOS product owns the app_%_pos namespace in the FREEPOS schema).
--   * Every length-bearing VARCHAR2 uses CHAR semantics (Oracle BYTE-vs-CHAR
--     trap: bytes overflow on multi-byte input -> ORA-12899).
--
-- This V1 covers the CORE only (auth / site / RBAC). Domain tables (inventory,
-- sales, procurement, AR/AP, loyalty, ...) arrive in later migrations as each
-- module is ported.
-- =============================================================================


-- =============================================================================
-- SECTION 1 — Auth / identity
-- =============================================================================

CREATE TABLE pos_user (
  id                   NUMBER(19)         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  username             VARCHAR2(255 CHAR) NOT NULL,
  password_hash        VARCHAR2(72 CHAR)  NOT NULL,
  full_name            VARCHAR2(150 CHAR) NOT NULL,
  email                VARCHAR2(255 CHAR),
  active               VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  failed_attempts      NUMBER(3)          DEFAULT 0   NOT NULL,
  locked_until         TIMESTAMP,
  password_changed_at  TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  must_change_password VARCHAR2(1 CHAR)   DEFAULT 'N' NOT NULL,
  last_login_at        TIMESTAMP,
  created_at           TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  created_by           VARCHAR2(50 CHAR),
  updated_at           TIMESTAMP,
  updated_by           VARCHAR2(50 CHAR),
  CONSTRAINT uq_pos_user_username UNIQUE (username),
  CONSTRAINT uq_pos_user_email    UNIQUE (email),
  CONSTRAINT ck_pos_user_active   CHECK (active IN ('Y','N')),
  CONSTRAINT ck_pos_user_mustchg  CHECK (must_change_password IN ('Y','N'))
);

CREATE TABLE pos_role (
  id          NUMBER(10)        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code        VARCHAR2(30 CHAR) NOT NULL,
  name        VARCHAR2(80 CHAR) NOT NULL,
  description VARCHAR2(255 CHAR),
  built_in    VARCHAR2(1 CHAR)  DEFAULT 'N' NOT NULL,
  wildcard    VARCHAR2(1 CHAR)  DEFAULT 'N' NOT NULL,
  CONSTRAINT uq_pos_role_code     UNIQUE (code),
  CONSTRAINT ck_pos_role_built_in CHECK (built_in IN ('Y','N')),
  CONSTRAINT ck_pos_role_wildcard CHECK (wildcard IN ('Y','N'))
);

CREATE TABLE pos_user_role (
  user_id NUMBER(19) NOT NULL,
  role_id NUMBER(10) NOT NULL,
  CONSTRAINT pk_pos_user_role      PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_pos_user_role_user FOREIGN KEY (user_id) REFERENCES pos_user(id),
  CONSTRAINT fk_pos_user_role_role FOREIGN KEY (role_id) REFERENCES pos_role(id)
);

CREATE TABLE pos_password_reset_token (
  id            NUMBER(19)        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id       NUMBER(19)        NOT NULL,
  token_hash    VARCHAR2(64 CHAR) NOT NULL,
  expires_at    TIMESTAMP         NOT NULL,
  used_at       TIMESTAMP,
  requested_at  TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  requested_ip  VARCHAR2(64 CHAR),
  CONSTRAINT uq_pos_pwreset_hash UNIQUE (token_hash),
  CONSTRAINT fk_pos_pwreset_user FOREIGN KEY (user_id) REFERENCES pos_user(id)
);
CREATE INDEX ix_pos_pwreset_user ON pos_password_reset_token (user_id, used_at);


-- =============================================================================
-- SECTION 2 — Site structure (branches of the one business)
-- =============================================================================

CREATE TABLE pos_site (
  id          NUMBER(10)         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code        VARCHAR2(20 CHAR)  NOT NULL,
  name        VARCHAR2(100 CHAR) NOT NULL,
  address     VARCHAR2(500 CHAR),
  active      VARCHAR2(1 CHAR)   DEFAULT 'Y' NOT NULL,
  created_at  TIMESTAMP          DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uq_pos_site_code   UNIQUE (code),
  CONSTRAINT ck_pos_site_active CHECK (active IN ('Y','N'))
);

CREATE TABLE pos_user_site (
  user_id NUMBER(19) NOT NULL,
  site_id NUMBER(10) NOT NULL,
  CONSTRAINT pk_pos_user_site      PRIMARY KEY (user_id, site_id),
  CONSTRAINT fk_pos_user_site_user FOREIGN KEY (user_id) REFERENCES pos_user(id),
  CONSTRAINT fk_pos_user_site_site FOREIGN KEY (site_id) REFERENCES pos_site(id)
);

-- Refresh tokens carry the site_id chosen at login (so refresh keeps same site).
CREATE TABLE pos_refresh_token (
  id         NUMBER(19)        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id    NUMBER(19)        NOT NULL,
  site_id    NUMBER(10),
  token_hash VARCHAR2(64 CHAR) NOT NULL,
  expires_at TIMESTAMP         NOT NULL,
  revoked_at TIMESTAMP,
  created_at TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uq_pos_refresh_hash UNIQUE (token_hash),
  CONSTRAINT fk_pos_refresh_user FOREIGN KEY (user_id) REFERENCES pos_user(id),
  CONSTRAINT fk_pos_refresh_site FOREIGN KEY (site_id) REFERENCES pos_site(id)
);
CREATE INDEX ix_pos_refresh_user ON pos_refresh_token (user_id);


-- =============================================================================
-- SECTION 3 — RBAC / module catalog
-- =============================================================================

CREATE TABLE pos_module (
  code        VARCHAR2(40 CHAR) NOT NULL,
  name        VARCHAR2(80 CHAR) NOT NULL,
  description VARCHAR2(255 CHAR),
  sort_order  NUMBER(5)         DEFAULT 100 NOT NULL,
  CONSTRAINT pk_pos_module PRIMARY KEY (code)
);

CREATE TABLE pos_role_module (
  role_id     NUMBER(19)        NOT NULL,
  module_code VARCHAR2(40 CHAR) NOT NULL,
  CONSTRAINT pk_pos_role_module        PRIMARY KEY (role_id, module_code),
  CONSTRAINT fk_pos_role_module_role   FOREIGN KEY (role_id)     REFERENCES pos_role(id),
  CONSTRAINT fk_pos_role_module_module FOREIGN KEY (module_code) REFERENCES pos_module(code)
);
CREATE INDEX ix_pos_role_module_role ON pos_role_module (role_id);
