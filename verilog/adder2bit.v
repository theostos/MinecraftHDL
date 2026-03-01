// 2-bit adder: adds two 2-bit numbers, outputs 3-bit sum
module adder2bit(input a0, input a1, input b0, input b1, output s0, output s1, output carry);
    wire c0;
    assign s0 = a0 ^ b0;
    assign c0 = a0 & b0;
    assign s1 = a1 ^ b1 ^ c0;
    assign carry = (a1 & b1) | (a1 & c0) | (b1 & c0);
endmodule
