package minecrafthdl.synthesis.routing.pins;

import GraphBuilder.GraphBuilder;
import MinecraftGraph.Function;
import MinecraftGraph.FunctionType;
import MinecraftGraph.Graph;
import MinecraftGraph.MacroVertex;
import MinecraftGraph.Vertex;
import minecrafthdl.synthesis.Circuit;
import minecrafthdl.testing.TestLogicGates;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MacroPinsTest {

    @Test
    void seqLockMacroPinsFollowDeclaredPortOrder() throws IOException {
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
                            "btn_pulse": [4,5,6],
                            "clear": ["0"],
                            "clk": [2],
                            "correct_pulse": [9],
                            "progress": [10,11],
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

        Graph graph = GraphBuilder.buildGraph(writeTempNetlist(json).toString());

        MacroVertex seq = null;
        Vertex clk = null;
        Vertex rst = null;
        Vertex a = null;
        Vertex b = null;
        Vertex c = null;

        for (Vertex vertex : graph.getVertices()) {
            if (vertex.getType() == MinecraftGraph.VertexType.FUNCTION) {
                Function function = (Function) vertex;
                if (function.getFunc_Type() == FunctionType.MC_SEQ_LOCK && seq == null) {
                    seq = (MacroVertex) function;
                }
            }

            if ("clk".equals(vertex.getID())) {
                clk = vertex;
            } else if ("rst".equals(vertex.getID())) {
                rst = vertex;
            } else if ("a_pressed".equals(vertex.getID())) {
                a = vertex;
            } else if ("b_pressed".equals(vertex.getID())) {
                b = vertex;
            } else if ("c_pressed".equals(vertex.getID())) {
                c = vertex;
            }
        }

        assertNotNull(seq);
        assertNotNull(clk);
        assertNotNull(rst);
        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        Vertex clearConst = null;
        for (Vertex before : seq.getBefore()) {
            if (before.getType() != MinecraftGraph.VertexType.FUNCTION) {
                continue;
            }
            Function function = (Function) before;
            if (function.getFunc_Type() == FunctionType.LOW) {
                clearConst = before;
                break;
            }
        }
        assertNotNull(clearConst);

        Circuit.TEST = true;
        try {
            MacroPins pins = new MacroPins(TestLogicGates.MC_SEQ_LOCK(3), seq, 0, false);

            assertEquals(0, pins.getNextPin(clk).xPos());
            assertEquals(2, pins.getNextPin(rst).xPos());
            assertEquals(4, pins.getNextPin(clearConst).xPos());
            assertEquals(6, pins.getNextPin(a).xPos());
            assertEquals(8, pins.getNextPin(b).xPos());
            assertEquals(10, pins.getNextPin(c).xPos());
        } finally {
            Circuit.TEST = false;
        }
    }

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-macro-pins", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }
}
