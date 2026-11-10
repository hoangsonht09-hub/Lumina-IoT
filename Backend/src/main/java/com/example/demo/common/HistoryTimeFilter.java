package com.example.demo.common;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.http.HttpStatus;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HistoryTimeFilter(
        int year,
        Integer month,
        Integer day,
        Integer hour,
        Integer minute,
        Integer second
) {
    private static final ZoneId HISTORY_ZONE = ZoneId.of("Asia/Bangkok");
    private static final Pattern DATE_TIME = Pattern.compile(
            "^(\\d{4})(?:[-/](\\d{1,2})(?:[-/](\\d{1,2}))?)?(?: (\\d{1,2})(?::(\\d{1,2})(?::(\\d{1,2}))?)?)?$"
    );
    public static HistoryTimeFilter parse(String value) {
        if (value == null || value.isBlank()) return null;

        // A trailing separator left after deleting the last time/date part is harmless.
        String normalized = value.trim()
                .replaceAll("\\s+", " ")
                .replaceFirst("[\\s:/-]+$", "");
        Matcher dateTime = DATE_TIME.matcher(normalized);
        if (dateTime.matches()) {
            int year = number(dateTime, 1);
            Integer month = optionalNumber(dateTime, 2);
            Integer day = optionalNumber(dateTime, 3);
            Integer hour = optionalNumber(dateTime, 4);
            Integer minute = optionalNumber(dateTime, 5);
            Integer second = optionalNumber(dateTime, 6);

            if ((day != null && month == null)
                    || (hour != null && day == null)
                    || (minute != null && hour == null)
                    || (second != null && minute == null)) {
                throw invalidTimeQuery();
            }
            if (day != null) {
                validateDate(year, month, day);
            } else if (month != null) {
                validateMonth(year, month);
            } else {
                validateYear(year);
            }
            validateTime(hour, minute, second);
            return new HistoryTimeFilter(year, month, day, hour, minute, second);
        }

        throw invalidTimeQuery();
    }

    public Predicate toPredicate(CriteriaBuilder cb, Expression<OffsetDateTime> timestamp) {
        LocalDate date = LocalDate.of(year, month == null ? 1 : month, day == null ? 1 : day);
        LocalDateTime start = date.atStartOfDay();
        if (hour != null) start = start.withHour(hour);
        if (minute != null) start = start.withMinute(minute);
        if (second != null) start = start.withSecond(second);

        LocalDateTime end = second != null ? start.plusSeconds(1)
                : minute != null ? start.plusMinutes(1)
                : hour != null ? start.plusHours(1)
                : day != null ? start.plusDays(1)
                : month != null ? start.plusMonths(1)
                : start.plusYears(1);

        OffsetDateTime from = start.atZone(HISTORY_ZONE).toOffsetDateTime();
        OffsetDateTime to = end.atZone(HISTORY_ZONE).toOffsetDateTime();
        return cb.and(
                cb.greaterThanOrEqualTo(timestamp, from),
                cb.lessThan(timestamp, to)
        );
    }

    private static int number(Matcher matcher, int group) {
        return Integer.parseInt(matcher.group(group));
    }

    private static Integer optionalNumber(Matcher matcher, int group) {
        String value = matcher.group(group);
        return value == null ? null : Integer.valueOf(value);
    }

    private static void validateDate(int year, int month, int day) {
        try {
            LocalDate.of(year, month, day);
        } catch (DateTimeException exception) {
            throw invalidTimeQuery();
        }
    }

    private static void validateMonth(int year, int month) {
        try {
            YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw invalidTimeQuery();
        }
    }

    private static void validateYear(int year) {
        try {
            Year.of(year);
        } catch (DateTimeException exception) {
            throw invalidTimeQuery();
        }
    }

    private static void validateTime(Integer hour, Integer minute, Integer second) {
        if (hour == null) return;
        try {
            LocalTime.of(hour, minute == null ? 0 : minute, second == null ? 0 : second);
        } catch (DateTimeException exception) {
            throw invalidTimeQuery();
        }
    }

    private static BusinessException invalidTimeQuery() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TIME_QUERY",
                "Thời gian không đúng định dạng. Dùng YYYY-MM-DD HH:mm:ss và có thể bỏ dần phần cuối đến tháng (YYYY-MM) hoặc năm (YYYY)");
    }
}
