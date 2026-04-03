# Leveraging Crux for Migration-Sync Projects

This note explains how `err/crux` (Crux bitemporal DB) can support
`projects/fts-sync-migration`.

Crux repository inspected: `https://github.com/err/crux.git` (local clone for
analysis).

## Why Crux fits this migration use case

Key Crux strengths relevant to FTS/system migration projects:

- bitemporal model (`valid-time` + `tx-time`) for late-arriving data,
  backfills, and corrections,
- immutable transaction-log centric architecture for auditable history,
- Datalog query interface for graph-like operational joins,
- HTTP endpoints for status/query/tx submission and entity history.

This maps well to migration operations where observations may arrive out of
order and "current state" and "state as known at time T" must both be
explainable.

## Practical architecture with ZIL

Recommended split:

1. Crux stores event/evidence documents and historical corrections.
2. ZIL project models (`SYNC_*`, `SYNC_TLM_*`) express sync semantics and
   policy checks.
3. ZIL runtime ingest pulls from Crux HTTP endpoints via `DATASOURCE type=rest`
   and materializes normalized tuple facts.

Use Crux for:

- authoritative temporal evidence and audit replay,
- conflict-safe update patterns (`match` operations),
- long-lived history queries (`entity?history=true`).

Use ZIL for:

- domain DSL and migration-specific reasoning,
- out-of-sync detection and policy interpretation,
- cross-source model composition (Crux + other sources).

## Endpoint surface to use first

From Crux HTTP docs:

- `GET /_crux/status`
- `GET /_crux/sync`
- `GET /_crux/latest-completed-tx`
- `GET/POST /_crux/query`
- `POST /_crux/submit-tx`
- `GET /_crux/entity?history=true`

For writes, use `submit-tx` operations (`put/delete/match/evict/fn`), with
`match` to enforce optimistic consistency on migration state updates.

## ZIL model added for this

- `models/examples/system-sync-migration-crux-interop.zc`

This model declares:

- Crux provider binding (`PROVIDER crux_main`),
- rest datasources for status/sync/latest-tx/query/history,
- integration guardrail policies,
- discovery queries for provider + datasource contracts.

## Suggested operational loop

1. Ingest Crux status + query views on interval.
2. Join ingested evidence with `SYNC_*` model outputs.
3. Trigger alerts on `sync_out_of_sync_links`.
4. Drill into Crux entity history for forensic explanation.
5. Optionally write adjudicated state/evidence pointers back to Crux via
   `submit-tx`.

## Notes

- Crux is now known as XTDB in newer ecosystem contexts, but this repo is the
  Crux codebase and APIs as documented there.
- Start with read-side integration first (`status/sync/query/history`), then
  add write-side workflows.
