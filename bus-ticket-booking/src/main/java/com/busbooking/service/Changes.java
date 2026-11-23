package com.busbooking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Collects "field: old → new" lines for the audit log while applying an update. */
public final class Changes {

    private final List<String> lines = new ArrayList<>();

    /** Records the change if the value differs, and returns the new value for convenience. */
    public <T> T track(String field, Object oldValue, T newValue) {
        if (!sameValue(oldValue, newValue)) lines.add(field + ": " + oldValue + " → " + newValue);
        return newValue;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Override
    public String toString() {
        return lines.isEmpty() ? "no changes" : String.join("; ", lines);
    }

    private static boolean sameValue(Object a, Object b) {
        if (a instanceof java.math.BigDecimal x && b instanceof java.math.BigDecimal y) return x.compareTo(y) == 0;
        return Objects.equals(a, b);
    }
}
