package minecrafthdl.synthesis.prefab;

import minecrafthdl.synthesis.prefab.primitives.BitRegister;
import minecrafthdl.synthesis.prefab.primitives.BoundedCounter;
import minecrafthdl.synthesis.prefab.primitives.BoundedTimer;
import minecrafthdl.synthesis.prefab.primitives.EdgeDetector;
import minecrafthdl.synthesis.prefab.primitives.PulseStretcher;
import minecrafthdl.synthesis.prefab.primitives.SRLatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrefabPrimitivesTest {

    @Test
    void edgeDetectorDetectsSingleRisingEdges() {
        EdgeDetector detector = new EdgeDetector();
        assertFalse(detector.rising(false));
        assertTrue(detector.rising(true));
        assertFalse(detector.rising(true));
        assertFalse(detector.rising(false));
        assertTrue(detector.rising(true));
    }

    @Test
    void pulseStretcherHoldsForConfiguredWidth() {
        PulseStretcher stretcher = new PulseStretcher(2);
        assertTrue(stretcher.tick(true));
        assertTrue(stretcher.tick(false));
        assertFalse(stretcher.tick(false));
    }

    @Test
    void srLatchTracksSetClearResetPriority() {
        SRLatch latch = new SRLatch();
        assertFalse(latch.tick(false, false, false));
        assertTrue(latch.tick(true, false, false));
        assertTrue(latch.tick(false, false, false));
        assertFalse(latch.tick(false, true, false));
        assertFalse(latch.tick(true, false, true));
    }

    @Test
    void bitRegisterLoadsAndResets() {
        BitRegister register = new BitRegister();
        assertFalse(register.tick(false, true, false));
        assertTrue(register.tick(true, true, false));
        assertTrue(register.tick(false, false, false));
        assertFalse(register.tick(false, false, true));
    }

    @Test
    void boundedCounterWrapsByWidth() {
        BoundedCounter counter = new BoundedCounter(2);
        assertEquals(1, counter.tick(true, false, false));
        assertEquals(2, counter.tick(true, false, false));
        assertEquals(3, counter.tick(true, false, false));
        assertEquals(0, counter.tick(true, false, false));
        assertEquals(0, counter.tick(false, true, false));
    }

    @Test
    void boundedTimerLoadsAndCountsDown() {
        BoundedTimer timer = new BoundedTimer();
        assertTrue(timer.tick(true, 3, false));
        assertTrue(timer.tick(false, 3, false));
        assertTrue(timer.tick(false, 3, false));
        assertFalse(timer.tick(false, 3, false));
        assertFalse(timer.tick(false, 3, true));
    }
}
