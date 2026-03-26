# Zil Lang

Zil Lang (short name: Zil) is a declarative tuple-and-rule language for general knowledge modeling,
verification, and policy reasoning.

This repository now includes both:
- language design artifacts (normative specs), and
- an initial Clojure + DataScript runtime scaffold that executes core ideas.

## Scope and Architecture

ZIL core is domain-agnostic and general-purpose.

Current built-in declarations are IT-oriented because they are the primary
immediate use case in this repository, but they are layered above the core
tuple/rule semantics and do not define language boundaries.

The declaration layer includes an explicit external `PROVIDER` concept so
models can reference external systems/adapters (for example OpenTofu/HCL
providers) without hardwiring provider semantics into the core language.

Architecture guide:

- `docs/language-architecture.md`
- `docs/language-design.md`
- `docs/tooling-workflows.md`
- `docs/project-import-patterns.md`
- `docs/provider-model-hcl-import.md`
- `docs/json-yaml-csv-interop-layer.md`
- `docs/config-declarative-macro-layer.md`
- `docs/k8s-helm-compat-automation.md`
- `docs/aws-overview-modeling-workflow.md`
- `docs/aws-model-extension-and-icons.md`
- `docs/aws-namespace-integration.md`
- `docs/nvidia-aie-glossary-workflow.md`
- `docs/everparse-interop-layer.md`
- `docs/everparse-paper-automation.md`
- `docs/backend-formal-contract.md`
- `docs/acl2-integration.md`
- `docs/needs-provider-macro-layer.md`
- `docs/request-form-modeling.md`
- `docs/rbac-dac-macro-layer.md`
- `docs/bitemporal-dsl-layer.md`
- `docs/namespace-pn-nd-extension.md`
- `docs/theorem-impact-macro-layer.md`
- `docs/theorem-dsl-ci-workflow.md`
- `docs/evmone-dmetavm-application.md`
- `docs/dmetavm-formal-workflow.md`
- `docs/vscode-wolfram-reuse-for-zil.md`
- `spec/zil-v0.1r1.md`
- `spec/dmetavm-core-v0.1.md`

## Repository Goals

1. Define a stable core language with formal semantics.
2. Separate core semantics from optional profiles (e.g., Zanzibar compatibility).
3. Keep implementation-language concerns out of core language design.
4. Provide worked examples for IT infrastructure, dependencies, failover, and drift detection.
5. Provide one concrete Datalog-first execution path to validate semantics in practice.

## Structure

- `spec/` normative language specs
- `profiles/` optional profile specs (e.g., zanzibar)
- `docs/` explanatory guides and rationale
- `lib/` reusable macro-library modules
- `examples/` illustrative models and queries
- `projects/` domain-specific implementation packs (kept separate from core)
- `notes/` open issues and design decisions
- `src/zil/runtime/` executable runtime scaffolds

## DataScript Leverage

We leverage the Clojure DataScript engine as a concrete runtime target:
- immutable in-memory DB values for snapshot-friendly semantics,
- Datalog query/rule execution as the primary evaluation model,
- tuple attributes and unique constraints for composite identity and integrity,
- direct support for recursive rules and stratified negation patterns.

See:
- `spec/runtime-datascript-profile-v0.1.md`
- `docs/datascript-leverage.md`
- `src/zil/runtime/datascript.clj`

## Quick Start (Clojure)

```bash
clojure -M -e "(require 'zil.runtime.datascript) (println :ok)"
```

Core engine load:

```bash
clojure -M -e "(require 'zil.core) (println :ok)"
```

Minimal usage:

```clj
(require '[zil.runtime.datascript :as zr])

(def conn (zr/make-conn))

(zr/transact-facts! conn
  [{:object "app:svc1"
    :relation :depends_on
    :subject "service:db1"
    :revision 1
    :event :e1}
   {:object "service:db1"
    :relation :available
    :subject "value:true"
    :revision 1
    :event :e1}])

(zr/facts-at-or-before @conn 1)
```

Run a `.zc` file through the core engine:

```bash
clojure -M -m zil.cli examples/it-infra-minimal.zc
```

Import HCL/OpenTofu descriptions into ZIL:

