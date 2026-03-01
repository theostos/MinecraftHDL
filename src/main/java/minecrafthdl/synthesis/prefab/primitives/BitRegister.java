package minecrafthdl.synthesis.prefab.primitives;

public final class BitRegister {

    private boolean q;

    public boolean tick(boolean load, boolean data, boolean reset) {
        if (reset) {
            this.q = false;
        } else if (load) {
            this.q = data;
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
