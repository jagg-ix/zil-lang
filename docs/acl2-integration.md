# ACL2 Integration in ZIL

This note documents how ACL2 is integrated into ZIL today and why this shape is used.

## Current ACL2 Presence

ACL2 is already cloned in this workspace at:

- `/Users/macbookpro/lab/tau/tau-information-dynamics/entropic-time/entropic-time/acl2`
- remote: `https://github.com/acl2/acl2.git`

## Design Choice

Use ACL2 through **optional macro and evidence layers**, not as a hard dependency in core language semantics.

Why:

1. ZIL core stays tool-agnostic (`REFINES`, `CORRESPONDS`, `PROOF_OBLIGATION`).
2. ACL2 workflows vary (interactive, books, CI logs, custom wrappers).
3. Evidence ingestion is robust for distributed teams and asynchronous proof exchange.

## What Is Implemented

1. `PROOF_OBLIGATION tool=acl2` is now supported in `proof-obligation-check` using artifact mode:
   - `artifact_in=<path-to-acl2-log>`
   - log markers such as `Q.E.D.` map to `satisfied`
   - error markers map to `violated`
2. Optional command mode is available:
   - `command=<shell command producing ACL2-style output>`
   - optional `artifact_out=<path>` captures stdout/stderr for traceability
3. Backend/FormalFeedback contract now recognizes:
   - `tool:acl2`
   - `capability:check_acl2`
   - `capability:emit_acl2`
4. ACL2 macro layer:
   - `lib/acl2-interop-macros.zc`
   - helper macros for ACL2 obligations and contract wiring

## ACL2 Macro Layer

Main macros:

- `ACL2_CORRESPONDS(id, left, right, refines)`
- `ACL2_OBLIGATION(id, relation, statement, criticality)`
- `ACL2_OBLIGATION_LOG(id, relation, statement, artifact_in, criticality)`
- `ACL2_BACKEND(...)`, `ACL2_BFC_OBLIGATION(...)`, `ACL2_BFC_FEEDBACK(...)`

These are convenience wrappers over canonical ZIL declarations and BFC macros.

## Example

- model: `examples/acl2-proof-obligation-log.zc`
- sample evidence: `examples/data/acl2-proof-ok.log`

Run:

```bash
cd zil
./bin/zil proof-obligation-check examples/acl2-proof-obligation-log.zc acl2
```

## Recommended Usage Pattern

1. Keep formal contract declarations in ZIL (`PROOF_OBLIGATION`).
2. Run ACL2 externally (native ACL2 workflows/books) or through explicit `command=` wrappers.
3. Export/collect logs per obligation.
4. Feed logs back through `artifact_in`.
5. Use `vstack-ci` outputs (`summary_json` + `formal_feedback_zc`) for cross-tool rollups.

This gives a stable integration point without forcing every node/user to have identical ACL2 runtime behavior.
