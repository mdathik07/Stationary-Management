package com.example.demo.entity;

public enum PrintType {
    BW("Black & White"),
    COLOR("Color");

    private final String displayName;

    PrintType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
