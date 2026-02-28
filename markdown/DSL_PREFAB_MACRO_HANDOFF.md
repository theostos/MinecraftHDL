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

## Testing Path

Use the prefab simulation tests as acceptance references:

- `src/test/java/minecrafthdl/simulation/prefab/PrefabMacroSpecVectorsTest.java`
- `src/test/java/minecrafthdl/simulation/prefab/Example6AcceptanceTest.java`

Run:

```bash
./gradlew test --tests minecrafthdl.simulation.prefab.PrefabMacroSpecVectorsTest --tests minecrafthdl.simulation.prefab.Example6AcceptanceTest
```
