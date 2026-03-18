# Cucumber Integration with Vector Clocks and Partial Order

This guide shows how to integrate ZIL with Cucumber-style test execution using
causal ordering primitives:

- vector clocks (representation profile),
- `before` / happens-before partial order (core semantics),
- concurrency checks (incomparability).

## Why This Layering Works

ZIL time core treats causality as primary:

- `before(e1,e2)` is authoritative.
- vector clocks are an optional deterministic representation that must not
  contradict causal order.

For Cucumber integration this means:

1. emit executable test events,
2. attach vector-clock metadata per event,
3. derive or declare `before` edges,
4. run causal and concurrency queries.

## Adapter Support

Runtime ingest now includes datasource type `cucumber`.

Input:

- Cucumber JSON report (feature -> scenario -> step hierarchy).

Output records per step:

- `event_id`
- `scenario_id` (used as default actor)
- `step_index`
- `vector_clock` (local scenario component increments)
- `before` edge to previous step in same scenario
- status/duration/metadata fields

This gives a minimal but useful causal model:

- total order inside each scenario,
- cross-scenario events are concurrent unless explicit dependencies are added.

## Minimal ZIL Model File

Use:

- `examples/cucumber-causal-datasource.zc`

It declares:

- `DATASOURCE cucumber_run [type=cucumber, ...]`
- one metric and one policy placeholder

## Runtime Ingest Example

```clj
(require '[zil.core :as core]
         '[zil.runtime.datascript :as zr]
         '[zil.runtime.ingest :as ingest])

(def program
  "MODULE cucumber.demo.
   DATASOURCE cuc_run [type=cucumber, path=\"/tmp/cucumber-report.json\"].")

(def compiled (core/compile-program program))
(def conn (zr/make-conn))
(def summary (ingest/ingest-all! conn compiled {:revision 100}))

;; before-edge datoms
(zr/q '[:find ?l ?r
        :where
        [?e :zil/event-left ?l]
        [?e :zil/event-right ?r]]
      @conn)

;; vector-clock component facts
(zr/q '[:find ?event ?actor ?attrs
        :where
        [?e :zil/object ?event]
        [?e :zil/relation :vc_component]
        [?e :zil/subject ?actor]
        [?e :zil/attrs ?attrs]]
      @conn)
```

`summary` now reports:

- `:before_edges`
- `:before_edges_explicit`
- `:before_edges_derived`

## Partial-Order and Concurrency Queries

Use runtime helpers:

- `zil.runtime.datascript/before?`
- `zil.runtime.datascript/concurrent?`

and vector-clock helpers:

- `normalize-vector-clock`
- `vector-clock-before?`
- `vector-clock-concurrent?`
- `derive-before-edges-from-vector-clocks`

Recommended workflow:

1. ingest cucumber run,
2. validate expected `before` edges (scenario-local and explicit dependencies),
3. check suspicious event pairs with `concurrent?`,
4. gate CI if forbidden concurrency or missing causal links are detected.

## Cross-Scenario Dependencies

Standard Cucumber JSON does not encode all distributed synchronization points.
If you need cross-scenario causality, enrich records with explicit `before`
lists (for example from hooks, orchestrator metadata, or external trace IDs).

Then ZIL ingest merges:

- explicit `before` edges,
- vector-clock-derived edges.

## Practical Notes

1. Vector clocks are metadata; causal edges remain the semantic source of truth.
2. Keep actor cardinality bounded (scenario, worker, or service role) to avoid
   high-dimensional vectors.
3. Use explicit `before` for barriers/checkpoints that Cucumber runtime does
   not expose directly.
