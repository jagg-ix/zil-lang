# Leveraging DataScript for Zil

This guide explains why DataScript is a strong fit for Zil and how to use it
without collapsing core language design into backend-specific behavior.

Reference implementation source:
`https://github.com/tonsky/datascript`

## Why DataScript Fits Zil

1. Datalog-first evaluation aligns with Zil rule/query semantics.
2. Immutable DB values support revision and frontier snapshots.
3. Recursive rules directly model transitive dependencies and causal closure.
4. Tuple attributes provide practical composite identities and constraints.

## Backend Boundary

Keep this boundary strict:

- Zil core semantics remain in `spec/`.
- DataScript details live in runtime profile + adapter code.
- Source language should not require DataScript-specific syntax.

## Recommended Fact Shape

Use canonical fact records before transacting:

```clj
{:object "app:svc1"
 :relation :depends_on
 :subject "service:db1"
 :attrs {:critical true}
 :revision 12
 :event :e12
 :op :assert}
```

Retract by re-emitting same key with higher revision and `:op :retract`.

## Composite Keys (Tuple Attrs)

DataScript tuple attrs are the key leverage point:

- `[:object :relation :subject]` for logical fact identity.
- `[:object :relation :subject :revision]` for revision identity.
- `[:event-left :event-right]` for causal-edge identity.

This gives deterministic upsert behavior and cleaner query paths.

## Snapshot Strategy

Profile snapshot is revision-frontier based:

1. query all fact rows `<= frontier`,
2. keep latest row per logical fact key,
3. include only asserted rows.

This approximates append-only event-sourcing while staying in simple DataScript primitives.

## Causal Query Pattern

Represent happens-before edges as datoms and define recursive rule `before*`.
Then:

- `before?(a,b)` is reachability in `before*`,
- `concurrent?(a,b)` is absence of reachability in both directions.

## IT Infrastructure Modeling Patterns

Useful direct mappings from your scenarios:

1. topology edges:
   - `location:dcA#connected_to@location:dcB`
2. deployment bindings:
   - `app:svc1#deployed_on@location:dcA`
3. service health:
   - `service:db1#available@value:true`
4. policy/risk states:
   - `app:svc1#status@value:"degraded"`

These all remain plain facts with optional attrs for metadata (owner, confidence, source).

## Macro Layer Opportunity

Macros can improve ergonomics while compiling to canonical facts:

- infrastructure shortcuts (`DATACENTER`, `SERVICE`, `LINK`),
- rule templates (`FAILOVER_IF`, `DRIFT_IF`),
- policy shorthand that expands into plain rule clauses.

Do not let macros change semantic meaning; they must lower to core forms.

## Minimal Runtime API

The scaffold in `src/zil/runtime/datascript.clj` provides:

1. `make-conn`
2. `transact-facts!`
3. `transact-before-edges!`
4. `facts-at-or-before`
5. `before?` / `concurrent?`
6. `q` pass-through for custom Datalog

This is intentionally small so semantics stay inspectable and testable.
