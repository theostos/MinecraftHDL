package GraphBuilder;

import GraphBuilder.json_representations.JCell;
import GraphBuilder.json_representations.JPort;
import GraphBuilder.json_representations.JsonFile;
import GraphBuilder.json_representations.Module;
import MinecraftGraph.Function;
import MinecraftGraph.FunctionType;
import MinecraftGraph.Graph;
import MinecraftGraph.In_output;
import MinecraftGraph.MacroVertex;
import MinecraftGraph.MuxVertex;
import MinecraftGraph.Vertex;
import MinecraftGraph.VertexType;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import minecrafthdl.MHDLException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GraphBuilder {
    private static int highLowNets = Integer.MAX_VALUE;
    private static int generatedNetId = -1;
    private static int cellIds = 0;

    private static final int MAX_TIMER_TICKS = 1200;
    private static final int MAX_PERIOD_TICKS = 1200;
    private static final int MAX_COUNTER_WIDTH = 16;
    private static final int MAX_BTN_COUNT = 8;
    private static final int MAX_SEQ_LEN = 16;
    private static final int MAX_DEPART_TICKS = 1200;

    private static HashMap<Integer, Vertex> fromNet = new HashMap<Integer, Vertex>();
    private static HashMap<Integer, ArrayList<Vertex>> toNet = new HashMap<Integer, ArrayList<Vertex>>();

    private enum CellKind {
        AND,
        OR,
        XOR,
        INV,
        MUX,
        RELAY,
        NAND,
        NOR,
        XNOR,
        BUF,
        MC_TIMER,
        MC_PERIODIC,
        MC_LATCH,
        MC_COUNTER,
        MC_SEQ_LOCK,
        MC_STATION_FSM
    }

    private static final class MacroOutput {
        final String port;
        final int bitIndex;
        final int netId;

        MacroOutput(String port, int bitIndex, int netId) {
            this.port = port;
            this.bitIndex = bitIndex;
            this.netId = netId;
        }
    }

    public static Graph buildGraph(String path) {
        highLowNets = Integer.MAX_VALUE;
        generatedNetId = -1;
        cellIds = 0;

        JsonFile jsonFile = parseJson(path);
        Module module = selectTopModule(jsonFile);

        fromNet = new HashMap<Integer, Vertex>();
        toNet = new HashMap<Integer, ArrayList<Vertex>>();

        Graph graph = new Graph();

        for (String portName : module.ports.keySet()) {
            JPort port = module.ports.get(portName);
            In_output io;

            if ("input".equals(port.direction)) {
                io = new In_output(port.bits.size(), VertexType.INPUT, portName);
                for (Object bitToken : port.bits) {
                    putInFromNet(toNetId(bitToken), io);
                }
            } else if ("output".equals(port.direction)) {
                io = new In_output(port.bits.size(), VertexType.OUTPUT, portName);
                for (Object bitToken : port.bits) {
                    putInToNet(toNetId(bitToken), io, graph);
                }
            } else {
                throw new MHDLException("Unsupported port direction: " + port.direction);
            }

            graph.addVertex(io);
        }

        for (String cellName : module.cells.keySet()) {
            JCell cell = module.cells.get(cellName);
            addCell(graph, cell);
        }

        connectNets(graph);
        return graph;
    }

    private static JsonFile parseJson(String path) {
        Gson gson = new Gson();
        try (FileReader fr = new FileReader(path); JsonReader reader = new JsonReader(fr)) {
            reader.setLenient(true);
            JsonFile jsonFile = gson.fromJson(reader, JsonFile.class);
            if (jsonFile == null || jsonFile.modules == null || jsonFile.modules.isEmpty()) {
                throw new MHDLException("Invalid or empty JSON netlist: " + path);
            }
            jsonFile.postInit();
            return jsonFile;
        } catch (FileNotFoundException e) {
            throw new MHDLException("Netlist file not found: " + path);
        } catch (IOException e) {
            throw new MHDLException("Could not read netlist: " + path);
        }
    }

    private static Module selectTopModule(JsonFile jsonFile) {
        if (jsonFile.modules.size() == 1) {
            return jsonFile.modules.values().iterator().next();
        }

        for (Map.Entry<String, Module> entry : jsonFile.modules.entrySet()) {
            Module module = entry.getValue();
            if (module != null && module.attributes != null) {
                String topFlag = module.attributes.get("top");
                if (topFlag != null && topFlag.replace("0", "").length() > 0) {
                    return module;
                }
            }
        }

        Module namedTop = jsonFile.modules.get("top");
        if (namedTop != null) {
            return namedTop;
        }

        for (Map.Entry<String, Module> entry : jsonFile.modules.entrySet()) {
            if (!normalize(entry.getKey()).startsWith("$PARAMOD")) {
                return entry.getValue();
            }
        }

        throw new MHDLException("Could not determine top module. Ensure Yosys hierarchy marks a top module.");
    }

    private static void addCell(Graph graph, JCell cell) {
        CellKind kind = resolveKind(cell.type);

        switch (kind) {
            case AND:
                addSimpleCell(graph, cell, FunctionType.AND);
                break;
            case OR:
                addSimpleCell(graph, cell, FunctionType.OR);
                break;
            case XOR:
                addSimpleCell(graph, cell, FunctionType.XOR);
                break;
            case INV:
                addSimpleCell(graph, cell, FunctionType.INV);
                break;
            case RELAY:
            case BUF:
                addSimpleCell(graph, cell, FunctionType.RELAY);
                break;
            case MUX:
                addMuxCell(graph, cell);
                break;
            case NAND:
                addNandCell(graph, cell);
                break;
            case NOR:
                addNorCell(graph, cell);
                break;
            case XNOR:
                addXnorCell(graph, cell);
                break;
            case MC_TIMER:
                addMcTimerCell(graph, cell);
                break;
            case MC_PERIODIC:
                addMcPeriodicCell(graph, cell);
                break;
            case MC_LATCH:
                addMcLatchCell(graph, cell);
                break;
            case MC_COUNTER:
                addMcCounterCell(graph, cell);
                break;
            case MC_SEQ_LOCK:
                addMcSeqLockCell(graph, cell);
                break;
            case MC_STATION_FSM:
                addMcStationFsmCell(graph, cell);
                break;
            default:
                throw new MHDLException("Unsupported cell type: " + cell.type);
        }
    }

    private static void addSimpleCell(Graph graph, JCell cell, FunctionType functionType) {
        Function function = new Function(nextCellId(), functionType, countInputBits(cell));
        wireCellPorts(graph, cell, function);
        graph.addVertex(function);
    }

    private static void addMuxCell(Graph graph, JCell cell) {
        int aNet = requireSingleNet(cell, "A");
        int bNet = requireSingleNet(cell, "B");
        int sNet = requireSingleNet(cell, "S");

        MuxVertex mux = new MuxVertex(nextCellId(), FunctionType.MUX, 3);
        mux.a_net_num = aNet;
        mux.b_net_num = bNet;
        mux.s_net_num = sNet;

        putInToNet(aNet, mux, graph);
        putInToNet(bNet, mux, graph);
        putInToNet(sNet, mux, graph);

        int yNet = requireSingleNet(cell, "Y", "Q");
        putInFromNet(yNet, mux);

        graph.addVertex(mux);
    }

    private static void addNandCell(Graph graph, JCell cell) {
        int outNet = requireSingleNet(cell, "Y", "Q");
        int internalNet = nextGeneratedNet();

        Function andGate = new Function(nextCellId(), FunctionType.AND, countInputBits(cell));
        Function invGate = new Function(nextCellId(), FunctionType.INV, 1);

        wireOnlyInputs(graph, cell, andGate);
        putInFromNet(internalNet, andGate);

        putInToNet(internalNet, invGate, graph);
        putInFromNet(outNet, invGate);

        graph.addVertex(andGate);
        graph.addVertex(invGate);
    }

    private static void addNorCell(Graph graph, JCell cell) {
        int outNet = requireSingleNet(cell, "Y", "Q");
        int internalNet = nextGeneratedNet();

        Function orGate = new Function(nextCellId(), FunctionType.OR, countInputBits(cell));
        Function invGate = new Function(nextCellId(), FunctionType.INV, 1);

        wireOnlyInputs(graph, cell, orGate);
        putInFromNet(internalNet, orGate);

        putInToNet(internalNet, invGate, graph);
        putInFromNet(outNet, invGate);

        graph.addVertex(orGate);
        graph.addVertex(invGate);
    }

    private static void addXnorCell(Graph graph, JCell cell) {
        int outNet = requireSingleNet(cell, "Y", "Q");
        int internalNet = nextGeneratedNet();

        Function xorGate = new Function(nextCellId(), FunctionType.XOR, countInputBits(cell));
        Function invGate = new Function(nextCellId(), FunctionType.INV, 1);

        wireOnlyInputs(graph, cell, xorGate);
        putInFromNet(internalNet, xorGate);

        putInToNet(internalNet, invGate, graph);
        putInFromNet(outNet, invGate);

        graph.addVertex(xorGate);
        graph.addVertex(invGate);
    }

    private static void addMcTimerCell(Graph graph, JCell cell) {
        int ticks = parseParameter(cell, "TICKS", 60, 1, MAX_TIMER_TICKS);

        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("TICKS", (long) ticks);

        ArrayList<Integer> inputNets = orderedSingleBitInputs(cell, "clk", "rst", "trigger_pulse");
        ArrayList<MacroOutput> outputs = singleBitOutputs(cell, "active");

        addMacroVertices(graph, FunctionType.MC_TIMER, "mc_timer", params, inputNets, outputs);
    }

    private static void addMcPeriodicCell(Graph graph, JCell cell) {
        int period = parseParameter(cell, "PERIOD", 20, 1, MAX_PERIOD_TICKS);

        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("PERIOD", (long) period);

        ArrayList<Integer> inputNets = orderedSingleBitInputs(cell, "clk", "rst", "enable");
        ArrayList<MacroOutput> outputs = singleBitOutputs(cell, "pulse");

        addMacroVertices(graph, FunctionType.MC_PERIODIC, "mc_periodic", params, inputNets, outputs);
    }

    private static void addMcLatchCell(Graph graph, JCell cell) {
        HashMap<String, Long> params = new HashMap<String, Long>();

        ArrayList<Integer> inputNets = orderedSingleBitInputs(cell, "clk", "rst", "set_pulse", "clear_pulse");
        ArrayList<MacroOutput> outputs = singleBitOutputs(cell, "q");

        addMacroVertices(graph, FunctionType.MC_LATCH, "mc_latch", params, inputNets, outputs);
    }

    private static void addMcCounterCell(Graph graph, JCell cell) {
        int width = parseParameter(cell, "WIDTH", 8, 1, MAX_COUNTER_WIDTH);

        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("WIDTH", (long) width);

        ArrayList<Integer> inputNets = orderedSingleBitInputs(cell, "clk", "rst", "inc_pulse", "clear_pulse");
        ArrayList<MacroOutput> outputs = vectorOutputs(cell, "count", width);

        addMacroVertices(graph, FunctionType.MC_COUNTER, "mc_counter", params, inputNets, outputs);
    }

    private static void addMcSeqLockCell(Graph graph, JCell cell) {
        int btnCount = parseParameter(cell, "BTN_COUNT", 3, 1, MAX_BTN_COUNT);
        int seqLen = parseParameter(cell, "SEQ_LEN", 3, 1, MAX_SEQ_LEN);
        int latchSuccess = parseParameter(cell, "LATCH_SUCCESS", 1, 0, 1);
        int expectBits = ceilLog2(btnCount) * seqLen;
        long maxExpectIdx = expectBits >= 63 ? Long.MAX_VALUE : ((1L << expectBits) - 1L);
        long expectIdx = parseParameterLong(cell, "EXPECT_IDX", 0L, 0L, maxExpectIdx);

        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("BTN_COUNT", (long) btnCount);
        params.put("SEQ_LEN", (long) seqLen);
        params.put("LATCH_SUCCESS", (long) latchSuccess);
        params.put("EXPECT_IDX", expectIdx);

        ArrayList<Integer> inputNets = orderedSingleBitInputs(cell, "clk", "rst", "clear");
        inputNets.addAll(vectorInput(cell, "btn_pulse", btnCount));

        ArrayList<MacroOutput> outputs = new ArrayList<MacroOutput>();
        outputs.addAll(singleBitOutputs(cell, "unlocked"));
        outputs.addAll(singleBitOutputs(cell, "correct_pulse"));
        outputs.addAll(singleBitOutputs(cell, "wrong_pulse"));

        int progressWidth = ceilLog2(seqLen + 1);
        outputs.addAll(vectorOutputs(cell, "progress", progressWidth));

        addMacroVertices(graph, FunctionType.MC_SEQ_LOCK, "mc_seq_lock", params, inputNets, outputs);
    }

    private static void addMcStationFsmCell(Graph graph, JCell cell) {
        int departTicks = parseParameter(cell, "DEPART_TICKS", 20, 1, MAX_DEPART_TICKS);

        HashMap<String, Long> params = new HashMap<String, Long>();
        params.put("DEPART_TICKS", (long) departTicks);

        ArrayList<Integer> inputNets = orderedSingleBitInputs(cell, "clk", "rst", "clear", "arrival_pulse", "depart_pulse");

        ArrayList<MacroOutput> outputs = new ArrayList<MacroOutput>();
        outputs.addAll(singleBitOutputs(cell, "occupied"));
        outputs.addAll(singleBitOutputs(cell, "depart_now"));

        addMacroVertices(graph, FunctionType.MC_STATION_FSM, "mc_station_fsm", params, inputNets, outputs);
    }

    private static void addMacroVertices(Graph graph, FunctionType type, String macroName, Map<String, Long> params, ArrayList<Integer> inputNets, ArrayList<MacroOutput> outputs) {
        if (outputs.isEmpty()) {
            throw new MHDLException("Macro " + macroName + " has no outputs");
        }

        for (MacroOutput out : outputs) {
            MacroVertex macroVertex = new MacroVertex(nextCellId(), type, inputNets.size(), macroName, out.port, out.bitIndex, params);

            for (int inNet : inputNets) {
                putInToNet(inNet, macroVertex, graph);
            }

            putInFromNet(out.netId, macroVertex);
            graph.addVertex(macroVertex);
        }
    }

    private static ArrayList<Integer> orderedSingleBitInputs(JCell cell, String... names) {
        ArrayList<Integer> nets = new ArrayList<Integer>();
        for (String name : names) {
            nets.add(requireSingleNet(cell, name));
        }
        return nets;
    }

    private static ArrayList<Integer> vectorInput(JCell cell, String name, int expectedWidth) {
        ArrayList<Object> bits = requireBits(cell, name);
        if (bits.size() != expectedWidth) {
            throw new MHDLException("Macro input " + name + " width mismatch. Expected " + expectedWidth + " got " + bits.size());
        }
        ArrayList<Integer> nets = new ArrayList<Integer>();
        for (Object bit : bits) {
            nets.add(toNetId(bit));
        }
        return nets;
    }

    private static ArrayList<MacroOutput> singleBitOutputs(JCell cell, String name) {
        ArrayList<Object> bits = requireBits(cell, name);
        if (bits.size() != 1) {
            throw new MHDLException("Macro output " + name + " must be 1-bit");
        }

        ArrayList<MacroOutput> outputs = new ArrayList<MacroOutput>();
        outputs.add(new MacroOutput(name, 0, toNetId(bits.get(0))));
        return outputs;
    }

    private static ArrayList<MacroOutput> vectorOutputs(JCell cell, String name, int expectedWidth) {
        ArrayList<Object> bits = requireBits(cell, name);
        if (bits.size() != expectedWidth) {
            throw new MHDLException("Macro output " + name + " width mismatch. Expected " + expectedWidth + " got " + bits.size());
        }

        ArrayList<MacroOutput> outputs = new ArrayList<MacroOutput>();
        for (int i = 0; i < bits.size(); i++) {
            outputs.add(new MacroOutput(name, i, toNetId(bits.get(i))));
        }
        return outputs;
    }

    private static ArrayList<Object> requireBits(JCell cell, String portName) {
        ArrayList<Object> bits = cell.connections.get(portName);
        if (bits == null || bits.isEmpty()) {
            throw new MHDLException("Cell " + cell.type + " missing required port " + portName);
        }
        return bits;
    }

    private static int parseParameter(JCell cell, String key, int defaultValue, int minInclusive, int maxInclusive) {
        long value = defaultValue;

        if (cell.parameters != null && cell.parameters.containsKey(key)) {
            value = parseBinaryLiteral(cell.parameters.get(key));
        }

        Long fromType = parseParameterFromType(cell.type, key);
        if (fromType != null) {
            value = fromType;
        }

        if (value < minInclusive || value > maxInclusive) {
            throw new MHDLException("Macro parameter " + key + " out of bounds: " + value
                    + " (allowed " + minInclusive + ".." + maxInclusive + ")");
        }

        return (int) value;
    }

    private static long parseParameterLong(JCell cell, String key, long defaultValue, long minInclusive, long maxInclusive) {
        long value = defaultValue;

        if (cell.parameters != null && cell.parameters.containsKey(key)) {
            value = parseBinaryLiteral(cell.parameters.get(key));
        }

        Long fromType = parseParameterFromType(cell.type, key);
        if (fromType != null) {
            value = fromType;
        }

        if (value < minInclusive || value > maxInclusive) {
            throw new MHDLException("Macro parameter " + key + " out of bounds: " + value
                    + " (allowed " + minInclusive + ".." + maxInclusive + ")");
        }

        return value;
    }

    private static Long parseParameterFromType(String cellType, String key) {
        String type = normalize(cellType);
        String token = normalize(key) + "=S";

        int idx = type.indexOf(token);
        if (idx < 0) {
            return null;
        }

        int quote = type.indexOf('\'', idx);
        if (quote < 0 || quote + 1 >= type.length()) {
            return null;
        }

        int end = quote + 1;
        while (end < type.length()) {
            char c = type.charAt(end);
            if (c != '0' && c != '1') {
                break;
            }
            end++;
        }

        if (end == quote + 1) {
            return null;
        }

        return parseBinaryLiteral(type.substring(quote + 1, end));
    }

    private static long parseBinaryLiteral(String literal) {
        if (literal == null) {
            throw new MHDLException("Null parameter literal");
        }

        String raw = literal.trim();
        int quoteIndex = raw.indexOf('\'');
        String bits = (quoteIndex >= 0 && quoteIndex + 1 < raw.length()) ? raw.substring(quoteIndex + 1) : raw;
        bits = bits.replace("_", "");

        if (bits.isEmpty()) {
            throw new MHDLException("Invalid parameter literal: " + literal);
        }

        if (bits.length() > 63) {
            throw new MHDLException("Parameter literal too wide for runtime support: " + literal);
        }

        long value = 0L;
        for (int i = 0; i < bits.length(); i++) {
            char c = bits.charAt(i);
            value <<= 1;
            if (c == '1') {
                value |= 1;
            } else if (c == '0') {
                // noop
            } else {
                throw new MHDLException("Unsupported parameter bit in literal: " + literal);
            }
        }

        return value;
    }

    private static int ceilLog2(int x) {
        int v = 0;
        int n = Math.max(1, x - 1);
        while (n > 0) {
            n >>= 1;
            v++;
        }
        return Math.max(1, v);
    }

    private static void wireCellPorts(Graph graph, JCell cell, Function function) {
        for (String connName : cell.connections.keySet()) {
            String direction = cell.port_directions.get(connName);
            ArrayList<Object> connNets = cell.connections.get(connName);

            if ("input".equals(direction)) {
                for (Object bitToken : connNets) {
                    int net = putInToNet(toNetId(bitToken), function, graph);
                    if (function.func_type == FunctionType.MUX && function instanceof MuxVertex) {
                        if ("S".equals(connName)) {
                            ((MuxVertex) function).s_net_num = net;
                        } else if ("A".equals(connName)) {
                            ((MuxVertex) function).a_net_num = net;
                        } else if ("B".equals(connName)) {
                            ((MuxVertex) function).b_net_num = net;
                        }
                    }
                }
            } else if ("output".equals(direction)) {
                for (Object bitToken : connNets) {
                    putInFromNet(toNetId(bitToken), function);
                }
            } else {
                throw new MHDLException("Unknown cell connection direction: " + direction);
            }
        }
    }

    private static void wireOnlyInputs(Graph graph, JCell cell, Function function) {
        for (String connName : cell.connections.keySet()) {
            String direction = cell.port_directions.get(connName);
            if (!"input".equals(direction)) {
                continue;
            }
            for (Object bitToken : cell.connections.get(connName)) {
                putInToNet(toNetId(bitToken), function, graph);
            }
        }
    }

    private static int countInputBits(JCell cell) {
        int count = 0;
        for (String connName : cell.port_directions.keySet()) {
            if ("input".equals(cell.port_directions.get(connName))) {
                count += cell.connections.get(connName).size();
            }
        }
        return Math.max(1, count);
    }

    private static int requireSingleNet(JCell cell, String... candidateNames) {
        for (String name : candidateNames) {
            ArrayList<Object> bits = cell.connections.get(name);
            if (bits == null || bits.isEmpty()) {
                continue;
            }
            if (bits.size() != 1) {
                throw new MHDLException("Cell " + cell.type + " port " + name + " must be 1-bit");
            }
            return toNetId(bits.get(0));
        }

        StringBuilder names = new StringBuilder();
        for (int i = 0; i < candidateNames.length; i++) {
            if (i > 0) {
                names.append("/");
            }
            names.append(candidateNames[i]);
        }
        throw new MHDLException("Cell " + cell.type + " missing required port " + names);
    }

    private static void connectNets(Graph graph) {
        for (int net : toNet.keySet()) {
            Vertex from = fromNet.get(net);
            if (from == null) {
                throw new MHDLException("Net " + net + " has no driver vertex");
            }

            for (Vertex to : toNet.get(net)) {
                if (to.type == VertexType.FUNCTION) {
                    Function function = (Function) to;
                    if (function.func_type == FunctionType.MUX && function instanceof MuxVertex) {
                        MuxVertex mux = (MuxVertex) function;
                        if (net == mux.a_net_num) {
                            mux.a_vertex = from;
                        } else if (net == mux.b_net_num) {
                            mux.b_vertex = from;
                        } else if (net == mux.s_net_num) {
                            mux.s_vertex = from;
                        }
                    }
                }

                graph.addEdge(from, to);
            }
        }
    }

    private static CellKind resolveKind(String type) {
        String normalized = normalize(type);

        if (containsMacro(normalized, "mc_timer")) {
            return CellKind.MC_TIMER;
        }
        if (containsMacro(normalized, "mc_periodic")) {
            return CellKind.MC_PERIODIC;
        }
        if (containsMacro(normalized, "mc_latch")) {
            return CellKind.MC_LATCH;
        }
        if (containsMacro(normalized, "mc_counter")) {
            return CellKind.MC_COUNTER;
        }
        if (containsMacro(normalized, "mc_seq_lock")) {
            return CellKind.MC_SEQ_LOCK;
        }
        if (containsMacro(normalized, "mc_station_fsm")) {
            return CellKind.MC_STATION_FSM;
        }

        if (isNonWhitelistedSequentialCell(normalized)) {
            throw new MHDLException("Sequential cell is not allowed outside whitelisted mc_* macros: " + type);
        }

        if (normalized.contains("XNOR")) {
            return CellKind.XNOR;
        }
        if (normalized.contains("NAND")) {
            return CellKind.NAND;
        }
        if (normalized.contains("NOR")) {
            return CellKind.NOR;
        }
        if (normalized.contains("MUX")) {
            return CellKind.MUX;
        }
        if (normalized.contains("XOR")) {
            return CellKind.XOR;
        }
        if (normalized.contains("AND")) {
            return CellKind.AND;
        }
        if (normalized.contains("NOT") || normalized.contains("INV")) {
            return CellKind.INV;
        }
        if (normalized.contains("OR")) {
            return CellKind.OR;
        }
        if (normalized.contains("BUF")) {
            return CellKind.BUF;
        }
        if (normalized.contains("RELAY")) {
            return CellKind.RELAY;
        }

        throw new MHDLException("Unknown cell: " + type);
    }

    private static boolean containsMacro(String normalizedType, String macroName) {
        String macro = normalize(macroName);
        return normalizedType.equals(macro)
                || normalizedType.contains("\\" + macro)
                || normalizedType.contains("$PARAMOD\\" + macro)
                || normalizedType.contains("$PARAMOD_" + macro)
                || normalizedType.contains("/" + macro)
                || normalizedType.contains("_" + macro);
    }

    private static boolean isNonWhitelistedSequentialCell(String normalizedType) {
        return normalizedType.contains("DFF")
                || normalizedType.contains("DLATCH")
                || normalizedType.contains("LATCH")
                || normalizedType.contains("ADFF")
                || normalizedType.contains("SDFF")
                || normalizedType.contains("ALDFF")
                || normalizedType.contains("SR") && normalizedType.contains("FF");
    }

    private static String normalize(String str) {
        if (str == null) {
            return "";
        }
        return str.trim().toUpperCase(Locale.ROOT);
    }

    public static int putInToNet(int net, Vertex vertex, Graph graph) {
        if (net == 0 || net == 1) {
            highLowNets--;
            FunctionType constantType = (net == 0) ? FunctionType.LOW : FunctionType.HIGH;
            Function constant = new Function(nextCellId(), constantType, 0);
            fromNet.put(highLowNets, constant);
            toNet.put(highLowNets, new ArrayList<Vertex>());
            toNet.get(highLowNets).add(vertex);
            graph.addVertex(constant);
            return highLowNets;
        }

        if (!toNet.containsKey(net)) {
            toNet.put(net, new ArrayList<Vertex>());
        }
        toNet.get(net).add(vertex);
        return net;
    }

    public static int putInFromNet(int net, Vertex vertex) {
        if (fromNet.containsKey(net)) {
            throw new MHDLException("Two outputs on same net id: " + net);
        }
        fromNet.put(net, vertex);
        return net;
    }

    private static int toNetId(Object bitToken) {
        if (bitToken instanceof Number) {
            return ((Number) bitToken).intValue();
        }

        if (bitToken instanceof String) {
            String token = ((String) bitToken).trim();
            if ("0".equals(token)) {
                return 0;
            }
            if ("1".equals(token)) {
                return 1;
            }
            if ("x".equalsIgnoreCase(token) || "z".equalsIgnoreCase(token)) {
                return 0;
            }
            throw new MHDLException("Unsupported literal net token: '" + token + "'");
        }

        throw new MHDLException("Unsupported net token type: " + bitToken);
    }

    private static int nextCellId() {
        int id = cellIds;
        cellIds++;
        return id;
    }

    private static int nextGeneratedNet() {
        int id = generatedNetId;
        generatedNetId--;
        return id;
    }
}
