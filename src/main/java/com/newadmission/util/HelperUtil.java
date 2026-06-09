package com.newadmission.util;

import io.micrometer.common.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Objects;

public class HelperUtil {
    public static String DATE_FORMAT = "uuuu-MM-dd";

    public static String getDateWithFormat(LocalDate date) {
        if (Objects.nonNull(date)) {
            return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        }
        return "";
    }

    public static LocalDate parseDateWithFormat(String date) {
        LocalDate parsedDate = null;
        if (StringUtils.isNotBlank(date)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            parsedDate = LocalDate.parse(date, formatter);
        }
        return parsedDate;
    }

    public static boolean isStrictlyValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr,
                    DateTimeFormatter.ofPattern(DATE_FORMAT)
                            .withResolverStyle(ResolverStyle.STRICT)
            );
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
