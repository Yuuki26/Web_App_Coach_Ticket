package com.busbooking.repository;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small builders for search filters. Each returns a "match everything" spec when its value is
 * null or blank, so filters can simply be and-ed together and unused ones drop out.
 * All values go into the query as parameters, never as SQL text.
 */
public final class Specs {

    private Specs() {}

    public static <T> Specification<T> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    /** Case-insensitive "contains" across several fields (e.g. q=da lat matches origin or destination). */
    public static <T> Specification<T> containsAny(String text, String... paths) {
        if (text == null || text.isBlank()) return all();
        String pattern = "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            for (String p : paths) any.add(cb.like(cb.lower(path(root, p).as(String.class)), pattern, '\\'));
            return cb.or(any.toArray(Predicate[]::new));
        };
    }

    public static <T> Specification<T> equalTo(String path, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) return all();
        return (root, query, cb) -> cb.equal(path(root, path), value);
    }

    public static <T> Specification<T> equalsIgnoreCase(String path, String value) {
        if (value == null || value.isBlank()) return all();
        return (root, query, cb) -> cb.equal(cb.lower(path(root, path).as(String.class)),
                value.trim().toLowerCase(Locale.ROOT));
    }

    public static <T, V extends Comparable<? super V>> Specification<T> atLeast(String path, V min) {
        if (min == null) return all();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(path(root, path), min);
    }

    public static <T, V extends Comparable<? super V>> Specification<T> atMost(String path, V max) {
        if (max == null) return all();
        return (root, query, cb) -> cb.lessThanOrEqualTo(path(root, path), max);
    }

    /** A date-time column within whole days: from the start of {@code from} to the end of {@code to}. */
    public static <T> Specification<T> onOrAfterDay(String path, LocalDate from) {
        return from == null ? all() : atLeast(path, from.atStartOfDay());
    }

    public static <T> Specification<T> onOrBeforeDay(String path, LocalDate to) {
        if (to == null) return all();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return (root, query, cb) -> cb.lessThan(path(root, path), end);
    }

    /** Follows dotted paths such as "coach.operator.id". */
    @SuppressWarnings("unchecked")
    static <Y> Path<Y> path(Root<?> root, String dotted) {
        Path<?> p = root;
        for (String part : dotted.split("\\.")) p = p.get(part);
        return (Path<Y>) p;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
