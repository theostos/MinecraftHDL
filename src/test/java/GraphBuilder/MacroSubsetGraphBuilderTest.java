package GraphBuilder;

import MinecraftGraph.Function;
import MinecraftGraph.FunctionType;
import MinecraftGraph.Graph;
import MinecraftGraph.MacroVertex;
import MinecraftGraph.Vertex;
import minecrafthdl.MHDLException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MacroSubsetGraphBuilderTest {

    @Test
    void buildGraphUsesTopModuleAndSupportsParamodMacroCell() throws IOException {
        String json = """
                {
                  "modules": {
                    "$paramod\\\\mc_timer\\\\TICKS=s32'00000000000000000000000000010100": {
                      "attributes": {"src": "dummy"},
                      "ports": {},
                      "cells": {}
                    },
                    "demo_top": {
                      "attributes": {"top": "00000000000000000000000000000001"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "trigger": {"direction": "input", "bits": [4]},
                        "active": {"direction": "output", "bits": [5]}
                      },
                      "cells": {
                        "u0": {
                          "type": "$paramod\\\\mc_timer\\\\TICKS=s32'00000000000000000000000000010100",
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

        Graph graph = GraphBuilder.buildGraph(writeTempNetlist(json).toString());
        assertEquals(1, countFunctions(graph, FunctionType.MC_TIMER));
    }

    @Test
    void buildGraphRejectsSequentialCellsOutsideWhitelist() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "d": {"direction": "input", "bits": [3]},
                        "q": {"direction": "output", "bits": [4]}
                      },
                      "cells": {
                        "u0": {
                          "type": "$_DFF_P_",
                          "port_directions": {"C":"input","D":"input","Q":"output"},
                          "connections": {"C":[2],"D":[3],"Q":[4]}
                        }
                      }
                    }
                  }
                }
                """;

        Path netlist = writeTempNetlist(json);
        assertThrows(MHDLException.class, () -> GraphBuilder.buildGraph(netlist.toString()));
    }

    @Test
    void buildGraphExpandsCounterVectorOutputToBitVertices() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "inc": {"direction": "input", "bits": [4]},
                        "clr": {"direction": "input", "bits": [5]},
                        "count0": {"direction": "output", "bits": [6]},
                        "count1": {"direction": "output", "bits": [7]},
                        "count2": {"direction": "output", "bits": [8]},
                        "count3": {"direction": "output", "bits": [9]}
                      },
                      "cells": {
                        "u0": {
                          "type": "$paramod\\\\mc_counter\\\\WIDTH=s32'00000000000000000000000000000100",
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "inc_pulse":"input",
                            "clear_pulse":"input",
                            "count":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "inc_pulse":[4],
                            "clear_pulse":[5],
                            "count":[6,7,8,9]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Graph graph = GraphBuilder.buildGraph(writeTempNetlist(json).toString());
        assertEquals(4, countFunctions(graph, FunctionType.MC_COUNTER));
    }

    @Test
    void buildGraphRejectsOutOfBoundsMacroParameter() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "inc": {"direction": "input", "bits": [4]},
                        "clr": {"direction": "input", "bits": [5]},
                        "count0": {"direction": "output", "bits": [6]}
                      },
                      "cells": {
                        "u0": {
                          "type": "$paramod\\\\mc_counter\\\\WIDTH=s32'00000000000000000000000100000000",
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "inc_pulse":"input",
                            "clear_pulse":"input",
                            "count":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "inc_pulse":[4],
                            "clear_pulse":[5],
                            "count":[6]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Path netlist = writeTempNetlist(json);
        assertThrows(MHDLException.class, () -> GraphBuilder.buildGraph(netlist.toString()));
    }

    @Test
    void buildGraphParsesSeqLockExpectIdxParameter() throws IOException {
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
        MacroVertex firstSeq = null;
        for (Vertex vertex : graph.getVertices()) {
            if (vertex.getType() != MinecraftGraph.VertexType.FUNCTION) {
                continue;
            }
            Function function = (Function) vertex;
            if (function.getFunc_Type() == FunctionType.MC_SEQ_LOCK) {
                firstSeq = (MacroVertex) function;
                break;
            }
        }
        assertNotNull(firstSeq);
        assertEquals(36L, firstSeq.getParams().get("EXPECT_IDX"));
        assertEquals("u0", firstSeq.getInstanceName());
    }

    @Test
    void expandedMacroBitVerticesKeepSameInstanceName() throws IOException {
        String json = """
                {
                  "modules": {
                    "top": {
                      "attributes": {"top": "1"},
                      "ports": {
                        "clk": {"direction": "input", "bits": [2]},
                        "rst": {"direction": "input", "bits": [3]},
                        "inc": {"direction": "input", "bits": [4]},
                        "clr": {"direction": "input", "bits": [5]},
                        "count0": {"direction": "output", "bits": [6]},
                        "count1": {"direction": "output", "bits": [7]},
                        "count2": {"direction": "output", "bits": [8]},
                        "count3": {"direction": "output", "bits": [9]}
                      },
                      "cells": {
                        "uCounter": {
                          "type": "mc_counter",
                          "parameters": {
                            "WIDTH": "00000000000000000000000000000100"
                          },
                          "port_directions": {
                            "clk":"input",
                            "rst":"input",
                            "inc_pulse":"input",
                            "clear_pulse":"input",
                            "count":"output"
                          },
                          "connections": {
                            "clk":[2],
                            "rst":[3],
                            "inc_pulse":[4],
                            "clear_pulse":[5],
                            "count":[6,7,8,9]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        Graph graph = GraphBuilder.buildGraph(writeTempNetlist(json).toString());

        String sharedName = null;
        int seen = 0;
        for (Vertex vertex : graph.getVertices()) {
            if (vertex.getType() != MinecraftGraph.VertexType.FUNCTION) {
                continue;
            }
            Function function = (Function) vertex;
            if (function.getFunc_Type() != FunctionType.MC_COUNTER) {
                continue;
            }
            MacroVertex macro = (MacroVertex) function;
            if (sharedName == null) {
                sharedName = macro.getInstanceName();
            } else {
                assertEquals(sharedName, macro.getInstanceName());
            }
            seen++;
        }

        assertFalse(sharedName == null || sharedName.isBlank());
        assertEquals(4, seen);
        assertEquals("uCounter", sharedName);
    }

    private static long countFunctions(Graph graph, FunctionType type) {
        long count = 0;
        for (Vertex vertex : graph.getVertices()) {
            if (vertex.getType() == MinecraftGraph.VertexType.FUNCTION) {
                Function function = (Function) vertex;
                if (function.getFunc_Type() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Path writeTempNetlist(String json) throws IOException {
        Path file = Files.createTempFile("mhdl-macro-netlist", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }
}
