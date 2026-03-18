# TLM Macro Layer for ZIL

This document adds a transaction-level modeling (TLM) domain library using ZIL's
native macro system.

The design is language-level and implementation-agnostic: macros lower into
normal tuple facts/rules, so core semantics do not change.

## Concept mapping

TLM concept to ZIL macro mapping:

- Computation:
  - `TLM_COMPUTE(step, component, operation)`
  - emits `phase=compute` facts local to a component.
- Communication:
  - `TLM_SEND(tx, src, channel)` and `TLM_ACCEPT(tx, dst)`
  - emits `phase=communication`, status transitions, and endpoint links.
- Transaction object:
  - `TLM_TRANSACTION(...)`
  - encapsulates command, address, payload, size, priority, timing level.
- Abstract channels/interfaces:
  - `TLM_CHANNEL(...)`, `TLM_BIND(...)`, and composed `TLM_LINK(...)`.
- Timing abstraction spectrum:
  - `TLM_TIMING_PROFILE(name, level, quantum_ns)`
  - supports untimed, loosely timed, approximately timed, and PV tags.

## Why this fits ZIL architecture

1. Preserves core tuple/rule semantics.
2. Keeps TLM as a domain layer, not a hardcoded core feature.
3. Supports refinement by adding extra facts/rules without changing macro APIs.
4. Remains compatible with downstream checks/exporters because lowered output is canonical.

## Runnable model

Use:

- `examples/tlm-domain-macros.zc`
- `examples/sshx11-tlm-catalog.zc` (catalog-scale macro composition)

Run:

```bash
cd zil
./bin/zil examples/tlm-domain-macros.zc
./bin/zil examples/sshx11-tlm-catalog.zc
```

This example is macro-domain focused and does not include `LTS_ATOM` or
`POLICY` declarations, so profile checks are intentionally not required here.

The SSHX11 catalog file demonstrates higher-level macro bundling:

- `SSHX11_COMPONENTS()`
- `SSHX11_TIMING_PROFILES()`
- `SSHX11_CHANNELS()`
- `SSHX11_CATALOG_TX20()` (instantiates the 20 transactions)
- `SSHX11_FORMAL_LTS()`
- `SSHX11_POLICIES()`

## Formal backend overlay (Z3 / TLA+ / Lean4)

Use the paired formal overlay file:

- `examples/tlm-formal-bridge.zc`

This file adds:

- `LTS_ATOM` declarations for transaction/channel/endpoint actor flows.
- `POLICY` declarations for SMT-backed constraint checks.

Run:

```bash
cd zil
./bin/zil bundle-check examples/tlm-formal-bridge.zc lts
./bin/zil bundle-check examples/tlm-formal-bridge.zc constraint
./bin/zil export-tla examples/tlm-formal-bridge.zc /tmp/tlm_bridge.tla TLMBridgeFromZil
./bin/zil export-lean examples/tlm-formal-bridge.zc /tmp/tlm_bridge.lean Zil.Generated.TLM
```

One-shot wrapper (CI-friendly):

```bash
cd zil
./tools/tlm_formal_ci.sh
```

Recommended pattern:

1. Keep domain communication/computation semantics in `tlm-domain-macros.zc`.
2. Keep formal actor/invariant overlays in `tlm-formal-bridge.zc`.
3. Evolve both together using stable actor/event naming.