```bash
clojure -M -m zil.cli import-hcl path/to/infra/ [output.zc] [module_name]
./bin/zil import-hcl path/to/infra/ /tmp/infra-imported.zc hcl.import.infra
```

Import external JSON/YAML/CSV into generated ZIL facts:

```bash
./bin/zil import-data examples/data/interop-sample.json /tmp/interop_from_json.zc interop.import json
./bin/zil import-data examples/data/interop-sample.yaml /tmp/interop_from_yaml.zc interop.import yaml
./bin/zil import-data examples/data/interop-sample.csv /tmp/interop_from_csv.zc interop.import csv
```

Export model outputs in JSON/YAML/CSV:

```bash
./bin/zil export-data examples/interop-json-yaml-csv.zc json /tmp/zil_queries.json queries
./bin/zil export-data examples/interop-json-yaml-csv.zc yaml /tmp/zil_queries.yaml queries
./bin/zil export-data examples/interop-json-yaml-csv.zc csv /tmp/service_states.csv service_states
```

Generate Kubernetes/Helm compatibility macro layer + runnable example automatically:

```bash
python3 tools/generate_k8s_helm_compat.py
./bin/zil preprocess examples/k8s-helm-compat.zc /tmp/k8s_helm_compat.pre.zc libsets/k8s-helm-compat
./bin/zil /tmp/k8s_helm_compat.pre.zc
./tools/k8s_helm_compat_smoke.sh
```

Extract AWS whitepaper model inputs and run AWS compatibility smoke:

```bash
python3 tools/extract_aws_overview_model_inputs.py
./bin/zil examples/generated/aws-overview-model-inputs.zc
./tools/aws_overview_compat_smoke.sh
./tools/aws_extension_icons_smoke.sh
```

Extract NVIDIA AI Enterprise glossary inputs (equivalent glossary model flow):

```bash
python3 tools/extract_nvidia_aie_glossary_inputs.py
./bin/zil examples/generated/nvidia-aie-glossary-inputs.zc
./tools/nvidia_aie_glossary_smoke.sh
```

EverParse/3d interop demo (specs/artifacts/guarantees/tests as ZIL facts):

```bash
./bin/zil preprocess examples/everparse-interop-demo.zc /tmp/everparse_interop.pre.zc libsets/everparse-interop
./bin/zil /tmp/everparse_interop.pre.zc
```

Generate a paper-wide EverParse extension scaffold from arXiv:2505.17335v2:

```bash
python3 tools/generate_everparse_2505_17335_extension.py --pdf ~/Downloads/2505.17335v2.pdf
./bin/zil preprocess examples/generated/everparse-2505-17335-extension.zc /tmp/everparse_2505_17335.pre.zc libsets/everparse-interop
./bin/zil /tmp/everparse_2505_17335.pre.zc
```

Preprocess a model with automatic `lib/*.zc` concatenation:

```bash
./bin/zil preprocess models/system.zc /tmp/system.pre.zc
./bin/zil /tmp/system.pre.zc
```

Run imported domain project models (example: FTS migration pack):

```bash
./tools/project_import_fts_sync.sh
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-generic.zc
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-tlm.zc
```

RBAC/DAC macro-layer demo:

```bash
./bin/zil preprocess examples/rbac-dac-orange-book.zc /tmp/rbd.pre.zc
./bin/zil /tmp/rbd.pre.zc
```

Namespace + PetriNet + N-dimensional extension demo:

```bash
./bin/zil preprocess examples/namespace-pn-nd-extension.zc /tmp/namespace_pn_nd.pre.zc libsets/namespace-pn-nd
./bin/zil /tmp/namespace_pn_nd.pre.zc
./tools/namespace_pn_nd_smoke.sh
```

AWS + namespace scoped compatibility demo:

```bash
./bin/zil preprocess examples/aws-namespace-compat.zc /tmp/aws_ns.pre.zc libsets/aws-namespace
./bin/zil /tmp/aws_ns.pre.zc
./tools/aws_namespace_smoke.sh
```

Theorem/lemma status + backward breakage demo:

```bash
./bin/zil preprocess examples/theorem-impact-devops-sre.zc /tmp/theorem_impact.pre.zc
./bin/zil /tmp/theorem_impact.pre.zc
```

Bitemporal history + correction + replay demo:

