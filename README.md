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

Architecture guide:

- `docs/language-architecture.md`
- `docs/language-design.md`
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
- `TM_ATOM` (complete deterministic TM unit for model exchange profile `tm.det`)
- `LTS_ATOM` (labeled transition-system unit for profile `lts`)

Lowering behavior:

- each declaration emits `entity-id#kind@entity:<kind>`
- each attribute emits `entity-id#<attr>@<value>`
- collection attrs emit one fact per element
- `criticality` is normalized to `low|high`
- service `depends` aliases to `uses`
- service `depended_by` aliases to `used_by`
- `uses` emits inverse `used_by` + `depends_on` facts

Validation in this phase:

- duplicate declarations are rejected
- service dependency targets must exist and be services
- service dependency cycles are rejected
- metric `source=...` references must point to declared datasources
- datasource and environment enum fields are normalized/validated
- `TM_ATOM` consistency + transition completeness are validated
- `LTS_ATOM` transition-state and shape constraints are validated

## Ingest Adapter Skeleton (Phase 3)

Runtime ingestion now includes:

- adapter registry: `src/zil/runtime/adapters/core.clj`
- adapters: `rest`, `file`, `command`
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

## Usage Comparison (Best Way By Goal)

| Goal | Best Zil Path | Command | Why this is best |
|---|---|---|---|
| Fast local modeling and query iteration | Execute `.zc` directly | `clojure -M -m zil.cli models/system.zc` | Fast feedback loop; no CI/policy overhead. |
| Validate one bundle before sharing | `bundle-check` with target profile | `clojure -M -m zil.cli bundle-check models lts` | Confirms compile + profile presence + profile checks. |
| Enforce strict per-commit model units in CI | `commit-check` | `clojure -M -m zil.cli commit-check changes lts` | Best for team governance and reviewable commit atoms. |
| Mixed files but still unit-gated | Relaxed `commit-check` | `clojure -M -m zil.cli commit-check changes lts --allow-mixed` | Preserves one unit/file rule while allowing helper declarations. |
| Constraint/invariant consistency | `constraint` profile | `clojure -M -m zil.cli bundle-check models constraint` | Uses SMT (Z3) to detect unsat policies early. |
| Operational incident/state progression models | `LTS_ATOM` + `lts` checks | `clojure -M -m zil.cli bundle-check incidents lts` | Most direct fit for workflows and state transitions. |
| Deterministic machine-atom exchange | `TM_ATOM` + `tm.det` checks | `clojure -M -m zil.cli commit-check units tm.det` | Strong unit completeness contract. |
| Formal spec alignment with TLA+ | `export-tla` from same LTS vocabulary | `clojure -M -m zil.cli export-tla models/sshx11.zc /tmp/model.tla ModuleName` | Keeps transitions and names synchronized with model files. |
| Lean4 implementation/proof bootstrap | `export-lean` from same LTS vocabulary | `clojure -M -m zil.cli export-lean models/sshx11.zc /tmp/model.lean Zil.Generated.SSHX11` | Generates `State`/`Event`/`step` skeletons quickly and consistently. |
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
