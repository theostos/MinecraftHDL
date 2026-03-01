package minecrafthdl.synthesis.macro;

import GraphBuilder.GraphBuilder;
import MinecraftGraph.FunctionType;
import MinecraftGraph.Graph;
import MinecraftGraph.MacroVertex;
import MinecraftGraph.Vertex;
import minecrafthdl.MHDLException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroInstanceGroupsTest {

    @Test
    void groupsExpandedOutputsFromSameMacroInstance() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "clr": {"direction": "input", "bits": [4]},
                        "b0": {"direction": "input", "bits": [5]},
                        "b1": {"direction": "input", "bits": [6]},
                        "b2": {"direction": "input", "bits": [7]},
                        "ok": {"direction": "output", "bits": [8]}
                      },
                      "cells": {
                        "u_seq": {
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
                            "clear":[4],
                            "btn_pulse":[5,6,7],
                            "unlocked":[8],
                            "correct_pulse":[9],
                            "wrong_pulse":[10],
                            "progress":[11,12]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Graph graph = GraphBuilder.buildGraph(writeTempNetlist(json).toString());
        LinkedHashMap<String, MacroInstanceGroups.Group> groups = MacroInstanceGroups.fromVertices(graph.getVertices());

        assertTrue(groups.containsKey("u_seq"));
        MacroInstanceGroups.Group seq = groups.get("u_seq");
        assertEquals("mc_seq_lock", seq.getMacroName());
        assertEquals(5, seq.getMembers().size());
    }

    @Test
    void rejectsInconsistentParamsInsideSameInstance() {
        ArrayList<Integer> nets = new ArrayList<Integer>();
        nets.add(2);
        nets.add(3);
        nets.add(4);

        HashMap<String, Long> p1 = new HashMap<String, Long>();
        p1.put("TICKS", 10L);
        HashMap<String, Long> p2 = new HashMap<String, Long>();
        p2.put("TICKS", 11L);

        MacroVertex v1 = new MacroVertex(1, FunctionType.MC_TIMER, 3, "u_timer", "mc_timer", "active", 0, p1, nets);
        MacroVertex v2 = new MacroVertex(2, FunctionType.MC_TIMER, 3, "u_timer", "mc_timer", "active", 0, p2, nets);

        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        vertices.add(v1);
        vertices.add(v2);

        assertThrows(MHDLException.class, () -> MacroInstanceGroups.fromVertices(vertices));
    }

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-macro-groups", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }
}
