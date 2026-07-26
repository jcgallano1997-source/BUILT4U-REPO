package com.built4u.pos.payable;

/** Origin of a payable. Stored as a string. */
public enum PayableSource {
    PURCHASE,   // auto-created at Goods Receipt for AP-enabled suppliers
    EXPENSE     // manually entered (utilities, reimbursements, etc.)
}
