package com.mogrul.videocompressor.util;

import java.util.Locale;

public class SizeFormatUtil {
    private static final String[] UNITS = {"B", "KiB", "MiB", "GiB", "TiB"};

    private SizeFormatUtil() {}

    public static String humanBytes(long bytes) {
        if (bytes < 0) return "-" + humanBytes(-bytes);
        if (bytes < 1024) return bytes + " B";

        double v = bytes;
        int unit = 0;
        while (v >= 1024.0 && unit < UNITS.length - 1) {
            v /= 1024.0;
            unit++;
        }

        // 0 decimals for B, 1 decimal for < 10, else 2 for nicer readability
        int decimals = (unit == 0) ? 0 : (v < 10 ? 2 : 1);
        return String.format(Locale.ROOT, "%." + decimals + "f %s", v, UNITS[unit]);
    }
}
