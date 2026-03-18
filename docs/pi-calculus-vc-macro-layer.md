# Pi-Calculus + Vector Clock Macro Layer

This document adds a Pi-calculus-inspired macro DSL to ZIL and embeds vector
clock constructs directly in the same modeling surface.

Reference:
- https://en.wikipedia.org/wiki/%CE%A0-calculus

## Goal

Provide a practical authoring layer for:

1. process interaction patterns from Pi-calculus,
2. explicit causal metadata with vector clocks,
3. queryable partial-order and concurrency relations.

The layer is macro-based, so it preserves core ZIL semantics.

## Included Pi Constructs

The macro vocabulary in `examples/pi-calculus-vc-macro-layer.zc` covers:

- output prefix (`PI_SEND`)
- input prefix (`PI_RECV`)
- parallel composition (`PI_PAR`)
- choice (`PI_CHOICE`)
- restriction/new name (`PI_RESTRICT`)
- replication (`PI_REPLICATE`)
- recursion + variable (`PI_REC`, `PI_VAR`)
- nil process (`PI_NIL`)

Derived rules provide an abstract synchronization result:

- `pi_parallel_sync` marks `can_tau` when send/recv channels match in parallel,
- continuation links are exposed (`left_cont`, `right_cont`),
- wrappers propagate tau capability (choice/restrict/rec/var),
- replication exposes spawnability.

## Vector Clocks in the DSL

The same file includes vector-clock macros:

- `VC_ACTOR`
- `VC_COUNTER`
- `VC_LT`
- `VC_EVENT`
- `VC_COMPONENT`
- `VC_DISTINCT`

Partial-order rules:

1. transitive closure on counter order (`vc_lt_transitive`),
2. strict witness detection (`vc_strict_witness`),
3. violation detection (`vc_violation_over`),
4. happens-before derivation (`vc_before`),
5. concurrency derivation (`vc_concurrent`).

This allows vector clocks to be represented and queried as first-class DSL data.

## Runnable Example

Use:

- `examples/pi-calculus-vc-macro-layer.zc`

Run:

```bash
cd zil
./bin/zil examples/pi-calculus-vc-macro-layer.zc
```

Key queries in the example:

- `pi_tau_enabled`
- `pi_sync_payloads`
- `pi_spawnable`
- `vc_before_edges`
- `vc_concurrency_from_sync_x`
- `vc_event_pi_links`

## Notes on Fidelity

This layer is intentionally model-oriented and does not claim full operational
equivalence with all standard Pi-calculus semantics (for example full
substitution/scope extrusion machinery). It is designed to be:

1. executable in current ZIL,
2. inspectable via facts/rules,
3. extensible toward stricter formal backends.
