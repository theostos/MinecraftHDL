// 4-bit ripple carry adder: adds two 4-bit numbers, outputs 5-bit sum
module adder4bit(input a0, input a1, input a2, input a3,
                 input b0, input b1, input b2, input b3,
                 output s0, output s1, output s2, output s3, output carry);
    wire c0, c1, c2;
    assign s0 = a0 ^ b0;
    assign c0 = a0 & b0;
    assign s1 = a1 ^ b1 ^ c0;
    assign c1 = (a1 & b1) | (a1 & c0) | (b1 & c0);
    assign s2 = a2 ^ b2 ^ c1;
    assign c2 = (a2 & b2) | (a2 & c1) | (b2 & c1);
    assign s3 = a3 ^ b3 ^ c2;
    assign carry = (a3 & b3) | (a3 & c2) | (b3 & c2);
endmodule
