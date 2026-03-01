package minecrafthdl.synthesis.prefab.primitives;

public final class PulseStretcher {

    private final int widthTicks;
    private int remainingTicks;

    public PulseStretcher(int widthTicks) {
        this.widthTicks = Math.max(1, widthTicks);
    }

    public boolean tick(boolean trigger) {
        if (trigger) {
            this.remainingTicks = this.widthTicks;
        }
        boolean active = this.remainingTicks > 0;
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
        }
        return active;
    }

    public void reset() {
        this.remainingTicks = 0;
    }
}
