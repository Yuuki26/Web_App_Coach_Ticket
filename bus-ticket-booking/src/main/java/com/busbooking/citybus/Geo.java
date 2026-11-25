package com.busbooking.citybus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Small geometry helpers for bus paths. Points are {lat, lng} in degrees. */
public final class Geo {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private Geo() {}

    /** Great-circle distance in metres. */
    public static double meters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public static double meters(double[] a, double[] b) {
        return meters(a[0], a[1], b[0], b[1]);
    }

    public static double length(List<double[]> path) {
        double total = 0;
        for (int i = 1; i < path.size(); i++) total += meters(path.get(i - 1), path.get(i));
        return total;
    }

    /** Road pieces whose ends are this close are taken as connected. */
    static final double JOIN_METERS = 30;

    /**
     * Joins the pieces of a route (OpenStreetMap ways, each drawn in whichever direction it was mapped)
     * into one path, turning pieces round as needed. OSM usually lists pieces in travel order but not
     * always, so at each step: the next listed piece if it connects, else any unused piece that connects
     * at the current end, else (a real gap in the data) the next listed piece.
     */
    public static List<double[]> stitch(List<List<double[]>> pieces) {
        List<List<double[]>> todo = new ArrayList<>();
        for (List<double[]> p : pieces) if (!p.isEmpty()) todo.add(p);
        List<double[]> path = new ArrayList<>();
        if (todo.isEmpty()) return path;

        // First piece: turned so that its end connects to another piece.
        List<double[]> first = todo.remove(0);
        if (!todo.isEmpty() && nearestEndOfAny(first.get(0), todo) < nearestEndOfAny(first.get(first.size() - 1), todo)) {
            first = reversed(first);
        }
        path.addAll(first);

        while (!todo.isEmpty()) {
            double[] end = path.get(path.size() - 1);
            int pick = -1;
            if (nearestEnd(end, todo.get(0)) <= JOIN_METERS) {
                pick = 0;
            } else {
                double best = JOIN_METERS;
                for (int i = 1; i < todo.size(); i++) {
                    double d = nearestEnd(end, todo.get(i));
                    if (d <= best) {
                        best = d;
                        pick = i;
                    }
                }
                if (pick < 0) pick = 0; // a gap in the data: jump to the next listed piece
            }
            List<double[]> piece = todo.remove(pick);
            boolean reverse = meters(end, piece.get(piece.size() - 1)) < meters(end, piece.get(0));
            List<double[]> oriented = reverse ? reversed(piece) : piece;
            int from = meters(end, oriented.get(0)) < 1 ? 1 : 0;
            path.addAll(oriented.subList(from, oriented.size()));
        }
        return path;
    }

    private static double nearestEndOfAny(double[] p, List<List<double[]>> pieces) {
        double best = Double.MAX_VALUE;
        for (List<double[]> piece : pieces) best = Math.min(best, nearestEnd(p, piece));
        return best;
    }

    private static double nearestEnd(double[] p, List<double[]> piece) {
        return Math.min(meters(p, piece.get(0)), meters(p, piece.get(piece.size() - 1)));
    }

    private static List<double[]> reversed(List<double[]> piece) {
        List<double[]> r = new ArrayList<>(piece);
        java.util.Collections.reverse(r);
        return r;
    }

    /**
     * How far along {@code path} (in metres from its start) the point nearest to {@code p} is.
     * Used to put stops in travel order.
     */
    public static double alongPath(List<double[]> path, double[] p) {
        double lat0 = Math.toRadians(p[0]);
        double kx = Math.toRadians(1) * Math.cos(lat0) * EARTH_RADIUS_M, ky = Math.toRadians(1) * EARTH_RADIUS_M;
        double px = p[1] * kx, py = p[0] * ky;
        double best = Double.MAX_VALUE, bestAlong = 0, walked = 0;
        for (int i = 1; i < path.size(); i++) {
            double ax = path.get(i - 1)[1] * kx, ay = path.get(i - 1)[0] * ky;
            double bx = path.get(i)[1] * kx, by = path.get(i)[0] * ky;
            double dx = bx - ax, dy = by - ay, len = Math.hypot(dx, dy);
            double t = len == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (len * len)));
            double d = Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
            if (d < best) {
                best = d;
                bestAlong = walked + t * len;
            }
            walked += len;
        }
        return bestAlong;
    }

    /** The part of {@code path} between {@code fromM} and {@code toM} metres from its start. */
    public static List<double[]> slice(List<double[]> path, double fromM, double toM) {
        List<double[]> out = new ArrayList<>();
        double walked = 0;
        for (int i = 1; i < path.size(); i++) {
            double[] a = path.get(i - 1), b = path.get(i);
            double len = meters(a, b);
            double segStart = walked, segEnd = walked + len;
            if (segEnd >= fromM && segStart <= toM) {
                if (out.isEmpty()) out.add(lerp(a, b, len == 0 ? 0 : (Math.max(fromM, segStart) - segStart) / len));
                if (segEnd <= toM) {
                    out.add(b);
                } else {
                    out.add(lerp(a, b, len == 0 ? 0 : (toM - segStart) / len));
                    break;
                }
            }
            walked = segEnd;
        }
        return out.size() >= 2 ? out : path;
    }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    /** Douglas–Peucker simplification: drops points closer than {@code toleranceM} to the line. */
    public static List<double[]> simplify(List<double[]> path, double toleranceM) {
        int n = path.size();
        if (n < 3) return new ArrayList<>(path);
        // Flat projection in metres around the path's first point (fine at city scale).
        double lat0 = Math.toRadians(path.get(0)[0]);
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = Math.toRadians(path.get(i)[1]) * Math.cos(lat0) * EARTH_RADIUS_M;
            ys[i] = Math.toRadians(path.get(i)[0]) * EARTH_RADIUS_M;
        }
        boolean[] keep = new boolean[n];
        keep[0] = keep[n - 1] = true;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, n - 1});
        while (!stack.isEmpty()) {
            int[] seg = stack.pop();
            int a = seg[0], b = seg[1];
            double maxDist = -1;
            int index = -1;
            for (int i = a + 1; i < b; i++) {
                double d = distanceToSegment(xs[i], ys[i], xs[a], ys[a], xs[b], ys[b]);
                if (d > maxDist) {
                    maxDist = d;
                    index = i;
                }
            }
            if (index >= 0 && maxDist > toleranceM) {
                keep[index] = true;
                stack.push(new int[]{a, index});
                stack.push(new int[]{index, b});
            }
        }
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < n; i++) if (keep[i]) out.add(path.get(i));
        return out;
    }

    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / len2));
        double cx = ax + t * dx, cy = ay + t * dy;
        return Math.hypot(px - cx, py - cy);
    }
}
