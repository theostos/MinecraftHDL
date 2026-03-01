module top(comp_2_level, comp_3_level, door);
  input wire [3:0] comp_2_level;
  input wire [3:0] comp_3_level;
  output wire door;
  wire __act_0;
  assign __act_0 = ((comp_2_level <= 2) & (comp_3_level <= 3));
  assign door = (__act_0) & ~(1'b0);
endmodule
