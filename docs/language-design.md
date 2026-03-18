# Language Design Notes

## Scope Clarification

ZIL is a general-purpose declarative language, not an IT-only DSL.

The current IT-flavored declaration surface exists as a domain library layered
on top of core semantics. The core remains domain-agnostic and can describe
non-IT models using canonical facts/rules directly.

See:

- `docs/language-architecture.md`
- `spec/zil-v0.1r1.md`

## Core vs Macro Rule

Core should define semantics and correctness contracts.
Macros should provide syntax sugar and reusable templates that expand into core forms.

Core examples:
- causal semantics
- constraint semantics
- aggregation semantics

Macro examples:
- IT DSL convenience forms (`DATACENTER`, `RULESET`, `MIRROR_CHECK`)
- policy shorthand expansion

Runtime profiles (such as DataScript) are outside core and outside macros.
They define execution/storage mappings for already-lowered canonical IR.

Zil macros are language-native (`MACRO`/`EMIT`/`USE`) and expand before parsing.
They are intentionally independent of host-language macro systems.

Domain library macro examples:

- `docs/domain-library-macros.md`
- `examples/domain-library-macros.zc`
- `docs/tlm-macro-layer.md`
- `examples/tlm-domain-macros.zc`
- `examples/tlm-formal-bridge.zc`

## Standard Library Declarations

The language now includes a first declaration surface for common IT entities:

- `SERVICE`, `HOST`, `DATASOURCE`, `METRIC`, `POLICY`, `EVENT`, `PROVIDER`, `TM_ATOM`, `LTS_ATOM`

These are not new core semantics. They are lowered to the canonical tuple core:

- declaration kind fact: `entity#kind@entity:<kind>`
- declaration attrs: `entity#attr@value`
- service dependencies: `uses` / `used_by` / `depends_on`
- provider relations: `entity#provider@provider:<name>` + inverse `provider:<name>#provides_for@entity`
- TM transition relation: `tm_atom:<name>#transition@tmtr:<idx> [...]`
- LTS edge relation: `lts_atom:<name>#edge@ltsedge:<idx> [...]`

This keeps the core stable while providing a more ergonomic authoring layer.

## Architecture Layers (Design View)

1. Core semantics:
   - canonical tuples, rules, queries, causal/revision semantics.
2. Macro/declaration layer:
   - ergonomic authoring forms lowered to core.
3. Profile layer:
   - runtime and verification mappings (DataScript, SMT, TLA, Lean bridge).
4. Adapter/operations layer:
   - ingestion and external-system integration.

Only layer 1 defines language truth conditions.

## Validation and Runtime Layers

Validation currently includes:

- enum normalization/validation for selected attrs
- declaration uniqueness
- dependency existence and cycle checks for service graph
- metric source reference checks

Runtime ingestion is adapter-based and remains outside core semantics:

- adapters (`rest`, `file`, `command`) read records
- ingest pipeline lowers records to canonical facts
- DataScript remains one execution target profile
