package minecrafthdl.synthesis;

import GraphBuilder.GraphBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroSubsetIntermediateCircuitTest {

    @AfterEach
    void resetCircuitTestFlag() {
        Circuit.TEST = false;
    }

    @Test
    void timerMacroNetlistBuildsAndRoutes() throws IOException {
        String json = """
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
                          "port_directions": {
                            "clk": "input",
                            "rst": "input",
                            "trigger_pulse": "input",
                            "active": "output"
                          },
                          "connections": {
                            "clk": [2],
                            "rst": [3],
                            "trigger_pulse": [4],
                            "active": [5]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Path netlist = writeTempNetlist(json);

        Circuit.TEST = true;
        IntermediateCircuit circuit = new IntermediateCircuit();
        assertDoesNotThrow(() -> {
            circuit.loadGraph(GraphBuilder.buildGraph(netlist.toString()));
            circuit.buildGates();
            circuit.routeChannels();
        });
    }

    @Test
    void ioSignsKeepPortLabelsInGeneratedCircuit() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "a_pressed": {"direction": "input", "bits": [4]},
                        "b_pressed": {"direction": "input", "bits": [5]},
                        "c_pressed": {"direction": "input", "bits": [6]},
                        "trap": {"direction": "output", "bits": [7]},
                        "reward": {"direction": "output", "bits": [8]}
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
                            "btn_pulse": "input",
                            "clear": "input",
                            "clk": "input",
                            "correct_pulse": "output",
                            "progress": "output",
                            "rst": "input",
                            "unlocked": "output",
                            "wrong_pulse": "output"
                          },
                          "connections": {
                            "btn_pulse": [4, 5, 6],
                            "clear": ["0"],
                            "clk": [2],
                            "correct_pulse": [9],
                            "progress": [10, 11],
                            "rst": [3],
                            "unlocked": [8],
                            "wrong_pulse": [7]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Path netlist = writeTempNetlist(json);

        Circuit.TEST = true;
        IntermediateCircuit circuit = new IntermediateCircuit();
        circuit.loadGraph(GraphBuilder.buildGraph(netlist.toString()));
        circuit.buildGates();
        circuit.routeChannels();
        Circuit generated = circuit.genCircuit();

        assertTrue(generated.getSignPlacementCount() >= 2);
        assertTrue(generated.hasSignPlacementText("clk"));
        assertTrue(generated.hasSignPlacementText("trap"));
        assertTrue(generated.hasSignPlacementText("reward"));
    }

    @Test
    void ioSignsFollowTopPortDeclarationOrder() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "a_pressed": {"direction": "input", "bits": [4]},
                        "b_pressed": {"direction": "input", "bits": [5]},
                        "c_pressed": {"direction": "input", "bits": [6]},
                        "trap": {"direction": "output", "bits": [7]},
                        "reward": {"direction": "output", "bits": [8]}
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
                            "clk": "input",
                            "rst": "input",
                            "clear": "input",
                            "btn_pulse": "input",
                            "unlocked": "output",
                            "correct_pulse": "output",
                            "wrong_pulse": "output",
                            "progress": "output"
                          },
                          "connections": {
                            "clk": [2],
                            "rst": [3],
                            "clear": ["0"],
                            "btn_pulse": [4,5,6],
                            "unlocked": [8],
                            "correct_pulse": [9],
                            "wrong_pulse": [7],
                            "progress": [10,11]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Path netlist = writeTempNetlist(json);
        Circuit.TEST = true;

        IntermediateCircuit circuit = new IntermediateCircuit();
        circuit.loadGraph(GraphBuilder.buildGraph(netlist.toString()));
        circuit.buildGates();
        circuit.routeChannels();
        Circuit generated = circuit.genCircuit();

        ArrayList<Circuit.SignPlacement> placements = generated.getSignPlacementsSnapshot();
        ArrayList<String> labels = new ArrayList<String>();
        for (Circuit.SignPlacement placement : placements) {
            labels.add(placement.text);
        }

        List<String> expected = Arrays.asList("clk", "rst", "a_pressed", "b_pressed", "c_pressed", "trap", "reward");
        assertEquals(expected, labels);
    }

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-macro-ic", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }
}
