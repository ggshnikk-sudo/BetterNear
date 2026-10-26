package gg_tv.betternear.util;

import java.util.Locale;

/** Форматирование значений и цвета — без зависимостей от MC, покрыто тестами. */
public final class Fmt {

    public static final int GREEN = 0x55FF55;
    public static final int YELLOW = 0xFFFF55;
    public static final int ORANGE = 0xFFAA00;
    public static final int RED = 0xFF5555;
    public static final int WHITE = 0xFFFFFF;
    public static final int GRAY = 0xAAAAAA;
    public static final int DARK_GRAY = 0x555555;

    private Fmt() {
    }

    /** 150.0 -> "150", 150.5 -> "150.5". */
    public static String distance(double d) {
        return Math.floor(d) == d ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
    }

    /** Цвет пинга по порогам. */
    public static int pingColor(int ms) {
        if (ms < 0) {
            return GRAY;
        }
        if (ms <= 80) {
            return GREEN;
        }
        if (ms <= 160) {
            return YELLOW;
        }
        if (ms <= 300) {
            return ORANGE;
        }
        return RED;
    }

    public static String ping(int ms) {
        return ms < 0 ? "?" : ms + " ms";
    }
}
