package gg_tv.betternear.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterNearConfigTest {

    @Test
    void defaults() {
        BetterNearConfig c = new BetterNearConfig();
        assertEquals(150.0, c.radius);
        assertFalse(c.hideInvisible);
        assertFalse(c.hideSpectators);
        assertEquals(5.0, c.highlightSeconds);
    }

    @Test
    void gsonRoundTrip() {
        Gson gson = new GsonBuilder().create();
        BetterNearConfig c = new BetterNearConfig();
        c.radius = 250.5;
        c.hideInvisible = true;
        c.hideSpectators = true;
        c.highlightSeconds = 12.5;
        String json = gson.toJson(c);
        BetterNearConfig back = gson.fromJson(json, BetterNearConfig.class);
        assertEquals(250.5, back.radius);
        assertTrue(back.hideInvisible);
        assertTrue(back.hideSpectators);
        assertEquals(12.5, back.highlightSeconds);
    }

    @Test
    void ignoresUnknownKeys() {
        Gson gson = new GsonBuilder().create();
        BetterNearConfig back = gson.fromJson(
                "{\"radius\":42.0,\"hideInvisible\":true,\"hideSpectators\":false,"
                        + "\"showTracers\":true,\"tracerColor\":\"#FF0000\"}",
                BetterNearConfig.class);
        assertEquals(42.0, back.radius);
        assertTrue(back.hideInvisible);
        assertFalse(back.hideSpectators);
    }

    @Test
    void sanitizeClampsOutOfRange() {
        BetterNearConfig c = new BetterNearConfig();
        c.radius = 0.5;
        c.highlightSeconds = 999.0;
        ConfigManager.sanitize(c);
        assertEquals(150.0, c.radius);
        assertEquals(5.0, c.highlightSeconds);
    }

    @Test
    void sanitizeKeepsValidValues() {
        BetterNearConfig c = new BetterNearConfig();
        c.radius = 64.0;
        c.highlightSeconds = 30.0;
        ConfigManager.sanitize(c);
        assertEquals(64.0, c.radius);
        assertEquals(30.0, c.highlightSeconds);
    }

    @Test
    void sanitizeRejectsNaN() {
        BetterNearConfig c = new BetterNearConfig();
        c.radius = Double.NaN;
        c.highlightSeconds = Double.NaN;
        ConfigManager.sanitize(c);
        assertEquals(150.0, c.radius);
        assertEquals(5.0, c.highlightSeconds);
    }
}
