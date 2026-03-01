module top(input_is_on, output);
  input wire input_is_on;
  output wire output;
  wire __act_0;
  assign __act_0 = input_is_on;
  assign output = (__act_0) & ~(1'b0);
endmodule
