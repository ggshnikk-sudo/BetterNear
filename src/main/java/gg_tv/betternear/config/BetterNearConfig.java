package gg_tv.betternear.config;

/**
 * Настройки мода. Файл: config/betternear.json.
 * Радиус по умолчанию — 150 блоков.
 */
public class BetterNearConfig {

    /** Радиус подсветки в блоках. */
    public double radius = 150.0;

    /** Не подсвечивать невидимых игроков. */
    public boolean hideInvisible = false;

    /** Не подсвечивать игроков в спектаторе. */
    public boolean hideSpectators = false;

    /** Сколько секунд держится разовая подсветка после /bn. */
    public double highlightSeconds = 5.0;
}