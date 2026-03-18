# Request Form Modeling in ZIL

This guide models a formal request:

`requesting <something>`

where `<something>` can be:

1. data,
2. effectful actions,
3. compound and recursive structures.

## Normative Profile

- `spec/request-form-core-v0.1.md`

## Macro Library

- `lib/request-form-macros.zc`

Key macros:

- `RF_REQUEST`
- `RF_ROOT`
- `RF_DATA`
- `RF_ACTION`
- `RF_COMPOUND`
- `RF_RECUR`
- `RF_CONTAINS`
- `RF_EFFECT`
- `RF_ACTION_EFFECT`
- `RF_CRITERION`
- `RF_REQUEST_ACCEPTS`

## Why this helps

The model separates:

1. structure (`node:*`, `contains`),
2. side effects (`effect:*`, `has_effect`),
3. acceptance (`criterion:*`, `request#accepts@...`),
4. execution intent (`mode=dry_run|apply`).

This allows complex entities (including recursive action plans) to be described
formally without immediately executing effects.

## Run the example

```bash
cd zil
./bin/zil preprocess examples/request-form-recursive.zc /tmp/request.pre.zc
./bin/zil /tmp/request.pre.zc
```

Example file:

- `examples/request-form-recursive.zc`
