package minecrafthdl.synthesis.prefab.primitives;

public final class BoundedTimer {

    private int remaining;

    public boolean tick(boolean trigger, int durationTicks, boolean reset) {
        int duration = Math.max(1, durationTicks);
        if (reset) {
            this.remaining = 0;
        } else if (trigger) {
            this.remaining = duration;
        } else if (this.remaining > 0) {
            this.remaining--;
        }
        return this.remaining > 0;
    }

    public int remaining() {
        return this.remaining;
    }

    public void reset() {
        this.remaining = 0;
    }
}