```bash
./bin/zil preprocess examples/bitemporal-history-replay.zc /tmp/bt.pre.zc libsets/bitemporal
./bin/zil /tmp/bt.pre.zc
```

Integrated V-Stack CI (theorem + refinement relation layer):

```bash
./bin/zil vstack-ci examples/vstack-refinement-minimal.zc /tmp/vstack-ci
./bin/zil vstack-ci examples/vstack-refinement-minimal.zc /tmp/vstack-ci - - - /tmp/vstack.summary.json
```

`vstack-ci` now writes a machine-readable summary JSON artifact by default at
`<out-dir>/<input-stem>.vstack.summary.json`.
It also emits a BackendCapabilities/FormalFeedbackSchema-compatible sidecar at
`<out-dir>/<input-stem>.vstack.feedback.zc`.

## Standalone Runtime (No Clojure CLI Needed)

Build once (requires Clojure tooling on build machine):

```bash
cd zil
./bin/build-jar
```

Run anywhere with Java only:

```bash
cd zil
./bin/zil examples/it-infra-minimal.zc
./bin/zil bundle-check examples lts
./bin/zil export-tla examples/sshx11-vpn-system.zc /tmp/sshx11_bridge.tla SSHX11BridgeFromZil
java -jar dist/zil-standalone.jar bundle-check examples/quickstart-sshx11-beginner.zc lts
```

Notes:

- `./bin/zil` prefers `dist/zil-standalone.jar` when present.
- if jar is missing, it falls back to `clojure -M -m zil.cli`.
- if neither Java+jar nor Clojure is available, it prints a build hint.

SSHX11 + VS Code extension-host modeling example:

```bash
./bin/zil examples/sshx11-extension-vscode.zc
./bin/zil bundle-check examples/sshx11-extension-vscode.zc lts
./bin/zil bundle-check examples/sshx11-extension-vscode.zc constraint
```

Native host + WASM/JS screen automation modeling example:

```bash
./bin/zil examples/native-host-wasm-screen-automation.zc
./bin/zil bundle-check examples/native-host-wasm-screen-automation.zc lts
./bin/zil bundle-check examples/native-host-wasm-screen-automation.zc constraint
```

Declarative config macro-layer example:

```bash
./bin/zil preprocess examples/config-declarative-macros.zc /tmp/config.pre.zc libsets/config-declarative
./bin/zil /tmp/config.pre.zc
```

Transaction-level modeling (TLM) macro-layer example:

```bash
./bin/zil examples/tlm-domain-macros.zc
```

TLM formal backend bridge example (Z3/TLA+/Lean4):

```bash
./bin/zil bundle-check examples/tlm-formal-bridge.zc lts
./bin/zil bundle-check examples/tlm-formal-bridge.zc constraint
./bin/zil export-tla examples/tlm-formal-bridge.zc /tmp/tlm_bridge.tla TLMBridgeFromZil
./bin/zil export-lean examples/tlm-formal-bridge.zc /tmp/tlm_bridge.lean Zil.Generated.TLM
```

D-MetaVM (evmone-oriented) model example:

```bash
./bin/zil examples/evmone-dmetavm.zc
./bin/zil bundle-check examples/evmone-dmetavm.zc lts
./bin/zil bundle-check examples/evmone-dmetavm.zc constraint
./bin/zil export-tla examples/evmone-dmetavm.zc /tmp/dmetavm_evmone.tla DMetaVMEvmone
./bin/zil export-lean examples/evmone-dmetavm.zc /tmp/dmetavm_evmone.lean Zil.Generated.DMetaVMEvmone
```

D-MetaVM one-shot formal pipeline wrapper:

```bash
./tools/dmetavm_formal_ci.sh
```

One-shot CI wrapper for the same chain:

```bash
./tools/tlm_formal_ci.sh
```

## Native Macro System

Zil has its own language-level macro system (independent of Clojure macros):

```zc
MACRO link_pair(a,b):
EMIT {{a}}#connected_to@{{b}}.
EMIT {{b}}#connected_to@{{a}}.
ENDMACRO.

USE link_pair(location:dcA, location:dcB).
```

Rules:
- define with `MACRO name(params): ... ENDMACRO.`
- each body line is `EMIT ...`
- invoke with `USE name(args).`
- placeholders are `{{param}}`

