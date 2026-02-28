# Getting Started (Forge 1.20.1, Linux)

## 1. Prerequisites

- Minecraft Java Edition
- `yosys`
- Internet access for Gradle dependency/toolchain downloads

Optional but recommended:
- Java 17 JDK installed locally

## 2. Run MinecraftHDL in dev mode

From repository root:

```bash
./gradlew runClient
```

This launches Minecraft with the mod loaded.

## 3. Synthesize a Verilog file to JSON

```bash
cd verilog/linux
./synth.sh /absolute/path/to/your_design.v
```

Output file:
- `your_design.v.json`

## 4. Load synthesized designs in Minecraft

1. Create directory if it does not exist:
   - `run/verilog_designs`
2. Copy your generated JSON file into that directory.
3. In-game, place a **Synthesizer** block.
4. Right-click it and choose your JSON file.
5. Power the block with redstone to generate the circuit.

## 5. Build a distributable jar

```bash
./gradlew build
```

Produced jars are under `build/libs/`.

## 6. Prefab Macro Config (Optional)

When running in dev mode, Forge common config is generated under:

- `run/config/minecrafthdl-common.toml`

Macro-related keys:

- `prefabMacros.enabled=false`
- `prefabMacros.autoClockPeriodTicks=2`
- `prefabMacros.totalBlockBudget=10000`
- `prefabMacros.perInstanceBlockBudget=2000`

Macro behavior contract is defined in:

- `markdown/PREFAB_MACRO_SPEC.md`
