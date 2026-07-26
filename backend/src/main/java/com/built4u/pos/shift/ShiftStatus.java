package com.built4u.pos.shift;

/** Lifecycle of a cashier shift: OPEN from count-in until close + reconcile. */
public enum ShiftStatus {
    OPEN,
    CLOSED
}
