package minecrafthdl.synthesis.prefab.primitives;

public final class EdgeDetector {

    private boolean previous;

    public boolean rising(boolean signal) {
        boolean rise = !this.previous && signal;
        this.previous = signal;
        return rise;
    }

    public void reset() {
        this.previous = false;
    }
}
