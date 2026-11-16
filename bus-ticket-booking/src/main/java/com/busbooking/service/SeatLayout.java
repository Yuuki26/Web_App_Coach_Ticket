package com.busbooking.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The coach seat plan: 9 rows of A B | C D. Rows 1-2 have extra legroom and cost more.
 * Every coach uses the same plan for now; move this onto a Coach entity if layouts ever differ.
 */
public final class SeatLayout {

    public static final int ROWS = 9;
    public static final List<String> COLUMNS = List.of("A", "B", "C", "D");
    public static final int LEGROOM_ROWS = 2;
    public static final BigDecimal LEGROOM_SURCHARGE = new BigDecimal("5.00");
    public static final int TOTAL_SEATS = ROWS * COLUMNS.size();

    /** Sorts "2B" before "10A": by row number, then by letter. */
    public static final Comparator<String> ORDER =
            Comparator.comparingInt(SeatLayout::rowOf).thenComparing(s -> s.substring(s.length() - 1));

    private SeatLayout() {}

    public static boolean isValid(String label) {
        if (label == null || !label.matches("[1-9][0-9]?[A-Z]")) return false;
        return rowOf(label) <= ROWS && COLUMNS.contains(label.substring(label.length() - 1));
    }

    public static int rowOf(String label) {
        return Integer.parseInt(label.substring(0, label.length() - 1));
    }

    public static boolean hasExtraLegroom(String label) {
        return rowOf(label) <= LEGROOM_ROWS;
    }

    public static List<String> allSeats() {
        List<String> all = new ArrayList<>(TOTAL_SEATS);
        for (int row = 1; row <= ROWS; row++) {
            for (String col : COLUMNS) all.add(row + col);
        }
        return all;
    }
}
