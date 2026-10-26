package gg_tv.betternear;

import gg_tv.betternear.config.BetterNearConfig;

/**
 * Чистая логика фильтрации целей для подсветки. Без зависимостей от Minecraft,
 * чтобы покрыть юнит-тестами: радиус + фильтры (invisible/spectator).
 */
public final class NearLogic {

    private NearLogic() {
    }

    /**
     * Нужно ли подсвечивать игрока.
     *
     * @param distanceSqr квадрат дистанции до игрока
     * @param radius      радиус подсветки (блоки)
     * @param invisible   цель невидима
     * @param spectator   цель в спектаторе
     * @param cfg         конфиг (может быть null — тогда фильтры выключены)
     */
    public static boolean shouldHighlight(double distanceSqr, double radius, boolean invisible,
                                          boolean spectator, BetterNearConfig cfg) {
        if (distanceSqr > radius * radius) {
            return false;
        }
        if (cfg == null) {
            return true;
        }
        if (cfg.hideInvisible && invisible) {
            return false;
        }
        if (cfg.hideSpectators && spectator) {
            return false;
        }
        return true;
    }

    /** Округление дистанции до целых блоков для сообщений. */
    public static long distanceBlocks(double distance) {
        return Math.round(distance);
    }
}