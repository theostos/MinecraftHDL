// 1-bit ALU: op selects AND (0) or XOR (1)
module alu1bit(input a, input b, input op, output y);
    wire and_out, xor_out;
    assign and_out = a & b;
    assign xor_out = a ^ b;
    assign y = (and_out & ~op) | (xor_out & op);
endmodule
