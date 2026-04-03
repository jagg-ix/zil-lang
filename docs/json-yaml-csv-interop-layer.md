# JSON/YAML/CSV Interop Layer

This layer adds practical interoperability between ZIL and external systems
that exchange JSON, YAML, or CSV.

## What is included

1. Runtime codec registry (`src/zil/runtime/codec.clj`)
2. Adapter support for `format=json|yaml|yml|csv` in:
   - `src/zil/runtime/adapters/file.clj`
   - `src/zil/runtime/adapters/rest.clj`
   - `src/zil/runtime/adapters/command.clj`
3. Macro layer (`lib/interop-macros.zc`) for concise consume/produce intent
4. CLI commands:
   - `import-data`
   - `export-data`

## Consume side (into ZIL)

Describe sources in model declarations:

```zc
DATASOURCE ds_a [type=file, format=json, path="examples/data/interop-sample.json"].
DATASOURCE ds_b [type=file, format=yaml, path="examples/data/interop-sample.yaml"].
DATASOURCE ds_c [type=file, format=csv,  path="examples/data/interop-sample.csv"].
```

Or with macros:

```zc
USE IO_FILE_SOURCE(ds_a, json, "examples/data/interop-sample.json").
USE IO_FILE_SOURCE(ds_b, yaml, "examples/data/interop-sample.yaml").
USE IO_FILE_SOURCE(ds_c, csv, "examples/data/interop-sample.csv").
```

## Produce side (from ZIL)

Export model payloads directly:

```bash
./bin/zil export-data examples/interop-json-yaml-csv.zc json /tmp/zil_queries.json queries
./bin/zil export-data examples/interop-json-yaml-csv.zc yaml /tmp/zil_queries.yaml queries
./bin/zil export-data examples/interop-json-yaml-csv.zc csv  /tmp/service_states.csv service_states
```

Notes:
- CSV export expects a single tabular source (query name), not all queries at once.
- JSON/YAML can export `queries`, `facts`, or one query.

## Import external payloads as ZIL facts

Generate a `.zc` model from JSON/YAML/CSV input:

```bash
./bin/zil import-data examples/data/interop-sample.json /tmp/interop_from_json.zc interop.import json
./bin/zil import-data examples/data/interop-sample.yaml /tmp/interop_from_yaml.zc interop.import yaml
./bin/zil import-data examples/data/interop-sample.csv  /tmp/interop_from_csv.zc  interop.import csv
```

Then execute:

```bash
./bin/zil /tmp/interop_from_json.zc
```

## Runnable demo

- Model: `examples/interop-json-yaml-csv.zc`
- Data assets:
  - `examples/data/interop-sample.json`
  - `examples/data/interop-sample.yaml`
  - `examples/data/interop-sample.csv`

Use preprocess so macro library is included:

```bash
./bin/zil preprocess examples/interop-json-yaml-csv.zc /tmp/interop.pre.zc
./bin/zil /tmp/interop.pre.zc
```
