package minecrafthdl.synthesis.prefab.primitives;

public final class BoundedCounter {

    private final int width;
    private final int mask;
    private int value;

    public BoundedCounter(int width) {
        this.width = Math.max(1, width);
        this.mask = this.width >= 31 ? -1 : ((1 << this.width) - 1);
    }

    public int tick(boolean increment, boolean clear, boolean reset) {
        if (reset || clear) {
            this.value = 0;
        } else if (increment) {
            this.value = (this.value + 1) & this.mask;
        }
        return this.value;
    }

    public int value() {
        return this.value;
    }

    public void reset() {
        this.value = 0;
    }
}
