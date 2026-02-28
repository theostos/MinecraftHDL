package minecrafthdl.synthesis;

public class Gate extends Circuit {

    public int num_inputs;
    public int num_outputs;
    public int input_spacing;
    public int output_spacing;
    public int[] output_lines;

    public String[] id_txt;
    public boolean is_io = false;

    public Gate(int sizeX, int sizeY, int sizeZ, int num_inputs, int num_outputs, int input_spacing, int output_spacing, int[] output_lines) {
        super(sizeX, sizeY, sizeZ);

        this.num_inputs = num_inputs;
        this.num_outputs = num_outputs;
        this.input_spacing = input_spacing;
        this.output_spacing = output_spacing;
        this.output_lines = output_lines;
    }
}
