package com.busbooking.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The seat plan of a coach: {@code rows} rows of A B | C D (rows come from the coach).
 * Rows 1–2 have extra legroom and cost more.
 */
public final class SeatLayout {

    public static final List<String> COLUMNS = List.of("A", "B", "C", "D");
    public static final int LEGROOM_ROWS = 2;
    /** Extra charge per legroom seat, in VND. */
    public static final BigDecimal LEGROOM_SURCHARGE = new BigDecimal("30000");
    public static final int MIN_ROWS = 3;
    public static final int MAX_ROWS = 12;

    /** Sorts "2B" before "10A": by row number, then by letter. */
    public static final Comparator<String> ORDER =
            Comparator.comparingInt(SeatLayout::rowOf).thenComparing(s -> s.substring(s.length() - 1));

    private SeatLayout() {}

    public static boolean isValid(String label, int rows) {
        if (label == null || !label.matches("[1-9][0-9]?[A-Z]")) return false;
        return rowOf(label) <= rows && COLUMNS.contains(label.substring(label.length() - 1));
    }

    public static int rowOf(String label) {
        return Integer.parseInt(label.substring(0, label.length() - 1));
    }

    public static boolean hasExtraLegroom(String label) {
        return rowOf(label) <= LEGROOM_ROWS;
    }

    public static List<String> allSeats(int rows) {
        List<String> all = new ArrayList<>(rows * COLUMNS.size());
        for (int row = 1; row <= rows; row++) {
            for (String col : COLUMNS) all.add(row + col);
        }
        return all;
    }
}
