module top(a_is_on, b_is_on, result);
  input wire a_is_on;
  input wire b_is_on;
  output wire result;
  wire __act_0;
  assign __act_0 = (a_is_on ^ b_is_on);
  assign result = (__act_0) & ~(1'b0);
endmodule
