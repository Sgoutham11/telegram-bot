package com.telegram.bot.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Utility {
    public static Map<String, String> calculateDateDetails() {
        Map<String, String> dateDetails = new HashMap<>();

        // Current timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String formattedTimestamp = sdf.format(new Date());
        dateDetails.put("timestamp", formattedTimestamp);

        // Current month
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
        dateDetails.put("month", month < 10 ? "0" + month : String.valueOf(month));

        // Current day
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        dateDetails.put("currentDay", day < 10 ? "0" + day : String.valueOf(day));

        // Previous day
        int previousDay = day - 1;
        dateDetails.put("previousDay", previousDay < 10 ? "0" + previousDay : String.valueOf(previousDay));

        return dateDetails;
    }
}
