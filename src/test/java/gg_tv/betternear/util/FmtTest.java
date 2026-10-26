package gg_tv.betternear.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FmtTest {

    @Test
    void distanceWholeNumbers() {
        assertEquals("150", Fmt.distance(150.0));
        assertEquals("12", Fmt.distance(12.0));
        assertEquals("0", Fmt.distance(0.0));
    }

    @Test
    void distanceFractions() {
        assertEquals("12.3", Fmt.distance(12.34));
        assertEquals("0.5", Fmt.distance(0.5));
    }

    @Test
    void pingColorThresholds() {
        assertEquals(Fmt.GRAY, Fmt.pingColor(-1));
        assertEquals(Fmt.GREEN, Fmt.pingColor(0));
        assertEquals(Fmt.GREEN, Fmt.pingColor(80));
        assertEquals(Fmt.YELLOW, Fmt.pingColor(81));
        assertEquals(Fmt.YELLOW, Fmt.pingColor(160));
        assertEquals(Fmt.ORANGE, Fmt.pingColor(161));
        assertEquals(Fmt.ORANGE, Fmt.pingColor(300));
        assertEquals(Fmt.RED, Fmt.pingColor(301));
    }

    @Test
    void pingText() {
        assertEquals("?", Fmt.ping(-1));
        assertEquals("42 ms", Fmt.ping(42));
    }
}
