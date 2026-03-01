# DSL -> MinecraftHDL Macro Handoff

This file summarizes what the DSL/compiler pipeline should emit and how to validate it.

## Macro Contract Source

Use `markdown/PREFAB_MACRO_SPEC.md` as the behavior contract.

## Required Macro Names

- `mc_timer`
- `mc_periodic`
- `mc_latch`
- `mc_counter`
- `mc_seq_lock`
- `mc_station_fsm`

## Required Top-Level Constraints

- Keep non-macro logic combinational.
- Do not emit non-whitelisted sequential cells.
- Connect all required macro ports.
- Keep parameters compile-time constants within bounds already enforced by `GraphBuilder`.

## Enforced Macro IO Contract

MinecraftHDL now rejects malformed macro requests during prefab gate construction:

- `mc_timer`: `inputCount=3`, output must be `active[0]`.
- `mc_periodic`: `inputCount=3`, output must be `pulse[0]`.
- `mc_latch`: `inputCount=4`, output must be `q[0]`.
- `mc_counter`: `inputCount=4`, output must be `count[bit]` with `0 <= bit < WIDTH`.
- `mc_seq_lock`: `inputCount=3+BTN_COUNT`, outputs must be:
  - `unlocked[0]`, `correct_pulse[0]`, `wrong_pulse[0]`, or
  - `progress[bit]` with `0 <= bit < max(1, ceil(log2(SEQ_LEN+1)))`.
- `mc_station_fsm`: `inputCount=5`, output must be `occupied[0]` or `depart_now[0]`.

## Testing Path

Use the prefab simulation tests as acceptance references:

- `src/test/java/minecrafthdl/simulation/prefab/PrefabMacroSpecVectorsTest.java`
- `src/test/java/minecrafthdl/simulation/prefab/Example6AcceptanceTest.java`
- `src/test/java/minecrafthdl/simulation/prefab/PrefabMacroModelTest.java`

Run:

```bash
./gradlew test --tests minecrafthdl.simulation.prefab.PrefabMacroSpecVectorsTest --tests minecrafthdl.simulation.prefab.Example6AcceptanceTest
```
