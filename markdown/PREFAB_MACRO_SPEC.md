# Prefab Macro Spec (Cycle-Accurate Contract)

This document defines the canonical behavior contract for prefab macro support.
It is used by test vectors and simulator acceptance tests.

## Global Rules

- Sequential macro behavior uses an internal clock.
- Internal clock period: `2` game ticks.
- External Verilog `clk` port is kept for interface compatibility and signage but is ignored by prefab macro runtime semantics.
- Pulse outputs are one internal clock period wide.
- `rst` has highest priority over other macro inputs.
- `clear` is second-highest priority where present.

## Macro Contracts

## `mc_timer(TICKS)`

Inputs: `clk, rst, trigger_pulse`  
Outputs: `active`

- On each internal clock edge:
  - if `rst=1`: `active=0`, counter reset.
  - else if `trigger_pulse` rising edge: load counter to `TICKS`.
  - else if counter > 0: decrement.
- `active=1` iff counter > 0.

## `mc_periodic(PERIOD)`

Inputs: `clk, rst, enable`  
Outputs: `pulse`

- On each internal clock edge:
  - if `rst=1`: counter reset, `pulse=0`.
  - else if `enable=0`: `pulse=0`, counter holds.
  - else emit `pulse=1` every `PERIOD` edges, otherwise `pulse=0`.

## `mc_latch`

Inputs: `clk, rst, set_pulse, clear_pulse`  
Outputs: `q`

- On each internal clock edge:
  - if `rst=1` or `clear_pulse` rising edge: `q=0`.
  - else if `set_pulse` rising edge: `q=1`.
  - else hold state.

## `mc_counter(WIDTH)`

Inputs: `clk, rst, inc_pulse, clear_pulse`  
Outputs: `count[WIDTH-1:0]`

- On each internal clock edge:
  - if `rst=1` or `clear_pulse` rising edge: count reset to `0`.
  - else if `inc_pulse` rising edge: increment modulo `2^WIDTH`.

## `mc_seq_lock(BTN_COUNT, SEQ_LEN, EXPECT_IDX, LATCH_SUCCESS)`

Inputs: `clk, rst, clear, btn_pulse[BTN_COUNT-1:0]`  
Outputs: `unlocked, correct_pulse, wrong_pulse, progress`

- On each internal clock edge:
  - clear one-shot outputs first: `correct_pulse=0`, `wrong_pulse=0`.
  - if `LATCH_SUCCESS=0`, also clear `unlocked` unless success this edge.
  - if `rst=1` or `clear=1`: reset progress and unlock state.
  - decode rising edges from one-hot button vector:
    - no button edge: no transition.
    - multiple simultaneous edges: `wrong_pulse=1`, progress reset.
    - one edge:
      - if matches expected step: progress++, and on final step emit `correct_pulse=1`; set `unlocked=1`; reset progress.
      - else emit `wrong_pulse=1`; reset progress.

## `mc_station_fsm(DEPART_TICKS)`

Inputs: `clk, rst, clear, arrival_pulse, depart_pulse`  
Outputs: `occupied, depart_now`

- States: `EMPTY`, `OCCUPIED`, `DEPARTING`.
- On each internal clock edge:
  - if `rst=1` or `clear=1`: state=`EMPTY`, `depart_now=0`.
  - `EMPTY`: on rising `arrival_pulse` -> `OCCUPIED`.
  - `OCCUPIED`: on rising `depart_pulse` -> `DEPARTING` and assert `depart_now`.
  - `DEPARTING`: keep `depart_now=1` for `DEPART_TICKS`, then return `EMPTY`.
- `occupied=1` in `OCCUPIED` and `DEPARTING`, else `0`.
