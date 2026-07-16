package com.example.demo.entity;

public enum OrderStatus {
    PENDING("Pending"),
    PRINTING("Printing"),
    READY("Ready for Pickup"),
    PICKED_UP("Picked Up"),
    CANCELLED("Cancelled");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The next step in the fulfilment flow, or null if this is a terminal state. */
    public OrderStatus next() {
        return switch (this) {
            case PENDING -> PRINTING;
            case PRINTING -> READY;
            case READY -> PICKED_UP;
            case PICKED_UP, CANCELLED -> null;
        };
    }

    public boolean isTerminal() {
        return this == PICKED_UP || this == CANCELLED;
    }
}
