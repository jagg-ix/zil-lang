# NEED/Provider Macro Layer

This macro layer captures dependency concepts often handled in adapters:

1. canonical NEED normalization,
2. acceptance/rejection criteria,
3. provider candidate/eligibility/selection flow,
4. evaluator boundary and proof/provenance facts,
5. policy checks (for example `exactly_one` missing selection).

## Files

- library module: `lib/needs-provider-macros.zc`
- runnable demo: `examples/needs-provider-macro-layer.zc`

## Why this layer exists

You asked for the concepts adapters carry, without building a full Dune
adapter. This layer gives those concepts directly in language-level constructs:

- `NP_NORMALIZE_*` captures canonicalization/provenance.
- `NP_CRITERION_*` captures acceptance criteria as first-class entities.
- `NP_MEETS_WITH_PROOF` captures evaluator/solver output plus evidence.
- `NP_SELECTED` captures external selection decision and reason.
- derived rules compute candidate/accepted/rejected/eligible/effective states.

## Current limitation

ZIL core currently has no cross-file `IMPORT`/`INCLUDE`.
So `lib/needs-provider-macros.zc` is a reusable module-by-convention, and
models typically vendor/copy the macros they use (as done in the demo).

You can also use the new preprocessor command to get import-like composition:

```bash
./bin/zil preprocess examples/needs-provider-macro-layer.zc /tmp/np.pre.zc
./bin/zil /tmp/np.pre.zc
```

## Minimal usage pattern

```zc
USE NP_NEED(n_lwt, pkg:lwt).
USE NP_NEED_ACCEPTS(n_lwt, lwt_ge_5_2).
USE NP_NEED_POLICY(n_lwt, exactly_one).

USE NP_PROVIDER(lwt_5_7, pkg:lwt, v5_7).
USE NP_MEETS_WITH_PROOF(lwt_5_7, lwt_ge_5_2, opam_solver, evidence:dune_lock).
USE NP_SELECTED(n_lwt, lwt_5_7, selection_engine, prefer_newest).
```

After rules run:

- `need:n_lwt#candidate@provider:lwt_5_7`
- `need:n_lwt#accepted_by@provider:lwt_5_7`
- `need:n_lwt#eligible_provider@provider:lwt_5_7`
- `need:n_lwt#effective_provider@provider:lwt_5_7`

## Run

```bash
cd zil
./bin/zil examples/needs-provider-macro-layer.zc
```

Useful queries in the demo:

- `candidates`
- `accepted`
- `rejected`
- `eligible`
- `effective`
- `policy_violations`
- `selection_warnings`
