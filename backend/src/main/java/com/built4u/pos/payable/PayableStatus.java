package com.built4u.pos.payable;

/** Lifecycle of a {@code pos_payable} row. Stored as a string. */
public enum PayableStatus {
    OPEN, PARTIAL, PAID, CANCELLED
}
