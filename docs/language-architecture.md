# ZIL Language Architecture and Scope

## Short Answer

ZIL is a **general-purpose declarative modeling language**.

It is not limited to one IT stack or one product family. The current repository
includes an IT-oriented standard declaration set (`SERVICE`, `HOST`,
`DATASOURCE`, `PROVIDER`, etc.) because that is the primary immediate use case, but those
declarations are layered above a domain-agnostic core.

## What Is Core vs Domain-Specific

Core language (domain-agnostic):

- canonical tuple form: `object#relation@subject [attrs]`
- rules with stratified negation
- queries
- causal/revision semantics
- canonical lowering/evaluation model

Domain libraries (domain-specific, optional):

- IT declarations like `SERVICE`, `HOST`, `DATASOURCE`, `METRIC`, `POLICY`, `PROVIDER`
- model-exchange units (`TM_ATOM`, `LTS_ATOM`)
- future domain packs (for example finance, supply-chain, incident management)

Profiles (optional, environment-specific):

- runtime profile (DataScript)
- verification profiles (constraint/SMT checks, TLA export, Lean skeleton export)
- compatibility profiles (e.g., Zanzibar compatibility)

## Layered Architecture

Think of ZIL as a layered stack:

1. Surface syntax layer:
   - `.zc` source, macros, declarations, rules, queries.
2. Core semantics layer:
   - canonical IR/facts and Datalog-first evaluation semantics.
3. Domain library layer:
   - ergonomic declarations that lower into canonical facts.
4. Profile layer:
   - runtime/verification/compatibility mappings.
5. Operational integration layer:
   - adapters (`rest`, `file`, `command`), ingestion, CI policies.

The key property: upper layers must not redefine core truth conditions.

## Why This Matters

This separation allows:

- stable formal semantics even as integrations evolve
- reuse of the same model in multiple execution/verification backends
- domain growth without rewriting the core language
- explicit governance in Git/CI (`bundle-check`, `commit-check`)

## Is ZIL Only for IT Systems?

No.

Current examples and declarations are IT-heavy, but that is a library choice,
not a core-language restriction.

You can model non-IT domains today by:

1. using canonical facts/rules directly, and
2. adding domain macros/declarations that lower to those facts.

Minimal non-IT style example (direct core tuples):

```zc
MODULE manufacturing.demo.

line:assemblyA#status@value:running.
sensor:temp1#reading@value:92.

RULE overheat:
IF sensor:temp1#reading@?t AND ?t#greater_than@value:90
THEN line:assemblyA#alert@value:overheat.
```

## What Is IT-Focused Today

In this repo today, the most mature domain layer is IT operations and
infrastructure modeling:

- service dependency modeling
- datasource ingest and observation facts
- incident/workflow transitions (`LTS_ATOM`)
- formal verification bridge workflows (TLA+/Lean)

This is a current capability focus, not a language boundary.

## Practical Guidance

- If you need formal, portable model semantics: use core tuples/rules first.
- If you need speed for IT workflows: use stdlib declarations and profiles.
- If you need new domain ergonomics: add macro/declaration libraries that lower
  to the same core semantics.

Macro-based domain library examples:

- `docs/domain-library-macros.md`
- `examples/domain-library-macros.zc`

## Non-Goals

ZIL core is not intended to embed:

- backend-specific storage semantics,
- runtime side-effect semantics,
- one vendor's control-plane model as language truth.

Those belong in profiles/adapters/libraries above the core.
