# Navier-Stokes Physical-Closure Feedback Loop

This loop makes `examples/navier-stokes-physical-closure-gaps.zc` operational
for team progress tracking.

## Model split

- Base contract (stable semantics):
  - `examples/navier-stokes-physical-closure-gaps.zc`
- Progress overlay (mutable team state):
  - `examples/navier_stokes_physical_closure/lib/10-progress-macros.zc`
  - `examples/navier_stokes_physical_closure/lib/20-progress-current.zc`

The base file defines gaps, discharge steps, and feedback queries.
The overlay updates status/evidence without editing the base contract.

## Profiles

- `ns_progress_profile`
  - reporting mode
  - returns gap catalog, blockers, unresolved set, next steps, updates, adequacy
- `ns_release_gate_profile`
  - strict gate mode
  - requires `ready_for_strict_physical_close` to return at least one row

## One-shot command

```bash
cd zil
./tools/ns_physical_closure_feedback.sh
```

Outputs:

- preprocessed model snapshot
- `query_ci` report (`.edn`, stdout, stderr)
- exported JSON query artifacts
- summary file with counts and gate status

Default artifact folder:

- `/tmp/zil-ns-physical-closure-feedback`

## Useful variants

Progress mode without worklog write:

```bash
cd zil
./tools/ns_physical_closure_feedback.sh --no-worklog
```

Strict release gate:

```bash
cd zil
./tools/ns_physical_closure_feedback.sh --profile ns_release_gate_profile
```

## Team update workflow

1. Update only `20-progress-current.zc` at task boundaries.
2. Run feedback script.
3. Attach generated summary/artifacts in worklog or review.
4. Use `ns_release_gate_profile` before claiming strict physical closure.
