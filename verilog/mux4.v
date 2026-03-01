// 4-to-1 multiplexer: select one of 4 inputs using 2 select lines
module mux4(input a, input b, input c, input d, input s0, input s1, output y);
    assign y = (a & ~s1 & ~s0) |
               (b & ~s1 &  s0) |
               (c &  s1 & ~s0) |
               (d &  s1 &  s0);
endmodule
