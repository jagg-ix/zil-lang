# Ingest Adapters (Phase 3)

This phase introduces a minimal pull-mode ingest pipeline:

- `src/zil/runtime/adapters/core.clj`: adapter registry and dispatch
- `src/zil/runtime/adapters/rest.clj`: mock/local REST skeleton
- `src/zil/runtime/adapters/file.clj`: file reader (`lines`, `text`, `edn`, `json`, `yaml`, `csv`)
- `src/zil/runtime/adapters/command.clj`: shell command runner (+ optional stdout parsing by `format`)
- `src/zil/runtime/codec.clj`: shared format codec registry (`json`, `yaml`, `csv`, `edn`, `kv`, `text`)
- `src/zil/runtime/ingest.clj`: datasource declaration to fact ingest

## Datasource Declaration Examples

```zc
DATASOURCE ds_rest [type=rest, url="http://127.0.0.1:8090/metrics", format=json, method=get].
DATASOURCE ds_rest_mock [type=rest, mock_response={:metric "metric:latency", :value 120}].
DATASOURCE ds_file [type=file, path="/tmp/metrics.log", mode=lines].
DATASOURCE ds_cmd [type=command, command="printf 'ok'"].
```

Continuous polling:

```zc
DATASOURCE live_metrics
  [type=rest, url="http://127.0.0.1:8090/metrics", format=json, poll_mode=interval, poll_every_ms=1000].
```

And start from runtime code with `start-all-pollers!` / `stop-all-pollers!`.

## Ingest Output Relations

Ingest emits canonical facts such as:

- `datasource:X#ingested_record@value:record [payload=..., ingest_ts=...]`
- `metric:Y#observed_from@datasource:X [value=..., ingest_ts=...]`
- `datasource:X#<field>@value:... [ingest_ts=...]`

This keeps ingestion observational and composable with existing Zil rules/queries.
