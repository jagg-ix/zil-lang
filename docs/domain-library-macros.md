# Domain Library Layers with Macros

This guide shows how to add domain-specific language layers in ZIL using native
macros while preserving core semantics.

## Why macros for domain layers

Use macros when you want:

- convenient domain syntax (healthcare, manufacturing, legal, etc.)
- reusable declaration patterns
- zero changes to core language semantics

Macros expand to normal ZIL lines before parsing, so the result is still
canonical facts/rules/queries.

## Macro layer design rules

1. Domain macros should lower into canonical facts/rules.
2. Do not encode backend/runtime behavior in macro meaning.
3. Keep macro names domain-readable; keep emitted tuples core-readable.
4. Compose macros with `EMIT USE ...` for library-like reuse.

## Pattern 1: Entity constructors

```zc
MACRO PRODUCT(name, category):
EMIT product:{{name}}#kind@entity:product.
EMIT product:{{name}}#category@value:{{category}}.
ENDMACRO.
```

## Pattern 2: Relation bundles

```zc
MACRO STOCK(product, warehouse, qty):
EMIT stock:{{product}}_{{warehouse}}#kind@entity:stock_record.
EMIT stock:{{product}}_{{warehouse}}#product@product:{{product}}.
EMIT stock:{{product}}_{{warehouse}}#warehouse@warehouse:{{warehouse}}.
EMIT stock:{{product}}_{{warehouse}}#qty@value:{{qty}}.
ENDMACRO.
```

## Pattern 3: Policy/rule templates

```zc
MACRO LOW_STOCK_RULE(product, warehouse, min_qty):
EMIT RULE low_stock_{{product}}_{{warehouse}}:
EMIT IF stock:{{product}}_{{warehouse}}#qty@?q AND ?q#less_than@value:{{min_qty}}
EMIT THEN alert:{{product}}_{{warehouse}}#type@value:low_stock.
ENDMACRO.
```

If your rule head needs multiple facts, emit a single `THEN ...` line and join
atoms with `AND`.

## Pattern 4: Library composition

```zc
MACRO REORDER_LIBRARY(product, warehouse, supplier, min_qty):
EMIT USE LOW_STOCK_RULE({{product}}, {{warehouse}}, {{min_qty}}).
EMIT supplier:{{supplier}}#replenishes@product:{{product}}.
EMIT warehouse:{{warehouse}}#preferred_supplier@supplier:{{supplier}}.
ENDMACRO.
```

## Runnable example

See:

- `examples/domain-library-macros.zc`

Run:

```bash
cd zil
clojure -M -m zil.cli examples/domain-library-macros.zc
```

This example demonstrates:

- a supply-chain domain library layered with macros
- macro composition (`EMIT USE ...`)
- rule generation from macro templates
- querying generated facts/alerts

## Generalization beyond IT

The same pattern works for other domains:

- healthcare: `PATIENT`, `OBSERVATION`, `TRIAGE_RULE`
- finance: `ACCOUNT`, `TRANSACTION`, `RISK_POLICY`
- manufacturing: `LINE`, `MACHINE`, `DOWNTIME_RULE`

Core stays the same; only domain macro vocabulary changes.

## Current practical limitation

ZIL currently has no import/include syntax for macro libraries, so macros are
usually placed at the top of each model file (or concatenated in build tooling
before execution).
