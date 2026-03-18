# Dune Dependency Semantics as NEED/Acceptance/Provider (ZIL View)

This note formalizes Dune's dependency language into a ZIL-friendly model and
then analyzes what is direct, what needs adaptation, and where solver-backed
steps are required.

## Primary Sources Inspected

- Dune package stanza and dependency grammar:
  - https://dune.readthedocs.io/en/stable/reference/dune-project/package.html
- Dune library dependency forms (`libraries`, `select`):
  - https://dune.readthedocs.io/en/latest/reference/library-dependencies.html
- Dune virtual libraries (`virtual_modules`, `implements`, default implementation):
  - https://dune.readthedocs.io/en/stable/virtual-libraries.html
- Dune package-management solving model:
  - https://dune.readthedocs.io/en/stable/explanation/package-management.html
- Dune source for dependency AST and conversion:
  - https://github.com/ocaml/dune/blob/main/src/dune_lang/package_dependency.ml
  - https://github.com/ocaml/dune/blob/main/src/dune_lang/package_constraint.ml
  - https://github.com/ocaml/dune/blob/main/src/dune_pkg/package_dependency.ml
  - https://github.com/ocaml/dune/blob/main/src/dune_rules/lib.ml
- opam filter/formula semantics used by Dune dependency constraints:
  - https://opam.ocaml.org/doc/Manual.html

## What "Dependency Language" Means in Dune

Dune has two dependency planes plus one provider-selection mechanism:

1. Package-plane dependency formulas in `(package (depends ...))`, `(conflicts ...)`, `(depopts ...)`.
2. Build-plane library dependencies in `(libraries ...)`, including `(select ... from ...)`.
3. Provider selection for virtual libraries via `(implements ...)` (and optional default implementation).

There is no single standalone "dependency RFC" as a separate spec artifact in Dune docs; the canonical behavior is split across reference docs and implementation.

## Formalization

### 1) Package-plane NEED

Let a package NEED be:

- `Need = (pkg_name, formula_opt)`
- `formula_opt` is optional; if absent, acceptance criterion is `true`.

Formula syntax (as documented by Dune, opam-modeled):

- atoms:
  - relation constraints on versions/values (`=`, `<>`, `<`, `>`, `<=`, `>=`)
  - filter variables (for example `:with-test`, `:with-doc`, `:build`, `:post`, `:dev`)
- composition:
  - `and`, `or`, and (in current Dune source) `not` in constraints.

Semantics:

- A provider candidate `p` for NEED `n` is eligible if package names match.
- `Accept(n, p, env)` holds iff `Eval(formula(n), p, env) = true`.
- `env` includes solver variables (platform, with-test/doc flags, etc.).

### 2) Library-plane NEED with `select`

A `select` clause is ordered cases.

- Case has `required_libs`, `forbidden_libs`, and source file.
- A case matches if all required are available and all forbidden are unavailable.
- Selection is first matching case; no match is an error unless fallback is provided.

This is NEED with ordered acceptance criteria over availability predicates.

### 3) Provider plane for virtual libraries

Virtual library semantics:

- A virtual library defines an interface-like NEED.
- Implementations are providers declared with `(implements <virtual-lib>)`.
- At link time, every used virtual library must resolve to exactly one provider.
- Conflicting multiple implementations are rejected.
- Optional default implementation may satisfy unresolved NEEDs (with package constraints).

## Canonical ZIL Mapping

Use these core entities:

- `need:<id>`: dependency requirement.
- `provider:<id>`: concrete package version or virtual-library implementation.
- `crit:<id>`: acceptance criterion atom or normalized criterion branch.

Core facts:

- `need:<id>#for_package@pkg:<name>.`
- `need:<id>#accepts@crit:<criterion>.`
- `provider:<id>#provides_package@pkg:<name>.`
- `provider:<id>#meets@crit:<criterion>.`

Core derivations:

- candidate provider:
  - if NEED package name equals provider package name.
- accepted provider:
  - candidate and provider meets one accepted criterion.

For full boolean formulas:

- normalize Dune/opam formulas into explicit criterion branches before loading
  facts into ZIL, or
- keep formula evaluation external (solver/evaluator) and ingest `meets` facts.

The second path matches Dune's own architecture better because real solving is
already done through opam solver machinery.

## Analysis

### What ports cleanly into ZIL

- NEED identity and package target.
- Acceptance criteria as explicit criterion facts.
- Provider inventory and accepted-provider proofs.
- Explanation/audit trails ("why provider selected", "which criterion matched").

### What does not belong in pure ZIL core

- Full version-order arithmetic and opam filter evaluation should remain solver-backed.
- SAT-like global dependency resolution (transitive closure + conflict solving) should remain in the dedicated solver layer.
- Ordered first-match and uniqueness constraints are best either:
  - encoded as explicit derived facts from an adapter, or
  - handled by a profile/engine extension with ordering and cardinality checks.

### Practical architecture (recommended)

1. Parse Dune dependency declarations (`depends`, `depopts`, `conflicts`, `select`, virtual libs).
2. Convert each dependency into normalized NEED + criterion objects.
3. Run the solver (opam-compatible semantics) externally.
4. Ingest solver outputs as PROVIDER + `meets` + `selected` facts.
5. Let ZIL handle cross-model reasoning, auditability, and policy checks.

This keeps semantics faithful to Dune while making NEED/provider logic explicit
and queryable in ZIL.
