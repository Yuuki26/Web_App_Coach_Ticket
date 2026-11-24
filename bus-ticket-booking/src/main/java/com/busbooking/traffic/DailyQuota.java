package com.busbooking.traffic;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Counts billed requests per calendar day (Vietnam time) so a busy page can't run up a Google bill.
 * The count is kept in memory, so it starts again from zero when the server restarts.
 */
public class DailyQuota {

    private final int limit;
    private final Clock clock;
    private LocalDate day;
    private int used;

    /** @param limit requests allowed per day; negative = unlimited */
    public DailyQuota(int limit, Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    /** Takes one request from today's allowance; false when none is left. */
    public synchronized boolean tryUse() {
        rollOver();
        if (limit >= 0 && used >= limit) return false;
        used++;
        return true;
    }

    public synchronized int usedToday() {
        rollOver();
        return used;
    }

    public int limit() {
        return limit;
    }

    private void rollOver() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
    }
}
