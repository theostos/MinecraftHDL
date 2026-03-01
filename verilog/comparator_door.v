module top(comparator_level, door);
  input wire [3:0] comparator_level;
  output wire door;
  wire __act_0;
  assign __act_0 = (comparator_level > 3);
  assign door = (__act_0) & ~(1'b0);
endmodule
