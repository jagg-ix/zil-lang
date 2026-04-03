# AWS ZIL Extension + Icon Representation

This package lets ZIL model AWS capabilities and represent AWS icon semantics
as queryable facts (without embedding binaries).

## What was added

- macro extension: `lib/aws-model-macros.zc`
- focused libset: `libsets/aws-model/aws-model-macros.zc`
- icon indexer: `tools/index_aws_icon_package.py`
- integration demo: `examples/aws-model-extension-with-icons.zc`
- full smoke: `tools/aws_extension_icons_smoke.sh`

## Source inputs

- AWS whitepaper PDF:
  `~/Downloads/aws-overview.pdf`
- AWS icon package:
  `~/Downloads/Icon-package_01302026.31b40d126ed27079b708594940ad577a86150582`

## Modeling approach for icons

The indexer emits:

- `examples/generated/aws-icon-catalog.json`
- `examples/generated/aws-icon-catalog.zc`
- `examples/generated/aws-service-icon-links.zc`

Representation structure:

- `aws:icon_concept:*` for logical concepts
  - family (`resource`, `architecture_service`, `architecture_group`, `category`)
  - bucket (e.g. `compute`, `storage`)
  - normalized token
  - preferred asset relation
- `aws:icon_asset:*` for concrete variants
  - format (`svg` / `png`)
  - size (`n_16`, `n_32`, `n_48`, `n_64`)
  - variant (`default`, `dark`, `light`)
  - tokenized relative-path reference
- service links:
  - `aws:service:<id>#icon_concept@aws:icon_concept:<id>`
  - `aws:service:<id>#icon_link_confidence@value:n_<score>`

## Full pipeline

```bash
cd zil
./tools/aws_extension_icons_smoke.sh
```

This runs extraction + icon indexing + combined execution and validates:

- compatibility target is ready (`prod_extension`)
- no missing required services/regions/config keys
- no missing icon links
- service-icon link query is present

