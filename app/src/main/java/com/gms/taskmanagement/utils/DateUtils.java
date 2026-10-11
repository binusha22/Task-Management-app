package com.gms.taskmanagement.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATETIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());

    public static String getTodayDate() {
        return DISPLAY_DATE_FORMAT.format(new Date());
    }

    public static String formatDisplayDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "";
        try {
            Date date = DISPLAY_DATE_FORMAT.parse(rawDate.trim());
            return DISPLAY_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            return rawDate;
        }
    }

    public static String getCurrentTimestamp() {
        return DISPLAY_DATETIME_FORMAT.format(new Date());
    }
}
