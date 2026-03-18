# Native Host + WASM/JS Screen Automation in ZIL

## Goal

Model the cross-platform screen automation architecture with ZIL so it can be:

1. reviewed as formal declarations (`HOST`, `SERVICE`, `DATASOURCE`, `LTS_ATOM`, `POLICY`),
2. checked with profile gates (`lts`, `constraint`),
3. evolved as a stable architecture artifact over time.

Related architecture narrative:

- `../docs/workstation/NATIVE_HOST_WASM_SCREEN_AUTOMATION_ARCHITECTURE.md`

## Canonical Example

- `examples/native-host-wasm-screen-automation.zc`

This example encodes:

1. native host + browser/WASM split,
2. capture/process/trigger/action service pipeline,
3. datasource surfaces for frame metadata, policy decisions, and action audits,
4. lifecycle behavior (`native_host_flow` as `LTS_ATOM`),
5. high-criticality safety policies (`no_subprocess_execution`, capability gating, loopback+token control API).

## Run It

Compile and inspect query outputs:

```bash
cd zil
./bin/zil examples/native-host-wasm-screen-automation.zc
```

Check behavior profile requirements:

```bash
cd zil
./bin/zil bundle-check examples/native-host-wasm-screen-automation.zc lts
```

Check policy/constraint profile requirements:

```bash
cd zil
./bin/zil bundle-check examples/native-host-wasm-screen-automation.zc constraint
```

## Modeling Guidance

1. Keep host adapters and plugin runtime as explicit `SERVICE` declarations.
2. Keep platform caveats as `POLICY` conditions (for example Wayland input support).
3. Keep runtime evidence streams (`audit`, `policy decisions`, `frame metadata`) as `DATASOURCE` declarations.
4. Keep safety behavior observable by modeling degraded/recovery and emergency-stop transitions in `LTS_ATOM`.

## Next Extension Targets

1. Add per-platform capability declarations (`windows`, `macos`, `linux_x11`, `linux_wayland`) as separate service/domain layers.
2. Add policy decomposition into static checks vs runtime checks.
3. Add macro-based domain library for repeated capture/action policy patterns.
