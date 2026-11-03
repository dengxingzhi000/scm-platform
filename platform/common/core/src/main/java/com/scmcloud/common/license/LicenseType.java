package com.scmcloud.common.license;

public enum LicenseType {
    COMMUNITY("Community", "Open source community edition"),
    STANDARD("Standard", "Standard enterprise edition"),
    ENTERPRISE("Enterprise", "Full enterprise edition");

    private final String displayName;
    private final String description;

    LicenseType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public boolean isAtLeast(LicenseType other) {
        return this.ordinal() >= other.ordinal();
    }
}