Domain library layer examples using macros:

- `docs/domain-library-macros.md`
- `examples/domain-library-macros.zc`
- `docs/tlm-macro-layer.md`
- `lib/tlm-macros.zc`
- `examples/tlm-domain-macros.zc`
- `examples/tlm-formal-bridge.zc`
- `examples/vstack-refinement-minimal.zc`
- `docs/tpl-macro-layer.md`
- `examples/tpl-macro-layer.zc`
- `docs/pi-calculus-vc-macro-layer.md`
- `examples/pi-calculus-vc-macro-layer.zc`
- `docs/temporal-pi-quickstart.md`
- `examples/temporal-pi-quickstart.zc`
- `docs/petrinet-nd-macro-layer.md`
- `examples/petrinet-nd-macro-layer.zc`
- `docs/provider-model-hcl-import.md`
- `docs/json-yaml-csv-interop-layer.md`
- `lib/interop-macros.zc`
- `examples/interop-json-yaml-csv.zc`
- `docs/config-declarative-macro-layer.md`
- `lib/config-declarative-macros.zc`
- `libsets/config-declarative/config-declarative-macros.zc`
- `examples/config-declarative-macros.zc`
- `docs/backend-formal-contract.md`
- `lib/backend-formal-contract-macros.zc`
- `lib/acl2-interop-macros.zc`
- `examples/backend-formal-contract-demo.zc`
- `examples/acl2-proof-obligation-log.zc`
- `examples/provider-external-minimal.zc`
- `docs/needs-provider-macro-layer.md`
- `lib/needs-provider-macros.zc`
- `examples/needs-provider-macro-layer.zc`
- `docs/request-form-modeling.md`
- `lib/request-form-macros.zc`
- `docs/rbac-dac-macro-layer.md`
- `lib/rbac-dac-macros.zc`
- `examples/rbac-dac-orange-book.zc`
- `docs/theorem-impact-macro-layer.md`
- `lib/theorem-impact-macros.zc`
- `examples/theorem-impact-devops-sre.zc`
- `examples/request-form-recursive.zc`

## Standard Declarations (Phase 1)

Zil now supports a first standard-library declaration surface that lowers into
canonical tuple facts:

```zc
MODULE demo.

SERVICE payment [env=prod, tier=critical].
HOST host1 [environment=prod].
DATASOURCE app_metrics [type=rest, format=json].
METRIC latency [source=datasource:app_metrics, unit=ms].
POLICY latency_guard [condition="latency > 120", criticality=high].
EVENT deploy [start_time="2026-03-16T10:00:00-06:00", labels=["ops","deploy"]].
```

Supported declaration kinds in this phase:

- `SERVICE`
- `HOST`
- `DATASOURCE` (requires `type=...`)
- `METRIC` (requires `source=...`)
- `POLICY`
- `EVENT`
- `PROVIDER` (requires `source=...`; for external provider identities/capabilities)
- `TM_ATOM` (complete deterministic TM unit for model exchange profile `tm.det`)
- `LTS_ATOM` (labeled transition-system unit for profile `lts`)

Lowering behavior:

- each declaration emits `entity-id#kind@entity:<kind>`
- each attribute emits `entity-id#<attr>@<value>`
- collection attrs emit one fact per element
- `criticality` is normalized to `low|medium|high|critical`
- service `depends` aliases to `uses`
- service `depended_by` aliases to `used_by`
- `uses` emits inverse `used_by` + `depends_on` facts
- provider refs (`provider`/`providers`) normalize to `provider:<name>`
- provider refs emit inverse `provider:<name>#provides_for@<entity>`

Validation in this phase:

- duplicate declarations are rejected
- service dependency targets must exist and be services
- service dependency cycles are rejected
- metric `source=...` references must point to declared datasources
- datasource and environment enum fields are normalized/validated
- provider refs must target declared `PROVIDER` entities
- `TM_ATOM` consistency + transition completeness are validated
- `LTS_ATOM` transition-state and shape constraints are validated

## Ingest Adapter Skeleton (Phase 3)

Runtime ingestion now includes:

