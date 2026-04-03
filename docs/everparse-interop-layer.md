# EverParse Interop Layer

This layer models EverParse/3d validation workflows as ZIL facts and rules.

## Added artifacts

- `lib/everparse-interop-macros.zc`
- `libsets/everparse-interop/everparse-interop-macros.zc`
- `examples/everparse-interop-demo.zc`

## What is represented

- 3d specs and entrypoints (`ep:spec:*`)
- compile/run profile (`ep:profile:*`, `ep:run:*`)
- generated artifacts (`ep:artifact:*`)
- deterministic pos/neg test suites (`ep:test:*`)
- guarantees (memory/arithmetic/double-fetch) (`ep:guarantee:*`)
- evidence records linking run to theorem/contract references (`ep:evidence:*`)

## Contract reasoning

Rules derive:

- `contract_verified` when a run has:
  - succeeded status
  - generated artifacts
  - deterministic tests with positive and negative coverage
  - required guarantees proved
  - evidence attached
- explicit `contract_gap` facts when any requirement is missing.

## Demo run

```bash
cd zil
./bin/zil preprocess examples/everparse-interop-demo.zc /tmp/everparse_interop.pre.zc libsets/everparse-interop
./bin/zil /tmp/everparse_interop.pre.zc
```

Expected demo behavior:

- `run_tls_record_ci_001` appears in `ep_contract_verified_runs`
- `run_quic_ci_002` appears in `ep_contract_gaps` with missing negative tests, guarantees, and evidence.

