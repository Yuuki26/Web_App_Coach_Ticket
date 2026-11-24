package com.busbooking.traffic;

import java.util.List;

/**
 * Google's encoded polyline format (5 decimal places), which most map libraries can decode. Used so the
 * road path looks the same to the frontend whichever traffic provider produced it.
 */
public final class Polyline {

    private Polyline() {}

    /** @param points {latitude, longitude} pairs */
    public static String encode(List<double[]> points) {
        StringBuilder out = new StringBuilder();
        long lastLat = 0, lastLng = 0;
        for (double[] p : points) {
            long lat = Math.round(p[0] * 1e5);
            long lng = Math.round(p[1] * 1e5);
            write(lat - lastLat, out);
            write(lng - lastLng, out);
            lastLat = lat;
            lastLng = lng;
        }
        return out.toString();
    }

    private static void write(long delta, StringBuilder out) {
        long v = delta < 0 ? ~(delta << 1) : delta << 1;
        while (v >= 0x20) {
            out.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        out.append((char) (v + 63));
    }
}
