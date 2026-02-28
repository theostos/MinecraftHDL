package minecrafthdl.synthesis;

import GraphBuilder.GraphBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-macro-ic", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }
}
