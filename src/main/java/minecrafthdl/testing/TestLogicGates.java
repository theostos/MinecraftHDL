package minecrafthdl.testing;

import minecrafthdl.Demo;
import minecrafthdl.MHDLException;
import minecrafthdl.synthesis.Gate;
import minecrafthdl.synthesis.IntermediateCircuit;
import minecrafthdl.synthesis.LogicGates;

/**
 * Created by Francis O'Brien - 3/4/2017 - 2:35 AM
 */

public class TestLogicGates extends LogicGates {



    public static void main(String[] args) {
        IntermediateCircuit ic = new IntermediateCircuit();
        ic.loadGraph(Demo.create4bitmuxgraph());
        ic.printLayers();
    }

    private static Gate unitGate(int numInputs, int numOutputs) {
        int inputWidth = numInputs <= 1 ? 1 : (numInputs * 2) - 1;
        int outputWidth = numOutputs <= 1 ? 1 : (numOutputs * 2) - 1;
        int width = Math.max(1, Math.max(inputWidth, outputWidth));

        int inputSpacing = numInputs <= 1 ? 0 : 1;
        int outputSpacing = numOutputs <= 1 ? 0 : 1;

        TestGate gate = new TestGate(width, 1, 1, numInputs, numOutputs, inputSpacing, outputSpacing, new int[]{0});
        gate.setBlock(0, 0, 0, "x");
        return gate;
    }

    public static Gate IO(){
        return unitGate(1, 1);
    }

    public static Gate NOT(){
        TestGate gate = new TestGate(1, 1, 5, 1, 1, 0, 0, new int[]{0});
        gate.setBlock(0, 0, 0, "x");
        gate.setBlock(0, 0, 1, "i");
        gate.setBlock(0, 0, 2, "*");
        gate.setBlock(0, 0, 3, "*");
        gate.setBlock(0, 0, 4, "x");
        return gate;
    }

    public static Gate RELAY(){
        TestGate gate = new TestGate(1, 1, 5, 1, 1, 0, 0, new int[]{0});
        gate.setBlock(0, 0, 0, "x");
        gate.setBlock(0, 0, 1, "*");
        gate.setBlock(0, 0, 2, ">");
        gate.setBlock(0, 0, 3, "*");
        gate.setBlock(0, 0, 4, "x");
        return gate;
    }

    public static Gate AND(int inputs) {
        if (inputs == 0) throw new MHDLException("Gate cannot have 0 inputs");
        int width;
        if (inputs == 1) width = 1;

        else width = (inputs * 2) - 1;

        TestGate gate = new TestGate(width, 2, 5, inputs, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 2, "i");
        gate.setBlock(0, 0, 3, "*");
        gate.setBlock(0, 0, 4, "x");

        for (int i = 0; i < width; i+=2) {
            gate.setBlock(i, 0, 0, "x");
            gate.setBlock(i, 0, 1, "x");
            gate.setBlock(i, 1, 0, "i");
            gate.setBlock(i, 1, 1, "*");

            if (i != width - 1) {
                gate.setBlock(i + 1, 0, 1, "x");
                if (i == 14) {
                    gate.setBlock(i + 1, 1, 1, ">");
                } else {
                    gate.setBlock(i + 1, 1, 1, "*");
                }
            }
        }

        return gate;
    }



    public static Gate OR(int inputs) {
        if (inputs == 0) throw new MHDLException("Gate cannot have 0 inputs");
        int width;
        if (inputs == 1) width = 1;
        else width = (inputs * 2) - 1;

        TestGate gate = new TestGate(width, 2, 5, inputs, 1, 1, 0, new int[]{0});

        gate.setBlock(0, 0, 3, "*");
        gate.setBlock(0, 0, 4, "x");

        for (int i = 0; i < width; i+=2) {
            gate.setBlock(i, 0, 0, "x");
            gate.setBlock(i, 0, 1, ">");
            gate.setBlock(i, 0, 2, "*");
            if (i != width - 1) {
                if (i == 14) {
                    gate.setBlock(i + 1, 0, 2, ">");
                } else {
                    gate.setBlock(i + 1, 0, 2, "*");
                }
            }
        }
        return gate;
    }

    public static Gate XOR() {
        return unitGate(2, 1);
    }

    public static Gate MUX() {
        return unitGate(3, 1);
    }

    public static Gate HIGH() {
        return unitGate(1, 1);
    }

    public static Gate LOW() {
        return unitGate(1, 1);
    }

    public static Gate D_LATCH() {
        return unitGate(2, 1);
    }

    public static Gate MC_TIMER() {
        return unitGate(3, 1);
    }

    public static Gate MC_PERIODIC() {
        return unitGate(3, 1);
    }

    public static Gate MC_LATCH() {
        return unitGate(4, 1);
    }

    public static Gate MC_COUNTER() {
        return unitGate(4, 1);
    }

    public static Gate MC_SEQ_LOCK(int btnCount) {
        return unitGate(3 + Math.max(1, btnCount), 1);
    }

    public static Gate MC_STATION_FSM() {
        return unitGate(5, 1);
    }

}
