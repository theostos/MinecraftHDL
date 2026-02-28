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
}
