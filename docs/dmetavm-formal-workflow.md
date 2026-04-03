# D-MetaVM Formal Workflow (ZIL -> TLA+ / Lean4)

This is the concrete workflow for the evmone-oriented D-MetaVM model.

Primary artifacts:

- ZIL model: `examples/evmone-dmetavm.zc`
- ZIL spec: `spec/dmetavm-core-v0.1.md`
- TLA modules:
  - `formal/dmetavm/CoreMetaVM.tla`
  - `formal/dmetavm/MetaInterpreter.tla`
  - `formal/dmetavm/DistributedL3.tla`
- TLC configs:
  - `formal/dmetavm/CoreMetaVM.cfg`
  - `formal/dmetavm/MetaInterpreter.cfg`
  - `formal/dmetavm/DistributedL3.cfg`

## 1. ZIL gates + exports

```bash
cd zil
./bin/zil bundle-check examples/evmone-dmetavm.zc lts
./bin/zil bundle-check examples/evmone-dmetavm.zc constraint
./bin/zil export-tla examples/evmone-dmetavm.zc /tmp/dmetavm_evmone.tla DMetaVMEvmone
./bin/zil export-lean examples/evmone-dmetavm.zc /tmp/dmetavm_evmone.lean Zil.Generated.DMetaVMEvmone
```

## 2. One-shot wrapper

```bash
cd zil
./tools/dmetavm_formal_ci.sh
```

Optional TLC inside wrapper:

```bash
TLA2TOOLS_JAR=/abs/path/tla2tools.jar ./tools/dmetavm_formal_ci.sh
```

Notes:

- The wrapper auto-detects `../tools/verification/vendor/tla2tools.jar` if present.
- If TLC cannot run (for example in restricted sandbox environments), the wrapper
  falls back to a SANY parse/semantic pass for `DistributedL3.tla`.

## 3. Direct TLC runs on hand-written TLA modules

```bash
java -jar /path/to/tla2tools.jar -config formal/dmetavm/CoreMetaVM.cfg formal/dmetavm/CoreMetaVM.tla
java -jar /path/to/tla2tools.jar -config formal/dmetavm/MetaInterpreter.cfg formal/dmetavm/MetaInterpreter.tla
java -jar /path/to/tla2tools.jar -config formal/dmetavm/DistributedL3.cfg formal/dmetavm/DistributedL3.tla
```

## 4. Role split

- ZIL file (`examples/evmone-dmetavm.zc`):
  - canonical authoring surface,
  - LTS/POLICY gating source,
  - exporter source for generated TLA/Lean skeletons.
- Hand-written TLA modules (`formal/dmetavm/*.tla`):
  - explicit algorithm-focused specifications aligned to the proposal.
- `evmone` codebase:
  - execution substrate for core-step semantics.

## 5. Expected invariants/properties

- Core:
  - `TypeOK`
  - `BudgetNonNegative`
  - `PCBounds`
  - `NoBlockchainProfile`
- Meta:
  - `MetaTypeOK`
  - `ExecInterpreterAllowed`
- Distributed:
  - `TypeOK`
  - `BudgetNonNegative`
  - `NoBlockchainProfile`
  - `FairProgress` (configured as a property in `DistributedL3.cfg`)

## 6. TLC profile note

`formal/dmetavm/DistributedL3.cfg` is intentionally a bounded quick-check
profile (`Nodes=2`, single interpreter/payload, small queue/log caps) so TLC
finishes fast in CI and local iteration.

For deeper exploration, increase these constants in the cfg:

- `Nodes`
- `Interpreters`
- `Payloads`
- `MaxBudget`
- `MaxChannelDepth`
- `MaxCompleted`
