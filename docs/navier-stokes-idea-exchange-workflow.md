# Navier-Stokes Idea Exchange Workflow (ZIL)

This workflow turns informal proof discussion into auditable model deltas that
multiple helpers can exchange through Git without losing semantics.

## Goal

Keep two views explicit at all times:

- formal closure (`status = proved` in Lean certificate chain)
- physical closure (no load-bearing reduced-carrier shim on critical path)

## Collaboration model

1. Each contributor encodes proposal units as theorem DSL facts:
- `ASSUMPTION` for accepted external/math premises.
- `LEMMA` for intermediate claims.
- `THEOREM` for closure targets.
- `EVIDENCE_FOR_LEMMA` / `EVIDENCE_FOR_THEOREM` for artifacts (`lean4`, `tla`, `z3`, notes).

2. Contradictions and regressions are encoded as incidents:
- `INCIDENT_BREAK_COMPONENT`
- `INCIDENT_BREAK_ASSUMPTION`
- `INCIDENT_LINK_THEOREM`

3. Exchange format is Git-native:
- one commit = one model update + evidence links.
- review compares status deltas (`PROVED/CONDITIONAL/BROKEN/WEAK`) instead of free text only.

## Minimal loop

1. Edit model: `examples/navier-stokes-idea-exchange.zc`.
2. Run pipeline:

```bash
cd zil
./bin/zil theorem-dsl-ci examples/navier-stokes-idea-exchange.zc /tmp/zil-ns-idea-ci
```

3. Inspect generated summary JSON (`operator_summary.status_counts`, `break_roots`, `impact_set`).
4. Post result + artifact path to worklog task/note.
5. Merge only when intended status transition is explicit.

## Recommended branch lanes

- `formal-lane`: theorem chain closure, no semantic reinterpretation.
- `physical-lane`: replace reduced-carrier shims with concrete observables/operators.
- `audit-lane`: maintain dual-view certificate and risk tags.

## Mapping to current NS targets

- Semantic hardening gate:
  - modeled as theorem `t_semantic_hardening_gate`.
- Physical observables concretization:
  - modeled as theorem `t_physical_observable_bridge`.
- PDE semantics concretization:
  - modeled as theorem `t_pde_semantics_concrete`.
- Dual-view re-audit:
  - modeled as theorem `t_dual_view_audit_honest`.

## Why this helps in shared workdir

- avoids silent meaning drift between helpers.
- keeps assumptions and blockers first-class.
- makes handoff machine-checkable through generated impact summaries.
