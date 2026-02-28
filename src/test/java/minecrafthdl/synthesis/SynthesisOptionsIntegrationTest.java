package minecrafthdl.synthesis;

import GraphBuilder.GraphBuilder;
import minecrafthdl.MHDLException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SynthesisOptionsIntegrationTest {

    @Test
    void defaultsExposeExpectedContractValues() {
        SynthesisOptions defaults = SynthesisOptions.defaults();
        assertEquals(false, defaults.prefabMacrosEnabled());
        assertEquals(2, defaults.prefabAutoClockPeriodTicks());
        assertEquals(10000, defaults.prefabMacroTotalBlockBudget());
        assertEquals(2000, defaults.prefabMacroPerInstanceBlockBudget());
    }

    @Test
    void prefabBudgetCanFailSynthesisEarly() throws IOException {
        Circuit.TEST = true;
        try {
            IntermediateCircuit circuit = new IntermediateCircuit(new SynthesisOptions(true, 2, 10, 5));
            circuit.loadGraph(GraphBuilder.buildGraph(writeTempNetlist(timerNetlist()).toString()));
            circuit.buildGates();
            circuit.routeChannels();
            assertThrows(MHDLException.class, circuit::genCircuit);
        } finally {
            Circuit.TEST = false;
        }
    }

    @Test
    void prefabModePassAUsesPrefabGatePathWithoutRuntimeMacroPlacements() throws IOException {
        assertPrefabModeHasNoRuntimePlacements(timerNetlist());
        assertPrefabModeHasNoRuntimePlacements(periodicNetlist());
        assertPrefabModeHasNoRuntimePlacements(latchNetlist());
    }

    @Test
    void prefabModePassBUsesPrefabGatePathWithoutRuntimeMacroPlacements() throws IOException {
        assertPrefabModeHasNoRuntimePlacements(counterNetlist());
        assertPrefabModeHasNoRuntimePlacements(seqLockNetlist());
        assertPrefabModeHasNoRuntimePlacements(stationNetlist());
    }

    private static void assertPrefabModeHasNoRuntimePlacements(String json) throws IOException {
        Circuit.TEST = true;
        try {
            IntermediateCircuit circuit = new IntermediateCircuit(new SynthesisOptions(true, 2, 10000, 2000));
            circuit.loadGraph(GraphBuilder.buildGraph(writeTempNetlist(json).toString()));
            circuit.buildGates();
            circuit.routeChannels();
            Circuit generated = circuit.genCircuit();
            assertEquals(0, generated.getMacroPlacementCount());
        } finally {
            Circuit.TEST = false;
        }
    }

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-synth-options", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
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
                        "trigger": {"direction": "input", "bits": [4]},
                        "active": {"direction": "output", "bits": [5]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_timer",
                          "parameters": {"TICKS": "00000000000000000000000000000100"},
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "trigger_pulse":"input",
                            "active":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "trigger_pulse":[4],
                            "active":[5]
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
                          "parameters": {"PERIOD": "00000000000000000000000000000101"},
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "enable":"input",
                            "pulse":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "enable":[4],
                            "pulse":[5]
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
                        "clear": {"direction": "input", "bits": [5]},
                        "q": {"direction": "output", "bits": [6]}
                      },
                      "cells": {
                        "u0": {
                          "type": "mc_latch",
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "set_pulse":"input",
                            "clear_pulse":"input",
                            "q":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "set_pulse":[4],
                            "clear_pulse":[5],
                            "q":[6]
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
}
