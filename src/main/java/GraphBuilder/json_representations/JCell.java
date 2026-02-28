package GraphBuilder.json_representations;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Francis O'Brien - 4/3/2017 - 19:39
 */

public class JCell extends Node{

    public String type;
    public LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
    public LinkedHashMap<String, String> port_directions;
    public LinkedHashMap<String, ArrayList<Object>> connections;

    public LinkedHashMap<String, JPort> ports = new LinkedHashMap<String, JPort>();

    @Override
    public ArrayList<Object> getNets() {
        return null;
    }

    public void posInit(){
        for (String p : port_directions.keySet()){
            ports.put(p, new JPort(p, port_directions.get(p), connections.get(p)));
        }


    }

    public int numInputs(){
        int conns = 0;
        for (String dir: port_directions.values()){
            if (dir.equals("input")) conns += 1;
        }
        return conns;
    }

    public void print(int tabs) {
        System.out.println(JsonFile.tabs(tabs) + "type: " + type);
        System.out.println(JsonFile.tabs(tabs) + "Ports: " + type);

        for (String p : ports.keySet()){
            System.out.println(JsonFile.tabs(tabs + 1) + p);
            ports.get(p).print(tabs + 1);
        }
    }
}
