# Theorem DSL CI Workflow

This workflow provides an operator-facing theorem tool where model authors use
macro-based DSL commands and the pipeline emits formal artifacts plus summary JSON.

## DSL Layer

High-level macros are defined in:

- `lib/theorem-dsl-macros.zc`

Core theorem semantics still come from:

- `lib/theorem-impact-macros.zc`

## One-Shot Command

```bash
cd zil
./bin/zil theorem-dsl-ci examples/theorem-dsl-incident.zc /tmp
```

Optional arguments:

```bash
./bin/zil theorem-dsl-ci <model.zc> [out_dir] [bridge_module] [tla_module] [lean_namespace] [summary_json]
```

## Internal Stages

1. preprocess model (`lib/*.zc` included),
2. execute preprocessed model and compute operator summary,
3. generate theorem bridge (`LTS_ATOM` + `POLICY`),
4. run `bundle-check` for `lts` and `constraint`,
5. export TLA and Lean,
6. write summary JSON.

## Operator Summary Payload

`theorem-dsl-ci` report includes:

- `status_counts` for `PROVED|CONDITIONAL|BROKEN|WEAK`,
- `statuses` rows per theorem,
- `missing_dependencies`,
- `break_roots`,
- `impact_set`.

These are saved into `*.dsl.summary.json` unless an explicit `summary_json` path
is provided.
