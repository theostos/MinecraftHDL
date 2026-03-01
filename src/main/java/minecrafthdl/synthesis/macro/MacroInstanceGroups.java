package minecrafthdl.synthesis.macro;

import MinecraftGraph.MacroVertex;
import MinecraftGraph.Vertex;
import MinecraftGraph.VertexType;
import minecrafthdl.MHDLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups split macro output vertices back into logical macro instances.
 * This is used as a prefab-synthesis foundation where one macro instance
 * should be represented by a shared implementation.
 */
public final class MacroInstanceGroups {

    private MacroInstanceGroups() {
    }

    public static final class Group {
        private final String instanceName;
        private final String macroName;
        private final ArrayList<Integer> orderedInputNets;
        private final LinkedHashMap<String, Long> params;
        private final ArrayList<MacroVertex> members = new ArrayList<MacroVertex>();

        private Group(String instanceName, MacroVertex seed) {
            this.instanceName = instanceName;
            this.macroName = seed.getMacroName();
            this.orderedInputNets = new ArrayList<Integer>(seed.getOrderedInputNets());
            this.params = new LinkedHashMap<String, Long>(seed.getParams());
            this.members.add(seed);
        }

        private void add(MacroVertex vertex) {
            if (!this.macroName.equals(vertex.getMacroName())) {
                throw new MHDLException("Inconsistent macro type within instance '" + this.instanceName + "'");
            }
            if (!this.orderedInputNets.equals(vertex.getOrderedInputNets())) {
                throw new MHDLException("Inconsistent macro inputs within instance '" + this.instanceName + "'");
            }
            if (!this.params.equals(vertex.getParams())) {
                throw new MHDLException("Inconsistent macro parameters within instance '" + this.instanceName + "'");
            }
            this.members.add(vertex);
        }

        public String getInstanceName() {
            return this.instanceName;
        }

        public String getMacroName() {
            return this.macroName;
        }

        public List<Integer> getOrderedInputNets() {
            return this.orderedInputNets;
        }

        public Map<String, Long> getParams() {
            return this.params;
        }

        public List<MacroVertex> getMembers() {
            return this.members;
        }
    }

    public static LinkedHashMap<String, Group> fromVertices(List<Vertex> vertices) {
        LinkedHashMap<String, Group> groups = new LinkedHashMap<String, Group>();
        for (Vertex vertex : vertices) {
            if (vertex.getType() != VertexType.FUNCTION) {
                continue;
            }
            if (!(vertex instanceof MacroVertex)) {
                continue;
            }
            MacroVertex macroVertex = (MacroVertex) vertex;
            String key = keyFor(macroVertex);
            Group group = groups.get(key);
            if (group == null) {
                groups.put(key, new Group(key, macroVertex));
            } else {
                group.add(macroVertex);
            }
        }
        return groups;
    }

    private static String keyFor(MacroVertex vertex) {
        String instanceName = vertex.getInstanceName();
        if (instanceName != null && !instanceName.isBlank()) {
            return instanceName;
        }
        return "__anon_" + vertex.getMacroName() + "_" + vertex.getID();
    }
}