- adapter registry: `src/zil/runtime/adapters/core.clj`
- adapters: `rest`, `file`, `command`, `cucumber`
- ingest pipeline: `src/zil/runtime/ingest.clj`

The ingest pipeline runs datasource declarations once and transacts normalized
facts into DataScript (`:ingested_record`, `:observed_from`, plus scalar fields).

Polling support:

- `poll_mode=event` (default) => one-shot ingest
- `poll_mode=interval` + `poll_every_ms=<n>` => continuous ingest loop

REST adapter notes:

- supports real HTTP calls with `url=...`
- supports `method`, `headers`, `body`, `timeout_ms`
- parses `format=json|edn|text`
- still supports `mock_response` / `mock_responses` and `path`

Cucumber adapter notes:

- `DATASOURCE ... [type=cucumber, path=\"/path/to/cucumber-report.json\"]`
- parses feature/scenario/step records into normalized ingest records
- emits scenario-local vector clock components and before edges
- supports causal checks via `before?` / `concurrent?` runtime helpers

See:

- `docs/cucumber-causal-integration.md`
- `examples/cucumber-causal-datasource.zc`

## Git-Native Modeling Plan

Planning doc for using Zil + Git so commits represent formal model changes:

- `docs/git-model-workflow-plan.md`
- `spec/model-exchange-profiles-v0.1.md`
- `spec/model-exchange-tm-atom-v0.1.md`

Bundle policy check:

```bash
clojure -M -m zil.cli bundle-check examples [tm.det|lts|constraint]
```

`bundle-check` enforces compilation + presence of at least one declaration for
the selected profile (`TM_ATOM`, `LTS_ATOM`, or `POLICY`).

For `constraint` profile, `POLICY` conditions are solver-checked using Z3
(SMT-LIB over arithmetic/boolean expressions). Unsatisfiable or unknown
conditions fail checks.
Implication is supported as `IMPLIES`, `->`, or `=>`.

Commit-unit policy check (profile-aware):

```bash
clojure -M -m zil.cli commit-check path/to/commit-bundle [tm.det|lts|constraint]
```

`commit-check` enforces compilation + exactly one profile unit per `.zc` file.
By default it also requires strict unit-only files (no non-profile declarations).

To allow mixed files while keeping one profile unit per file:

```bash
clojure -M -m zil.cli commit-check path/to/commit-bundle lts --allow-mixed
```

Bridge exporter (LTS_ATOM -> parse-friendly TLA+):

```bash
clojure -M -m zil.cli export-tla examples/sshx11-vpn-system.zc
clojure -M -m zil.cli export-tla examples/sshx11-vpn-system.zc /tmp/sshx11_bridge.tla SSHX11BridgeFromZil
```

Lean4 skeleton exporter (same LTS vocabulary -> `State`, `Event`, `step`):

```bash
clojure -M -m zil.cli export-lean examples/sshx11-vpn-system.zc
clojure -M -m zil.cli export-lean examples/sshx11-vpn-system.zc /tmp/sshx11_bridge.lean Zil.Generated.SSHX11
```

Theorem bridge generator (theorem contracts -> `LTS_ATOM` + `POLICY` sidecar):

```bash
clojure -M -m zil.cli theorem-bridge examples/theorem-impact-devops-sre.zc
clojure -M -m zil.cli theorem-bridge examples/theorem-impact-devops-sre.zc /tmp/theorem_bridge.zc theorem.bridge.generated
```

Theorem incident CI (one-shot: bridge + checks + TLA/Lean exports):

```bash
clojure -M -m zil.cli theorem-ci examples/theorem-impact-devops-sre.zc /tmp theorem.bridge.generated TheoremBridgeFromZil Zil.Generated.TheoremBridge
```

Proof-obligation execution gate (`tool=z3` and `tool=acl2` log-evidence mode):

```bash
clojure -M -m zil.cli proof-obligation-check examples/vstack-refinement-minimal.zc z3
clojure -M -m zil.cli proof-obligation-check examples/acl2-proof-obligation-log.zc acl2
```

`vstack-ci` can now select obligation backend explicitly:

```bash
clojure -M -m zil.cli vstack-ci examples/vstack-refinement-minimal.zc /tmp - - - - all
```

