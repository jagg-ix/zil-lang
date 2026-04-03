# BackendCapabilities + FormalFeedbackSchema Contract

This contract defines one concrete ZIL vocabulary for:

1. backend capability declarations (`BackendCapabilities`),
2. formal-tool run feedback (`FormalFeedbackSchema`),
3. deterministic status rollups for obligations and runs.

Files:

- `lib/backend-formal-contract-macros.zc`
- `examples/backend-formal-contract-demo.zc`

## Goal

Minimize ambiguity in the loop:

1. declare backend engines/capabilities/tool bridges,
2. declare obligations that require capabilities and route to formal tools,
3. ingest tool feedback,
4. derive operator-facing status classes:
   - `PROVED`
   - `CONDITIONAL`
   - `BROKEN`
   - `WEAK`
   - `NO_FEEDBACK`

## Schema Vocabulary

Backend declarations:

- `BFC_BACKEND`, `BFC_BACKEND_VERSION`, `BFC_BACKEND_ENABLED`
- `BFC_BACKEND_CAP`, `BFC_BACKEND_IO`, `BFC_BACKEND_FORMAL_BRIDGE`, `BFC_BACKEND_COST`

Execution context:

- `BFC_RUN`
- `BFC_ARTIFACT`, `BFC_ARTIFACT_PRODUCER`, `BFC_ARTIFACT_CONSUMER`
- `BFC_RUN_INPUT_ARTIFACT`, `BFC_RUN_OUTPUT_ARTIFACT`

Formal obligations + feedback:

- `BFC_OBLIGATION`, `BFC_OBLIGATION_RUN`
- `BFC_OBLIGATION_REQUIRES_CAP`, `BFC_OBLIGATION_TARGET_TOOL`, `BFC_OBLIGATION_ARTIFACT`
- `BFC_FEEDBACK`, `BFC_FEEDBACK_METRICS`

## Enumerations (Validated)

Backend engines:

- `clj`, `datafrog`, `souffle`

Tools:

- `tla`, `lean4`, `z3`, `acl2`

Obligation classes:

- `theorem`, `policy`, `safety`, `liveness`, `consistency`

Feedback verdicts:

- `satisfied`, `violated`, `unknown`, `timeout`, `error`

Capabilities:

- `fixpoint`, `stratified_negation`, `anti_join`, `incremental`
- `emit_tla`, `emit_lean4`, `emit_acl2`, `check_z3`, `check_acl2`
- `ingest_feedback`, `artifact_json`, `artifact_yaml`, `artifact_csv`

Artifact formats:

- `json`, `yaml`, `yml`, `csv`, `edn`, `text`

Unknown values are preserved as facts and also flagged through `contract_error`.

## Routing and Coverage

Default routed tool by obligation class:

- `policy -> z3`
- `theorem -> lean4`
- `safety -> tla`
- `liveness -> tla`
- `consistency -> z3`

`BFC_OBLIGATION_TARGET_TOOL` overrides defaults.

Coverage checks:

1. backend must expose a matching tool bridge for routed tool,
2. backend must provide each required capability.

Violations are emitted as obligation-level contract errors:

- `missing_tool_bridge`
- `missing_backend_capability`

## Status Semantics

Obligation status precedence:

1. `BROKEN` if violated feedback exists or obligation has contract errors.
2. `PROVED` if satisfied feedback exists and no broken/unknown/timeout/error flags.
3. `CONDITIONAL` if satisfied exists and also unknown/timeout/error exists.
4. `WEAK` if no satisfied feedback and unknown/timeout/error exists.
5. `NO_FEEDBACK` if no feedback exists.

Run status precedence:

1. `BROKEN`
2. `CONDITIONAL`
3. `WEAK`
4. `NO_FEEDBACK`
5. `PROVED`
6. `EMPTY` (no obligations attached)

## Contract Error Surface

The layer emits first-class contract errors for:

- unknown backend engine/capability,
- unknown artifact format,
- unknown feedback tool/verdict,
- invalid feedback references (run/obligation),
- unknown obligation class,
- missing tool bridge,
- missing backend capability.

## Operator Queries

Primary query set exposed by the library:

- `bfc_backend_capabilities`
- `bfc_backend_contract_errors`
- `bfc_obligation_contract_errors`
- `bfc_feedback_contract_errors`
- `bfc_missing_capabilities`
- `bfc_missing_tool_bridges`
- `bfc_feedback_matrix`
- `bfc_obligation_status`
- `bfc_run_status`

## Run Demo

```bash
cd zil
./bin/zil preprocess examples/backend-formal-contract-demo.zc /tmp/bfc.pre.zc
./bin/zil /tmp/bfc.pre.zc
```

Demo-focused result queries:

- `demo_backend_contract_errors`
- `demo_artifact_contract_errors`
- `demo_obligation_status`
- `demo_obligation_contract_errors`
- `demo_feedback_contract_errors`
- `demo_run_status`
