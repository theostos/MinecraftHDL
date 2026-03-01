package minecrafthdl.simulation;

import GraphBuilder.GraphBuilder;
import MinecraftGraph.Graph;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphRedstoneSimulatorTest {

    @Test
    void combinationalAndGateIsSimulatedWithoutMinecraft() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(andNetlist());

        assertFalse(sim.tick(mapOf("a", false, "b", false)).get("y"));
        assertFalse(sim.tick(mapOf("a", false, "b", true)).get("y"));
        assertFalse(sim.tick(mapOf("a", true, "b", false)).get("y"));
        assertTrue(sim.tick(mapOf("a", true, "b", true)).get("y"));
    }

    @Test
    void timerMacroCountsDownOnClockEdges() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(timerNetlist());

        assertFalse(tickTimer(sim, false, false, false).get("active"));
        assertTrue(tickTimer(sim, true, false, true).get("active"));
        assertTrue(tickTimer(sim, false, false, false).get("active"));
        assertTrue(tickTimer(sim, true, false, false).get("active"));
        assertTrue(tickTimer(sim, false, false, false).get("active"));
        assertTrue(tickTimer(sim, true, false, false).get("active"));
        assertTrue(tickTimer(sim, false, false, false).get("active"));
        assertFalse(tickTimer(sim, true, false, false).get("active"));
    }

    @Test
    void periodicMacroPulsesAtConfiguredPeriod() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(periodicNetlist());

        tickPeriodic(sim, false, false, true);
        assertFalse(tickPeriodic(sim, true, false, true).get("pulse"));
        tickPeriodic(sim, false, false, true);
        assertTrue(tickPeriodic(sim, true, false, true).get("pulse"));
        tickPeriodic(sim, false, false, true);
        assertFalse(tickPeriodic(sim, true, false, true).get("pulse"));
    }

    @Test
    void latchMacroRespondsToSetAndClearPulses() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(latchNetlist());

        tickLatch(sim, false, false, false, false);
        assertTrue(tickLatch(sim, true, false, true, false).get("q"));
        tickLatch(sim, false, false, true, false);
        assertTrue(tickLatch(sim, true, false, true, false).get("q"));
        tickLatch(sim, false, false, false, false);
        assertFalse(tickLatch(sim, true, false, false, true).get("q"));
    }

    @Test
    void counterMacroIncrementsAndClears() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(counterNetlist());

        tickCounter(sim, false, false, false, false);
        Map<String, Boolean> out1 = tickCounter(sim, true, false, true, false);
        assertTrue(out1.get("c0"));
        assertFalse(out1.get("c1"));

        tickCounter(sim, false, false, false, false);
        Map<String, Boolean> out2 = tickCounter(sim, true, false, true, false);
        assertFalse(out2.get("c0"));
        assertTrue(out2.get("c1"));

        tickCounter(sim, false, false, false, false);
        Map<String, Boolean> out3 = tickCounter(sim, true, false, true, false);
        assertTrue(out3.get("c0"));
        assertTrue(out3.get("c1"));

        tickCounter(sim, false, false, false, false);
        Map<String, Boolean> out4 = tickCounter(sim, true, false, false, true);
        assertFalse(out4.get("c0"));
        assertFalse(out4.get("c1"));
    }

    @Test
    void seqLockMacroTracksProgressAndEmitsPulses() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(seqLockNetlist());

        tickSeq(sim, false, false, false, false, false);

        Map<String, Boolean> stepA = tickSeq(sim, true, false, true, false, false);
        assertFalse(stepA.get("correct"));
        assertFalse(stepA.get("wrong"));
        assertTrue(stepA.get("p0"));
        assertFalse(stepA.get("p1"));

        tickSeq(sim, false, false, false, false, false);
        Map<String, Boolean> stepB = tickSeq(sim, true, false, false, true, false);
        assertFalse(stepB.get("correct"));
        assertFalse(stepB.get("wrong"));
        assertFalse(stepB.get("p0"));
        assertTrue(stepB.get("p1"));

        tickSeq(sim, false, false, false, false, false);
        Map<String, Boolean> stepC = tickSeq(sim, true, false, false, false, true);
        assertTrue(stepC.get("correct"));
        assertTrue(stepC.get("unlocked"));
        assertFalse(stepC.get("wrong"));

        tickSeq(sim, false, false, false, false, false);
        Map<String, Boolean> clearPulses = tickSeq(sim, true, false, false, false, false);
        assertFalse(clearPulses.get("correct"));
        assertFalse(clearPulses.get("wrong"));
        assertTrue(clearPulses.get("unlocked"));

        tickSeq(sim, false, false, false, false, false);
        tickSeq(sim, true, true, false, false, false); // reset state
        tickSeq(sim, false, false, false, false, false);

        Map<String, Boolean> wrong = tickSeq(sim, true, false, false, true, false);
        assertTrue(wrong.get("wrong"));
        assertFalse(wrong.get("unlocked"));
    }

    @Test
    void stationFsmMacroHandlesArrivalAndDepartWindow() throws IOException {
        GraphRedstoneSimulator sim = simulatorFromJson(stationNetlist());

        tickStation(sim, false, false, false, false);
        Map<String, Boolean> arrival = tickStation(sim, true, false, true, false);
        assertTrue(arrival.get("occupied"));
        assertFalse(arrival.get("depart_now"));

        tickStation(sim, false, false, false, false);
        Map<String, Boolean> depart = tickStation(sim, true, false, false, true);
        assertTrue(depart.get("occupied"));
        assertTrue(depart.get("depart_now"));

        tickStation(sim, false, false, false, false);
        Map<String, Boolean> hold = tickStation(sim, true, false, false, false);
        assertTrue(hold.get("occupied"));
        assertTrue(hold.get("depart_now"));

        tickStation(sim, false, false, false, false);
        Map<String, Boolean> finished = tickStation(sim, true, false, false, false);
        assertFalse(finished.get("occupied"));
        assertFalse(finished.get("depart_now"));
    }

    private static GraphRedstoneSimulator simulatorFromJson(String json) throws IOException {
        Path netlist = writeTempNetlist(json);
        Graph graph = GraphBuilder.buildGraph(netlist.toString());
        return new GraphRedstoneSimulator(graph);
    }

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-sim", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }

    private static String andNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "a": {"direction": "input", "bits": [2]},
                        "b": {"direction": "input", "bits": [3]},
                        "y": {"direction": "output", "bits": [4]}
                      },
                      "cells": {
                        "u0": {
                          "type": "$_AND_",
                          "port_directions": {"A":"input","B":"input","Y":"output"},
                          "connections": {"A":[2],"B":[3],"Y":[4]}
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String timerNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "trig": {"direction": "input", "bits": [4]},
                        "active": {"direction": "output", "bits": [5]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_timer",
                          "parameters": {"TICKS": "00000000000000000000000000000011"},
                          "port_directions": {
                            "clk":"input","rst":"input","trigger_pulse":"input","active":"output"
                          },
                          "connections": {
                            "clk":[2],"rst":[3],"trigger_pulse":[4],"active":[5]
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String periodicNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "enable": {"direction": "input", "bits": [4]},
                        "pulse": {"direction": "output", "bits": [5]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_periodic",
                          "parameters": {"PERIOD": "00000000000000000000000000000010"},
                          "port_directions": {
                            "clk":"input","rst":"input","enable":"input","pulse":"output"
                          },
                          "connections": {
                            "clk":[2],"rst":[3],"enable":[4],"pulse":[5]
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String latchNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "set": {"direction": "input", "bits": [4]},
                        "clr": {"direction": "input", "bits": [5]},
                        "q": {"direction": "output", "bits": [6]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_latch",
                          "port_directions": {
                            "clk":"input","rst":"input","set_pulse":"input","clear_pulse":"input","q":"output"
                          },
                          "connections": {
                            "clk":[2],"rst":[3],"set_pulse":[4],"clear_pulse":[5],"q":[6]
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String counterNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "inc": {"direction": "input", "bits": [4]},
                        "clr": {"direction": "input", "bits": [5]},
                        "c0": {"direction": "output", "bits": [6]},
                        "c1": {"direction": "output", "bits": [7]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_counter",
                          "parameters": {"WIDTH": "00000000000000000000000000000010"},
                          "port_directions": {
                            "clk":"input","rst":"input","inc_pulse":"input","clear_pulse":"input","count":"output"
                          },
                          "connections": {
                            "clk":[2],"rst":[3],"inc_pulse":[4],"clear_pulse":[5],"count":[6,7]
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String seqLockNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "a": {"direction": "input", "bits": [4]},
                        "b": {"direction": "input", "bits": [5]},
                        "c": {"direction": "input", "bits": [6]},
                        "unlocked": {"direction": "output", "bits": [7]},
                        "correct": {"direction": "output", "bits": [8]},
                        "wrong": {"direction": "output", "bits": [9]},
                        "p0": {"direction": "output", "bits": [10]},
                        "p1": {"direction": "output", "bits": [11]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_seq_lock",
                          "parameters": {
                            "BTN_COUNT": "00000000000000000000000000000011",
                            "SEQ_LEN": "00000000000000000000000000000011",
                            "LATCH_SUCCESS": "00000000000000000000000000000001",
                            "EXPECT_IDX": "00000000000000000000000000100100"
                          },
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "clear":"input",
                            "btn_pulse":"input",
                            "unlocked":"output",
                            "correct_pulse":"output",
                            "wrong_pulse":"output",
                            "progress":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "clear":["0"],
                            "btn_pulse":[4,5,6],
                            "unlocked":[7],
                            "correct_pulse":[8],
                            "wrong_pulse":[9],
                            "progress":[10,11]
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static String stationNetlist() {
        return """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "arrive": {"direction": "input", "bits": [4]},
                        "depart": {"direction": "input", "bits": [5]},
                        "occupied": {"direction": "output", "bits": [6]},
                        "depart_now": {"direction": "output", "bits": [7]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_station_fsm",
                          "parameters": {"DEPART_TICKS": "00000000000000000000000000000010"},
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "clear":"input",
                            "arrival_pulse":"input",
                            "depart_pulse":"input",
                            "occupied":"output",
                            "depart_now":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "clear":["0"],
                            "arrival_pulse":[4],
                            "depart_pulse":[5],
                            "occupied":[6],
                            "depart_now":[7]
                          }
                        }
                      }
                    }
                  }
                }
                """;
    }

    private static Map<String, Boolean> tickTimer(GraphRedstoneSimulator sim, boolean clk, boolean rst, boolean trig) {
        return sim.tick(mapOf("clk", clk, "rst", rst, "trig", trig));
    }

    private static Map<String, Boolean> tickPeriodic(GraphRedstoneSimulator sim, boolean clk, boolean rst, boolean enable) {
        return sim.tick(mapOf("clk", clk, "rst", rst, "enable", enable));
    }

    private static Map<String, Boolean> tickLatch(GraphRedstoneSimulator sim, boolean clk, boolean rst, boolean set, boolean clr) {
        return sim.tick(mapOf("clk", clk, "rst", rst, "set", set, "clr", clr));
    }

    private static Map<String, Boolean> tickCounter(GraphRedstoneSimulator sim, boolean clk, boolean rst, boolean inc, boolean clr) {
        return sim.tick(mapOf("clk", clk, "rst", rst, "inc", inc, "clr", clr));
    }

    private static Map<String, Boolean> tickSeq(GraphRedstoneSimulator sim, boolean clk, boolean rst, boolean a, boolean b, boolean c) {
        return sim.tick(mapOf("clk", clk, "rst", rst, "a", a, "b", b, "c", c));
    }

    private static Map<String, Boolean> tickStation(GraphRedstoneSimulator sim, boolean clk, boolean rst, boolean arrive, boolean depart) {
        return sim.tick(mapOf("clk", clk, "rst", rst, "arrive", arrive, "depart", depart));
    }

    private static Map<String, Boolean> mapOf(Object... pairs) {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<String, Boolean>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Boolean) pairs[i + 1]);
        }
        return map;
    }
}
