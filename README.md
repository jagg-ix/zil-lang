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
- `docs/provider-model-hcl-import.md`
- `docs/needs-provider-macro-layer.md`
- `docs/request-form-modeling.md`
- `docs/rbac-dac-macro-layer.md`
- `docs/theorem-impact-macro-layer.md`
- `docs/theorem-dsl-ci-workflow.md`
- `docs/vscode-wolfram-reuse-for-zil.md`
- `spec/zil-v0.1r1.md`

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

Preprocess a model with automatic `lib/*.zc` concatenation:

```bash
./bin/zil preprocess models/system.zc /tmp/system.pre.zc
./bin/zil /tmp/system.pre.zc
```

RBAC/DAC macro-layer demo:

```bash
./bin/zil preprocess examples/rbac-dac-orange-book.zc /tmp/rbd.pre.zc
./bin/zil /tmp/rbd.pre.zc
```

Theorem/lemma status + backward breakage demo:

```bash
./bin/zil preprocess examples/theorem-impact-devops-sre.zc /tmp/theorem_impact.pre.zc
./bin/zil /tmp/theorem_impact.pre.zc
```

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
- `examples/tlm-domain-macros.zc`
- `examples/tlm-formal-bridge.zc`
- `docs/tpl-macro-layer.md`
- `examples/tpl-macro-layer.zc`
- `docs/pi-calculus-vc-macro-layer.md`
- `examples/pi-calculus-vc-macro-layer.zc`
- `docs/temporal-pi-quickstart.md`
- `examples/temporal-pi-quickstart.zc`
- `docs/petrinet-nd-macro-layer.md`
- `examples/petrinet-nd-macro-layer.zc`
- `docs/provider-model-hcl-import.md`
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