When using `all`, each non-Z3 obligation should provide either `artifact_in` or
`command` evidence so the gate does not remain `unknown`.

Theorem DSL CI (macro DSL + operator summary JSON + formal exports):

```bash
clojure -M -m zil.cli theorem-dsl-ci examples/theorem-dsl-incident.zc /tmp
```

## Usage Comparison (Best Way By Goal)

`./bin/zil` is the portable default command in this table. If you prefer direct
Clojure invocation, replace `./bin/zil` with `clojure -M -m zil.cli`.

| Goal | Best Zil Path | Command | Why this is best |
|---|---|---|---|
| Fast local modeling and query iteration | Execute `.zc` directly | `./bin/zil models/system.zc` | Fast feedback loop; no CI/policy overhead. |
| Validate one bundle before sharing | `bundle-check` with target profile | `./bin/zil bundle-check models lts` | Confirms compile + profile presence + profile checks. |
| Enforce strict per-commit model units in CI | `commit-check` | `./bin/zil commit-check changes lts` | Best for team governance and reviewable commit atoms. |
| Mixed files but still unit-gated | Relaxed `commit-check` | `./bin/zil commit-check changes lts --allow-mixed` | Preserves one unit/file rule while allowing helper declarations. |
| Constraint/invariant consistency | `constraint` profile | `./bin/zil bundle-check models constraint` | Uses SMT (Z3) to detect unsat policies early. |
| Import-like model composition from `lib/*.zc` | `preprocess` | `./bin/zil preprocess models/system.zc /tmp/system.pre.zc` | Enables reusable macro/rule library layering without changing core semantics. |
| Operational incident/state progression models | `LTS_ATOM` + `lts` checks | `./bin/zil bundle-check incidents lts` | Most direct fit for workflows and state transitions. |
| Deterministic machine-atom exchange | `TM_ATOM` + `tm.det` checks | `./bin/zil commit-check units tm.det` | Strong unit completeness contract. |
| Formal spec alignment with TLA+ | `export-tla` from same LTS vocabulary | `./bin/zil export-tla models/sshx11.zc /tmp/model.tla ModuleName` | Keeps transitions and names synchronized with model files. |
| Lean4 implementation/proof bootstrap | `export-lean` from same LTS vocabulary | `./bin/zil export-lean models/sshx11.zc /tmp/model.lean Zil.Generated.SSHX11` | Generates `State`/`Event`/`step` skeletons quickly and consistently. |
| Theorem-to-formal integrated pipeline | `theorem-bridge` + profile checks | `./bin/zil theorem-bridge models/theorems.zc /tmp/theorem_bridge.zc` | Auto-derives `LTS_ATOM` and `POLICY` contracts from theorem facts for one pipeline. |
| Execute refinement/theorem obligations through SMT bridge | `proof-obligation-check` | `./bin/zil proof-obligation-check models/system.zc z3` | Uses declared `PROOF_OBLIGATION` contracts as one formal gate surface. |
| Ingest ACL2 proof outcomes from external runs | `proof-obligation-check` with `tool=acl2` + `artifact_in` | `./bin/zil proof-obligation-check examples/acl2-proof-obligation-log.zc acl2` | Lets ACL2 remain external while feeding deterministic proof verdicts back into ZIL CI/status loops. |
| Live telemetry-driven model updates | `DATASOURCE` + ingest pollers | Runtime API: `start-all-pollers!` with `poll_mode=interval` | Best for continuous observation pipelines. |

Recommended default for teams:

1. Author and iterate locally with direct execution.
2. Run `bundle-check` in pre-commit.
3. Run `commit-check` in CI for merge gates.
4. Export TLA/Lean from the same LTS files for formal tracks.

## Formal Verification Bridge

For current usage with SSHX11 TLA+ and Lean4 specs in this repo, see:

- `docs/sshx11-vpn-zil-modeling.md`
- `docs/tla-lean4-integration.md`

## Policy + IaC Bridge (OPA/Gatekeeper/OpenTofu)

For policy evaluation and infrastructure gating integration, see:

- `docs/opa-gatekeeper-opentofu-integration.md`

## Current Status

- Core: Draft
- Time model: Draft (causal core + optional time profiles)
- Zanzibar compatibility profile: Draft
- DataScript runtime profile: Draft
