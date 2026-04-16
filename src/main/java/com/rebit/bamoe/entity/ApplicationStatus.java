package com.rebit.bamoe.entity;

public enum ApplicationStatus {
    NEW("New Application"),
    SUBMITTED("Submitted for Review"),
    MAKER_REVIEW("Under Maker Review"),
    MAKER_APPROVED("Approved by Maker"),
    MAKER_REJECTED("Rejected by Maker"),
    EDIT_REQUESTED("Edit Requested by Maker"),
    CHECKER_REVIEW("Under Checker Review"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}