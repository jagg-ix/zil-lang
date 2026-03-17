# Zil Extension Assessment for `zanzibar-lang4-dsl-general.md`

## Scope

This assessment inspects how Zil can cover the requirements captured in:

- `/Users/macbookpro/Downloads/zanzibar-lang4-dsl-general.md`

using two lenses:

1. language design (what belongs in core vs profiles vs macros)
2. implementation design (what parser/IR/runtime components to add)

## What the Source Document Asks For

From the user-input stream in that document (110 user prompts), the requested DSL expands beyond Zanzibar-style authorization and targets a broad IT operations knowledge language.

Main demand clusters:

- Core domain model: service/app/host/environment/location/datacenter/network/CDN/team/owner/vendor
- SLO domain: SLI/SLO/SLA metrics, constraints, policies, exceptions, defaults
- Data plane: data sources (REST/JSON, TCP/WebSocket/pipe, file sets, command outputs, host telemetry)
- Time: windows/periods/schedules/recurrence, timezone-aware reasoning, pre/post conditions and hooks
- Topology and infra performance: channels, queue behavior, host bus/controller/fabric/NUMA/PCIe/cache
- Analytics: map-reduce style aggregation, anomaly detection, cost modeling, entropy/capacity
- Formal methods: Z3/TLA support as optional verification backends
- Operational concerns: validation, dependency graph checks, execution ordering, packaging/deployment

## Fit Against Current Zil

Current Zil state (repo `zil/`):

- Core tuples: `object#relation@subject [attrs]`
- Rules: stratified negation + safety checks
- Queries: `FIND ... WHERE ...`
- Time core spec: causal partial order with optional clock profiles
- DataScript runtime scaffold
- Native macro system: `MACRO` / `EMIT` / `USE`

This is a good substrate, but still minimal relative to the source document.

### Coverage Summary

- Already aligned:
  - tuple-based knowledge graph
  - Datalog-first rules/queries
  - modular profile concept (core vs Zanzibar profile)
  - native macro expansion layer
- Missing or partial:
  - typed domain declarations (`service`, `datasource`, `event`, etc.)
  - temporal DSL surface (recurrence/timezones/event lifecycle)
  - ingestion/adapters model for external telemetry
  - dependency graph and execution plan semantics
  - analysis libraries (map-reduce, anomaly, entropy/capacity)
  - verification adapters (Z3/TLA lowering)

## Recommended Language Architecture

## A) Keep Core Small and Stable

Core should stay as the minimal semantic kernel:

- facts, rules, queries, constraints
- causality and snapshot semantics
- no hard dependency on a specific operations domain

Do **not** push Z3, MapReduce, or AppDynamics into core semantics.
Those are profiles or libraries.

## B) Add a Standard Library Layer (Zil Lib)

Define an official domain library that lowers into core tuples.
This gives expressivity without bloating core.

Example categories:

- `lib.entity`: service/app/host/location/team/vendor
- `lib.slo`: metric/constraint/policy/exception
- `lib.time`: schedule/recurrence/event/pre-post hooks metadata
- `lib.data`: datasource/collector/channel/fileset/command
- `lib.infra`: queue/cache/bus/controller/fabric
- `lib.analysis`: map-reduce/entropy/cost/anomaly descriptors

Each construct should compile to canonical fact tuples + attrs.

## C) Use Profiles for Backend-Specific Behavior

Profiles remain optional overlays:

- `profile/zanzibar-compat` authorization mapping
- `profile/z3-checks` SMT lowering for selected constraints/policies
- `profile/tla-export` state-machine extraction
- `profile/runtime-datascript` concrete execution target

## D) Use Macros for Ergonomic Surface Syntax

Your native macro system is the right place for high-level forms.

Example style:

```zc
MACRO SERVICE(name, env, tier):
EMIT service:{{name}}#kind@entity:service [env="{{env}}", tier="{{tier}}"].
ENDMACRO.

USE SERVICE(payment, prod, critical).
```

This keeps syntax convenient while preserving one canonical core IR.

## Canonical IR Extension Proposal

Extend IR in a backward-compatible way:

- keep existing `:fact` and `:before`
- add optional metadata envelope for typed declarations:

```clj
{:type :fact
 :object "service:payment"
 :relation :depends_on
 :subject "service:db"
 :attrs {:criticality :high
         :domain :slo
         :source :datasource/appd}}
```

