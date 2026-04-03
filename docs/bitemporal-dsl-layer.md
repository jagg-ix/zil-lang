# Bitemporal DSL Layer

This layer extends ZIL with language-level support for:

- valid-time and transaction-time axes,
- append-only version history,
- replayable evidence links,
- explicit correction chains,
- explicit as-of replay snapshots.

Primary macro library:

- `lib/bitemporal-macros.zc`

Isolated lib-set (for focused runs):

- `libsets/bitemporal/bitemporal-macros.zc`

Runnable example:

- `examples/bitemporal-history-replay.zc`

## Core macros

- `BT_TIMEPOINT(id, axis, instant)`
- `BT_TIME_BEFORE(left, right)`
- `BT_ASSERTION(id, subject, relation)`
- `BT_VERSION(id, assertion, object, valid_from, tx_from)`
- `BT_VALID_TO(version, valid_to)`
- `BT_TX_TO(version, tx_to)`
- `BT_EVIDENCE(id, source, digest)`
- `BT_EVIDENCE_CAPTURED_AT(evidence, tx_timepoint)`
- `BT_VERSION_EVIDENCE(version, evidence)`
- `BT_CORRECTION(id, assertion, replaces_version, corrected_version, reason)`
- `BT_CORRECTION_AT(id, tx_timepoint)`
- `BT_CORRECTION_EVIDENCE(correction, evidence)`
- `BT_REPLAY_RUN(id, assertion, valid_asof, tx_asof)`
- `BT_ASOF_SNAPSHOT(id, assertion, version, valid_asof, tx_asof)`

## Derived semantics

Rules derive:

- transitive partial order for timepoints (`before`),
- assertion history from typed versions (`history_version`) and corrections (`history_correction`),
- correction supersession chain (`superseded_by`, `replay_next`),
- replayability from evidence links (`replayable`),
- evidence capture tx-time (`evidence_tx`),
- replay edges for each replay run,
- snapshot object projection from selected version.

This layer is monotonic by design. It does not retract facts; corrections and
snapshots are represented explicitly.

## Queries

- `bt_assertion_history_versions`
- `bt_assertion_history_corrections`
- `bt_correction_chain`
- `bt_replayable_versions`
- `bt_replayable_corrections`
- `bt_replay_runs`
- `bt_replay_edges`
- `bt_asof_snapshots`

## Run

Use the isolated bitemporal lib-set for deterministic execution:

```bash
./bin/zil preprocess examples/bitemporal-history-replay.zc /tmp/bt.pre.zc libsets/bitemporal
./bin/zil /tmp/bt.pre.zc
```
