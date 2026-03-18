# TPL Macro Layer for ZIL

This document introduces a Temporal Process Language (TPL)-inspired macro layer
for ZIL.

Reference:
- https://en.wikipedia.org/wiki/Temporal_Process_Language

The layer is intentionally macro-based:

1. TPL constructs are represented as ZIL facts/rules.
2. Core ZIL semantics remain unchanged.
3. You can query and analyze TPL process models with normal ZIL tooling.

## Scope

This is a pragmatic modeling layer, not a full process-calculus proof engine.

Included concepts:

- action prefix (`a.P`)
- timeout (`floor(E)(F)` style)
- choice (`E + F`)
- parallel composition (`E | F`)
- recursion (`rec X.E`) and process variables (`X`)
- insistent operator (`Omega`)
- restriction (`E \\ a`)
- abstract time signal (`sigma`) and silent action (`tau`)
- maximal-progress style blocking (`tau` suppresses time progression)

## Macro Vocabulary

See runnable file:
- `examples/tpl-macro-layer.zc`

Primary constructors:

- `TPL_VISIBLE(name)`
- `TPL_TAU(name)`
- `TPL_SIGMA(name)`
- `TPL_PREFIX(proc, action, next_proc)`
- `TPL_TIMEOUT(proc, guard_proc, on_tick_proc)`
- `TPL_CHOICE(proc, left_proc, right_proc)`
- `TPL_PAR(proc, left_proc, right_proc)`
- `TPL_REC(proc, label, body_proc)`
- `TPL_VAR(proc, label)`
- `TPL_OMEGA(proc)`
- `TPL_RESTRICT(proc, inner_proc, action)`
- `TPL_NIL(proc)`

## Semantics Encoded as Rules

The example file includes derived-rule approximations for:

1. visible-prefix patience (`patient`, `can_sigma`)
2. `tau`-readiness and maximal-progress blocking (`blocks_tick`)
3. timeout tick-branch enablement when guard does not block time
4. choice/parallel propagation of `blocks_tick`, `can_tau`, and `can_sigma`
5. recursion and variable resolution via labels
6. restriction as structural inheritance

These are represented as explicit derived facts so behavior is inspectable.

## Runnable Usage

```bash
cd zil
./bin/zil examples/tpl-macro-layer.zc
```

Useful queries already included:

- `patient_processes`
- `sigma_enabled_processes`
- `blocked_processes`
- `timeout_tick_enabled`
- `parallel_tick_enabled`
- `recursive_var_resolutions`

## Design Notes

1. This layer treats TPL syntax forms as data (facts) plus derived properties.
2. It is suitable for architecture reasoning, traceability, and policy checks.
3. If you need full operational semantics equivalence, keep this as the
   authoring layer and attach a dedicated formal backend/proof pipeline.
