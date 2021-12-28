package com.ryan.plugin.enums;

public enum SupportedFileType {

    JAVA("JAVA");

    private final String type;

    SupportedFileType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static Boolean contains(String fileType) {
        for (SupportedFileType val : values()) {
            if (fileType.equals(val.getType())) {
                return true;
            }
        }
        return false;
    }

    public static SupportedFileType get(String fileType) {
        for (SupportedFileType val : values()) {
            if (fileType.equals(val.getType())) {
                return val;
            }
        }
        return null;
    }
}
