# Adaptive Query Method (DSL-Aware)

ZIL now supports a first step toward DSL-aware query planning via two new
declaration kinds:

- `QUERY_PACK`
- `DSL_PROFILE`

## Why

Different DSL layers have different query patterns and verification needs.
This layer lets models declare:

- which query pack they use,
- which planner hint they prefer,
- which verification chain they expect.

## New Declarations

```zc
QUERY_PACK ops_pack [queries=[q_hot_path q_risk]].
DSL_PROFILE ops_profile [query_pack=ops_pack, planner_hint=high_selectivity_first, verification_chain=[constraint proof_obligation], default_profile=constraint].
```

You can also add query-level CI expectations:

```zc
QUERY_PACK ops_pack [queries=[q_hot_path q_risk], must_return=[q_hot_path]].
```

Validation guarantees:

- `QUERY_PACK` must define at least one query name.
- `QUERY_PACK.must_return` entries must be a subset of `queries`.
- `DSL_PROFILE.query_pack` must reference an existing `QUERY_PACK`.

## Planner behavior

Current planner hint values:

- `high_selectivity_first` (default)
- `bound_first`
- `as_is`

Execution behavior:

- positive literals are reordered using the selected hint,
- negative literals remain at the end (stratified-negation shape is preserved).

## CLI

New command:

```bash
./bin/zil query-plan <model.zc> [output.edn] [lib_dir]
./bin/zil query-ci <model.zc> [output.edn] [lib_dir] [dsl_profile]
```

`query-plan` returns:

- active DSL profiles,
- relation cardinalities,
- original and planned relation order per query.

`query-ci` executes selected query packs and verifies `must_return` checks.
If any required query is empty, it exits with code `1`.

`theorem-ci` and `vstack-ci` now run an integrated `query-ci` stage on the
input model path before formal export checks. Their reports include
`checks.query_ci`.

## Auto profile inference in bundle/commit checks

`bundle-check` and `commit-check` now accept `profile=auto`.
When `auto` is used, profile inference uses declaration evidence (including
`DSL_PROFILE.verification_chain`) and falls back to `tm.det`.

## Demo

- `examples/query-planner-adaptive.zc`
- `examples/query-ci-adaptive.zc`
