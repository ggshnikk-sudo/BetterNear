package gg_tv.betternear;

import gg_tv.betternear.config.BetterNearConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearLogicTest {

    private static final double R = 150.0;

    @Test
    void inRadiusIsHighlighted() {
        assertTrue(NearLogic.shouldHighlight(149.0 * 149.0, R, false, false, new BetterNearConfig()));
    }

    @Test
    void boundaryRadiusIsHighlighted() {
        assertTrue(NearLogic.shouldHighlight(R * R, R, false, false, new BetterNearConfig()));
    }

    @Test
    void outsideRadiusIsNotHighlighted() {
        assertFalse(NearLogic.shouldHighlight((R + 0.01) * (R + 0.01), R, false, false, new BetterNearConfig()));
    }

    @Test
    void nullConfigStillFiltersRadius() {
        assertFalse(NearLogic.shouldHighlight((R + 5) * (R + 5), R, true, true, null));
        assertTrue(NearLogic.shouldHighlight(100.0, R, true, true, null));
    }

    @Test
    void hideInvisibleFiltersInvisible() {
        BetterNearConfig cfg = new BetterNearConfig();
        cfg.hideInvisible = true;
        assertFalse(NearLogic.shouldHighlight(50.0, R, true, false, cfg));
        assertTrue(NearLogic.shouldHighlight(50.0, R, false, false, cfg));
    }

    @Test
    void hideSpectatorsFiltersSpectators() {
        BetterNearConfig cfg = new BetterNearConfig();
        cfg.hideSpectators = true;
        assertFalse(NearLogic.shouldHighlight(50.0, R, false, true, cfg));
        assertTrue(NearLogic.shouldHighlight(50.0, R, false, false, cfg));
    }

    @Test
    void filtersOffByDefault() {
        assertTrue(NearLogic.shouldHighlight(50.0, R, true, true, new BetterNearConfig()));
    }

    @Test
    void distanceBlocksRounds() {
        assertEquals(12, NearLogic.distanceBlocks(12.4));
        assertEquals(13, NearLogic.distanceBlocks(12.5));
        assertEquals(0, NearLogic.distanceBlocks(0.1));
    }
}