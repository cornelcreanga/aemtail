package com.creanga.jsch;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Util {

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();

    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String readableSizeLong(long value) {
        if (value == Long.MIN_VALUE) return readableSizeLong(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + readableSizeLong(-value);
        if (value < 1000) return Long.toString(value);

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    public static String removeLast(String s) {
        return s.substring(0, s.length() - 1);
    }

    public static String from(String s, char c) {
        int i = s.indexOf(c);
        if (i > -1)
            return s.substring(i);
        return s;
    }

    public static String readableSizeBytes(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String readableSizeMillis(long time) {
        return String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(time),
                TimeUnit.MILLISECONDS.toSeconds(time) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
        );
    }

    public static String[] split(String str, char separatorChar, boolean preserveAllTokens) {

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

    public static String left(String s, char separator) {
        int index = s.indexOf(separator);
        return index == -1 ? s : s.substring(0, index).trim();
    }

    public static String right(String s, char separator) {
        int index = s.indexOf(separator);
        return index == -1 ? "" : s.substring(index + 1).trim();
    }

    public static String parent(String page) {
        int i = page.lastIndexOf('/');
        return page.substring(0, i);
    }

    public static List<String> filterPages(List<String> pages) {
        List<String> filtered = new ArrayList<>();
        TreeSet<String> path = new TreeSet<>();
        String last = pages.get(0);
        filtered.add(last);
        for (int i = 1; i < pages.size(); i++) {
            String current = pages.get(i);
            String parent = parent(current);
            if (path.contains(parent))
                continue;
            path.add(parent(current));
            filtered.add(current);
        }
        return filtered;
    }

}
