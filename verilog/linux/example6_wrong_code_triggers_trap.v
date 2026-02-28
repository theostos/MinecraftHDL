(* blackbox *)
module mc_seq_lock #(
  parameter integer BTN_COUNT     = 3,
  parameter integer SEQ_LEN       = 3,
  parameter integer LATCH_SUCCESS = 1,
  parameter integer EXPECT_IDX    = 0
) (
  input  wire clk,
  input  wire rst,
  input  wire clear,
  input  wire [BTN_COUNT-1:0] btn_pulse,
  output wire unlocked,
  output wire correct_pulse,
  output wire wrong_pulse,
  output wire [((SEQ_LEN<=1)?1:$clog2(SEQ_LEN+1))-1:0] progress
);
endmodule

module top(clk, rst, a_pressed, b_pressed, c_pressed, trap, reward);
  input wire clk;
  input wire rst;
  input wire a_pressed;
  input wire b_pressed;
  input wire c_pressed;
  output wire trap;
  output wire reward;
  wire __act_0;
  wire __act_1;
  wire __m_sequencelock_1_correct_pulse;
  wire [1:0] __m_sequencelock_1_progress;
  wire __m_sequencelock_1_unlocked;
  wire __m_sequencelock_1_wrong_pulse;
  wire [2:0] __w_u_sequencelock_1_btn_pulse;
  wire __w_u_sequencelock_1_clear;
  assign __w_u_sequencelock_1_clear = 1'b0;
  assign __w_u_sequencelock_1_btn_pulse = {c_pressed, b_pressed, a_pressed};
  assign __act_0 = __m_sequencelock_1_wrong_pulse;
  assign __act_1 = __m_sequencelock_1_unlocked;
  assign trap = (__act_0) & ~(1'b0);
  assign reward = (__act_1) & ~(1'b0);
  mc_seq_lock #(.BTN_COUNT(3), .SEQ_LEN(3), .LATCH_SUCCESS(1), .EXPECT_IDX(36)) u_sequencelock_1 (.clk(clk), .rst(rst), .clear(__w_u_sequencelock_1_clear), .btn_pulse(__w_u_sequencelock_1_btn_pulse), .unlocked(__m_sequencelock_1_unlocked), .correct_pulse(__m_sequencelock_1_correct_pulse), .wrong_pulse(__m_sequencelock_1_wrong_pulse), .progress(__m_sequencelock_1_progress));
endmodule
