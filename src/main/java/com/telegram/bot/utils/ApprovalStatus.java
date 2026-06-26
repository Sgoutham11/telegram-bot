package com.telegram.bot.utils;

import io.micrometer.common.util.StringUtils;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ApprovalStatus {

    VERIFIED("VERIFIED", "Approved"),
    PROCESSED("PROCESSED", "Pending Approval"),
    DELETED("DELETED", "Deleted"),
    DELETE("DELETE", "Delete Approval"),
    BLOCK("BLOCK", "Blocked"),
    REJECT("REJECT", "Rejected"),
    INVALID_STATUS("INVALID", "Unknown");

    private final String value;
    private final String label;

    ApprovalStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static final Map<String, ApprovalStatus> valueMap = new HashMap<>();

    static {

        for(ApprovalStatus status : values()) {
            valueMap.put(status.getValue(), status);
        }
    }

    public static ApprovalStatus fromValue(String value) {

        if (StringUtils.isNotBlank(value) && valueMap.containsKey(value)) {
            return valueMap.get(value);
        }

        return INVALID_STATUS;
    }
}