For non-fact declarations (`datasource`, `event`, `schedule`), lower them to normalized fact bundles instead of introducing many new core record types.

## Time and Timezone Model

The document asks for per-entity timezone handling and recurrence.

Recommendation:

- Core time remains causal (already spec'd)
- Add `lib.time` with explicit fields:
  - `:time/zone` per source/entity/event
  - `:time/instant` (canonical UTC)
  - `:time/local` (original local representation)
  - recurrence tuple facts (`:rrule`, `:interval`, `:count`, `:until`)
- Deterministic normalization rule:
  - ingest local timestamp + zone
  - normalize to UTC for evaluation
  - preserve original tuple attrs for audit

This satisfies both correctness and operational traceability.

## Data Source and Ingestion Semantics

Model data acquisition declaratively, execute with adapter plugins.

Suggested minimal ingestion schema (as facts):

- `datasource:X#type@value:rest|file|command|socket|websocket|pipe`
- `datasource:X#poll_mode@value:event|interval`
- `datasource:X#format@value:json|text|csv|kv`
- `datasource:X#command_path@path:/...`
- `datasource:X#emits@metric:Y`

Implementation contract:

- parser validates declaration shape
- runtime loads adapter by type
- adapter emits normalized fact events
- events get causal stamps and optional clock profile stamps

## Dependency and Execution Graph

The source asks for ordering and cycle detection.

Recommendation:

- Introduce derived relation `depends_on` over deployable entities
- Build execution DAG in runtime planner
- reject cycles before evaluation (or stratify if possible)
- produce explicit plan artifacts (`plan step`, `blocked-by`, `cycle`) as facts

This allows queryable execution diagnostics.

## Formal Verification Integration

Keep verification optional but first-class in tooling.

- Z3 profile:
  - lower selected constraints/policies to SMT
  - return satisfiable/unsat/core as facts
- TLA profile:
  - export transition systems from event/rule subsets
  - link model check outcomes back as facts

Do not couple main evaluator correctness to solver availability.

## Implementation Plan (Concrete)

### Phase 1: Parser + AST (short)

Files:

- `src/zil/core.clj`

Add:

- typed declaration parsing via macro-lowered facts (preferred) or direct parsers
- richer attr value parsing (keywords, sets, nested maps in safe EDN subset)
- validation pass for standard library constructs

### Phase 2: Canonical Lowering and Validation (short)

Add namespace:

- `src/zil/lower.clj`

Responsibilities:

- lower stdlib constructs to canonical facts
- enforce naming conventions (`criticality` lower-case, allowed enums)
- check required relations (`service uses`, `used_by`, etc.)

### Phase 3: Runtime Adapter Layer (medium)

Add namespaces:

- `src/zil/runtime/adapters/*.clj`
- `src/zil/runtime/ingest.clj`

Responsibilities:

- adapter registry (`rest`, `file`, `command`, `host-metrics`)
- pull/push ingestion into fact stream
- causal/event stamping + timezone normalization

### Phase 4: Analysis Libraries (medium)

Add namespaces:

- `src/zil/lib/time.clj`
- `src/zil/lib/graph.clj`
- `src/zil/lib/analysis.clj`

Capabilities:

- recurrence expansion helpers
- dependency DAG checks and topological ordering
- map-reduce-like aggregate operators as deterministic query helpers

### Phase 5: Verification Profiles (medium)

Add namespaces:

- `src/zil/profile/z3.clj`
- `src/zil/profile/tla.clj`

Capabilities:

- controlled lowering from selected constraints/rules
- result ingestion back into fact model

## Suggested Priority for Your Next Iteration

1. standard library declarations for `service`, `host`, `datasource`, `metric`, `policy`, `event`
2. timezone + recurrence normalization pipeline
3. dependency graph validation and execution plan facts
4. adapter framework for command/file/rest ingestion
5. verification profile hooks (Z3/TLA)

This order gives immediate value for IT scenarios while preserving Zil's core correctness and composability.

## Design Verdict

Zil can cover the `zanzibar-lang4` scope if you treat it as:

- a small Datalog-first semantic core,
- plus a strong standard library of domain declarations,
- plus optional execution/verification profiles,
- plus macro-based ergonomic syntax.

Trying to place all requested IT abstractions directly in core would make the language unstable and harder to reason about. The layered approach above keeps it extensible and precise.
