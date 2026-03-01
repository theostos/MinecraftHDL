// MinecraftHDL feature showcase
// Exercises:
// - Hierarchy (multiple modules)
// - Sequential logic with reset + enable
// - Feedback paths (FSM, LFSR, accumulator)
// - Case-based muxing and combinational arithmetic/bitwise logic

module alu4 (
    input  [3:0] a,
    input  [3:0] b,
    input  [2:0] opcode,
    output reg [3:0] y,
    output parity,
    output eq,
    output gt
);
    always @(*) begin
        case (opcode)
            3'b000: y = a + b;
            3'b001: y = a - b;
            3'b010: y = a ^ b;
            3'b011: y = ~(a & b);      // NAND style
            3'b100: y = ~(a | b);      // NOR style
            3'b101: y = {a[2:0], a[3] ^ b[0]};
            3'b110: y = {b[0], a[3:1]};
            default: y = a ^~ b;       // XNOR style
        endcase
    end

    assign parity = y[0] ^ y[1] ^ y[2] ^ y[3];
    assign eq = (a == b);
    assign gt = (a > b);
endmodule

module seq_fsm (
    input clk,
    input rst,
    input en,
    input serial_in,
    output reg [1:0] state,
    output reg pulse
);
    // Detects sequence 1-0-1-1 (overlap enabled).
    always @(posedge clk) begin
        if (rst) begin
            state <= 2'b00;
            pulse <= 1'b0;
        end else if (en) begin
            pulse <= 1'b0;
            case (state)
                2'b00: state <= serial_in ? 2'b01 : 2'b00;
                2'b01: state <= serial_in ? 2'b01 : 2'b10;
                2'b10: state <= serial_in ? 2'b11 : 2'b00;
                default: begin
                    if (serial_in) begin
                        state <= 2'b01;
                        pulse <= 1'b1;
                    end else begin
                        state <= 2'b10;
                    end
                end
            endcase
        end else begin
            pulse <= 1'b0;
        end
    end
endmodule

module lfsr8 (
    input clk,
    input rst,
    input en,
    input load_seed,
    input [7:0] seed,
    output reg [7:0] q
);
    wire feedback;
    assign feedback = q[7] ^ q[5] ^ q[4] ^ q[3];

    always @(posedge clk) begin
        if (rst) begin
            q <= 8'b0000_0001;
        end else if (load_seed) begin
            q <= seed;
        end else if (en) begin
            q <= {q[6:0], feedback};
        end
    end
endmodule

module mhdl_feature_showcase (
    input clk,
    input rst,
    input en,
    input load_seed,
    input serial_in,
    input mode0,
    input mode1,
    input mode2,

    input a0,
    input a1,
    input a2,
    input a3,
    input b0,
    input b1,
    input b2,
    input b3,

    input seed0,
    input seed1,
    input seed2,
    input seed3,
    input seed4,
    input seed5,
    input seed6,
    input seed7,

    output out0,
    output out1,
    output out2,
    output out3,
    output out4,
    output out5,
    output out6,
    output out7,

    output pulse,
    output state0,
    output state1,
    output flag_eq,
    output flag_gt,
    output flag_parity
);
    wire [3:0] a_bus;
    wire [3:0] b_bus;
    wire [2:0] opcode;
    wire [7:0] seed_bus;

    assign a_bus = {a3, a2, a1, a0};
    assign b_bus = {b3, b2, b1, b0};
    assign opcode = {mode2, mode1, mode0};
    assign seed_bus = {seed7, seed6, seed5, seed4, seed3, seed2, seed1, seed0};

    wire [3:0] alu_y;
    wire alu_parity;
    wire alu_eq;
    wire alu_gt;

    alu4 u_alu (
        .a(a_bus),
        .b(b_bus),
        .opcode(opcode),
        .y(alu_y),
        .parity(alu_parity),
        .eq(alu_eq),
        .gt(alu_gt)
    );

    wire [1:0] fsm_state;
    wire fsm_pulse;

    seq_fsm u_fsm (
        .clk(clk),
        .rst(rst),
        .en(en),
        .serial_in(serial_in),
        .state(fsm_state),
        .pulse(fsm_pulse)
    );

    wire [7:0] lfsr_q;

    lfsr8 u_lfsr (
        .clk(clk),
        .rst(rst),
        .en(en),
        .load_seed(load_seed),
        .seed(seed_bus),
        .q(lfsr_q)
    );

    wire [7:0] mixed0;
    wire [7:0] mixed1;
    wire [7:0] selected_mix;

    assign mixed0 = {alu_y, alu_y} ^ lfsr_q;
    assign mixed1 = ({4'b0000, alu_y} + {4'b0000, fsm_state, mode0, serial_in});
    assign selected_mix = mode2 ? mixed1 : mixed0;

    reg [7:0] accum;
    always @(posedge clk) begin
        if (rst) begin
            accum <= 8'h00;
        end else if (en) begin
            case ({mode2, mode1})
                2'b00: accum <= accum + selected_mix;
                2'b01: accum <= {accum[6:0], accum[7] ^ serial_in};
                2'b10: accum <= (accum ^ selected_mix) + {7'b0000000, fsm_pulse};
                default: accum <= {accum[3:0], accum[7:4]} ^ {4'b0000, alu_y};
            endcase
        end
    end

    wire [7:0] final_out;
    assign final_out = fsm_state[1] ? accum : (fsm_state[0] ? (lfsr_q ^ accum) : lfsr_q);

    assign out0 = final_out[0] ^ fsm_pulse;
    assign out1 = final_out[1] ^ alu_parity;
    assign out2 = final_out[2] ^ alu_eq;
    assign out3 = final_out[3] ^ alu_gt;
    assign out4 = final_out[4] ^ fsm_state[0];
    assign out5 = final_out[5] ^ fsm_state[1];
    assign out6 = final_out[6] ^ mode1;
    assign out7 = final_out[7] ^ mode2;

    assign pulse = fsm_pulse;
    assign state0 = fsm_state[0];
    assign state1 = fsm_state[1];
    assign flag_eq = alu_eq;
    assign flag_gt = alu_gt;
    assign flag_parity = alu_parity;
endmodule
