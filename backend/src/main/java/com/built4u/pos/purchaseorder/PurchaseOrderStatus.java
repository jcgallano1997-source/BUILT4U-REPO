package com.built4u.pos.purchaseorder;

public enum PurchaseOrderStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CANCELLED;

    public boolean canEdit() {
        return this == DRAFT;
    }

    public boolean canApprove() {
        return this == DRAFT;
    }

    public boolean canReceive() {
        return this == APPROVED || this == PARTIALLY_RECEIVED;
    }

    public boolean canCancel() {
        return this == DRAFT || this == APPROVED;
    }
}
