# Applying D-MetaVM to evmone (ZIL-First)

This note explains how to apply the D-MetaVM proposal to `evmone` by modeling
first in ZIL, then binding runtime components around evmone.

Primary model/spec artifacts:

- `spec/dmetavm-core-v0.1.md`
- `examples/evmone-dmetavm.zc`
- `formal/dmetavm/CoreMetaVM.tla`
- `formal/dmetavm/MetaInterpreter.tla`
- `formal/dmetavm/DistributedL3.tla`
- `tools/dmetavm_formal_ci.sh`

## 1. What "apply using evmone" means

`evmone` is a high-performance EVM execution engine. In this architecture:

- `evmone` remains the single-node execution kernel.
- D-MetaVM layers are modeled around it:
  - meta-interpreter registry/dispatch,
  - distributed message orchestration,
  - no-ledger/no-token profile constraints.

So evmone is the execution substrate, not the whole distributed fabric.

## 2. Concrete mapping from ZIL model to evmone code areas

| ZIL concept | evmone target |
| --- | --- |
| core execution step (`dmetavm_exec_step`) | `lib/evmone/baseline_execution.cpp`, `lib/evmone/advanced_execution.cpp` |
| pre-execution analysis | `lib/evmone/advanced_analysis.cpp`, `lib/evmone/baseline_analysis.cpp` |
| stack/memory/storage carrier | `lib/evmone/execution_state.hpp` |
| gas/budget-style charging | `docs/efficient_gas_calculation_algorithm.md`, instruction tables and traits |
| host call / external effect boundary | `lib/evmone/instructions_calls.cpp`, EVMC host interface |
| execution tracing / observability | `lib/evmone/tracing.hpp` |

## 3. How the three proposal modules map

### 3.1 CoreMetaVM.tla equivalent in ZIL

In `examples/evmone-dmetavm.zc`:

- `DMV_NODE(...)` encodes node-local machine assumptions.
- `DMV_CORE_STEP(...)` encodes pc/budget/opcode-class transitions.
- `dmetavm_core_flow` `LTS_ATOM` encodes execution lifecycle.

### 3.2 MetaInterpreter.tla equivalent in ZIL

In `examples/evmone-dmetavm.zc`:

- `DMV_INTERPRETER(...)` encodes interpreter identities.
- `DMV_REGISTER_INTERPRETER(...)` models registration.
- `DMV_MESSAGE(..., exec_remote, ...)` models interpreter execution requests.
- `dmetavm_meta_flow` `LTS_ATOM` encodes lifecycle.

### 3.3 DistributedL3.tla equivalent in ZIL

In `examples/evmone-dmetavm.zc`:

- `DMV_CHANNEL(...)` and `DMV_MESSAGE(...)` encode asynchronous node messaging.
- `parallel_safe_remote_pair` rule captures disjoint-storage parallelizability.
- `dmetavm_distributed_flow` `LTS_ATOM` and `dmetavm_fair_progress` policy encode progress assumptions.

## 4. Profile constraints that remove blockchain semantics

The model includes explicit flags:

- `token_native_enabled = false`
- `global_ledger_enabled = false`
- `pow_enabled = false`
- `mining_enabled = false`

And a hard policy:

- `POLICY dmetavm_no_blockchain_profile [...]`

This makes "blockchain-free" a checkable contract, not a comment.

## 5. Execution and formal checks

From `zil/`:

```bash
./bin/zil examples/evmone-dmetavm.zc
./bin/zil bundle-check examples/evmone-dmetavm.zc lts
./bin/zil bundle-check examples/evmone-dmetavm.zc constraint
./bin/zil export-tla examples/evmone-dmetavm.zc /tmp/dmetavm_evmone.tla DMetaVMEvmone
./bin/zil export-lean examples/evmone-dmetavm.zc /tmp/dmetavm_evmone.lean Zil.Generated.DMetaVMEvmone
```

## 6. Practical integration path around evmone

1. Keep evmone unmodified as the core interpreter backend initially.
2. Add a host-side orchestrator process that:
   - owns node/channel/message routing,
   - invokes evmone per node for local execution slices,
   - dispatches `exec_remote` requests to registered interpreters.
3. Bind ZIL model entities to orchestrator telemetry so model checks are driven
   by observed state, not only design-time declarations.
4. Use exported TLA+/Lean skeletons for safety/liveness refinement.

This keeps the architecture incremental while preserving the formal contract
surface defined in ZIL.
