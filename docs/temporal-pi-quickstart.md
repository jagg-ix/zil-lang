# ZIL Temporal + Pi Calculus Quickstart

This guide gives direct samples for using ZIL with:

1. temporal process modeling (TPL-inspired),
2. Pi-calculus process modeling,
3. vector-clock causal ordering.

## Samples Included

1. Quick combined sample:
- `examples/temporal-pi-quickstart.zc`

2. Full temporal layer:
- `examples/tpl-macro-layer.zc`
- `docs/tpl-macro-layer.md`

3. Full Pi + vector-clock layer:
- `examples/pi-calculus-vc-macro-layer.zc`
- `docs/pi-calculus-vc-macro-layer.md`

## How to Run

From `zil/`:

```bash
./bin/zil examples/temporal-pi-quickstart.zc
./bin/zil examples/tpl-macro-layer.zc
./bin/zil examples/pi-calculus-vc-macro-layer.zc
```

## What to Look For

In `temporal-pi-quickstart.zc`, query results should include:

1. `temporal_sigma_enabled`:
- `tpl:proc:tp_ok`

2. `temporal_tick_blockers`:
- `tpl:proc:tp_block`

3. `pi_tau_enabled`:
- `pi:proc:pp`

4. `vc_before_edges`:
- `vc:event:e2`

## Typical Workflow

1. Start with `temporal-pi-quickstart.zc`.
2. Edit one constructor call at a time (`TPL_PREFIX`, `PI_SEND`, `VC_COMPONENT`).
3. Re-run `./bin/zil ...` and inspect query output.
4. Move to full examples when you need richer semantics (timeouts, recursion, restriction, replication, concurrency checks).

## Extending Your Model

1. Add more process nodes with macro calls, not raw facts.
2. Keep all temporal/pi constructs in macros and rules so core ZIL stays stable.
3. Add domain-specific queries for the properties you care about:
- deadlock-like blockers,
- synchronization opportunities,
- causal precedence and concurrency.
