# Zilog Language

Zilog is a declarative tuple-and-rule language for general knowledge modeling,
verification, and policy reasoning.

This repository now includes both:
- language design artifacts (normative specs), and
- an initial Clojure + DataScript runtime scaffold that executes core ideas.

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
- `src/zilog/runtime/` executable runtime scaffolds

## DataScript Leverage

We leverage the Clojure DataScript engine as a concrete runtime target:
- immutable in-memory DB values for snapshot-friendly semantics,
- Datalog query/rule execution as the primary evaluation model,
- tuple attributes and unique constraints for composite identity and integrity,
- direct support for recursive rules and stratified negation patterns.

See:
- `spec/runtime-datascript-profile-v0.1.md`
- `docs/datascript-leverage.md`
- `src/zilog/runtime/datascript.clj`

## Quick Start (Clojure)

```bash
clojure -M -e "(require 'zilog.runtime.datascript) (println :ok)"
```

Minimal usage:

```clj
(require '[zilog.runtime.datascript :as zr])

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

## Current Status

- Core: Draft
- Time model: Draft (causal core + optional time profiles)
- Zanzibar compatibility profile: Draft
- DataScript runtime profile: Draft
