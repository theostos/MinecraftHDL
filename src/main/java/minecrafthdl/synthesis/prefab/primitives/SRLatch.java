package minecrafthdl.synthesis.prefab.primitives;

public final class SRLatch {

    private boolean q;

    public boolean tick(boolean set, boolean clear, boolean reset) {
        if (reset || clear) {
            this.q = false;
        } else if (set) {
            this.q = true;
        }
        return this.q;
    }

    public boolean value() {
        return this.q;
    }

    public void reset() {
        this.q = false;
    }
}
