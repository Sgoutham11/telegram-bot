package com.telegram.bot.utils;

import io.micrometer.common.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum YOrN {
    Y, N, INVALID;

    public static final Map<String, YOrN> valueMap = new HashMap<>();

    static {

        for (YOrN status : values()) {
            valueMap.put(status.toString(), status);
        }
    }

    public static YOrN fromValue(String value) {

        if (StringUtils.isNotBlank(value) && valueMap.containsKey(value)) {
            return valueMap.get(value);
        }

        return INVALID;
    }
}
