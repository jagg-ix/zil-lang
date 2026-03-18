# RBAC + DAC Macro Layer

This layer adds a reusable access-control vocabulary on top of ZIL core tuples.

File:

- `lib/rbac-dac-macros.zc`

Runnable model:

- `examples/rbac-dac-orange-book.zc`

## Design Intent

The layer keeps core semantics unchanged and introduces domain macros for:

- discretionary grants by object owner (DAC),
- role permissions and assignments (RBAC),
- request decisions (allow/deny),
- accountability checks (audit record present),
- object reuse checks (allocation requires prior clear marker).

This aligns with Orange Book C2 themes (discretionary security property and accountability) while staying implementation-neutral.

Reference:

- Orange Book (DoD 5200.28-STD): <https://irp.fas.org/nsa/rainbow/std001.htm>

## Macro Surface

Principal/entity constructors:

- `RBD_SUBJECT(id)`
- `RBD_GROUP(id)`
- `RBD_GROUP_MEMBER(group, subject)`
- `RBD_ROLE(id)`
- `RBD_OBJECT(id, owner)`

DAC:

- `RBD_DAC_GRANT_SUBJECT(grant_id, object, action, subject, grantor)`
- `RBD_DAC_GRANT_GROUP(grant_id, object, action, group, grantor)`

RBAC:

- `RBD_ROLE_PERMIT(perm_id, role, object, action)`
- `RBD_ROLE_ASSIGN(binding_id, subject, role)`
- `RBD_SESSION(session_id, subject)`
- `RBD_SESSION_ACTIVATE(session_id, role)`

Requests, audit, reuse:

- `RBD_REQUEST(request_id, subject, object, action)`
- `RBD_REQUEST_SESSION(request_id, session_id)`
- `RBD_AUDIT(record_id, request_id, channel)`
- `RBD_OBJECT_REUSE_CLEARED(object)`
- `RBD_REUSE_ALLOC(alloc_id, object, subject)`

## Rule Semantics (Lowered)

Derived behavior in the library:

- DAC grant validity: only owner-issued grants are valid.
- DAC allow path:
  - direct subject grant
  - group grant with membership
- RBAC allow path:
  - static assignment (`role_binding`)
  - session-activated role
- Decision:
  - allow if any allow path exists
  - default deny otherwise
- Accountability:
  - decision without audit log -> `missing_audit_record`
- Object reuse:
  - allocation without clear marker -> `reuse_without_clear`

## Run

```bash
cd zil
./bin/zil preprocess examples/rbac-dac-orange-book.zc /tmp/rbd.pre.zc
./bin/zil /tmp/rbd.pre.zc
```

Useful queries from the library:

- `rbd_decisions`
- `rbd_allowed_paths`
- `rbd_denied_requests`
- `rbd_grant_violations`
- `rbd_request_policy_violations`
- `rbd_reuse_policy_violations`

## GitHub OSS To Leverage

The layer is intentionally generic so it can map to multiple OSS engines.

1. Open Policy Agent (OPA)  
   Repo: <https://github.com/open-policy-agent/opa>  
   Use with ZIL: compile `request/*`, `role_*`, `dac_grant/*` facts into Rego input for policy evaluation.

2. Gatekeeper (OPA on Kubernetes)  
   Repo: <https://github.com/open-policy-agent/gatekeeper>  
   Use with ZIL: enforce RBAC/DAC constraints as admission-time policies for K8s objects.

3. OpenFGA  
   Repo: <https://github.com/openfga/openfga>  
   Use with ZIL: map `role_binding`, `group member`, and owner grants into tuple relationships for fine-grained checks.

4. SpiceDB  
   Repo: <https://github.com/authzed/spicedb>  
   Use with ZIL: same tuple-relationship bridge as OpenFGA when Zanzibar-style consistency/scale is needed.

5. Ory Keto  
   Repo: <https://github.com/ory/keto>  
   Use with ZIL: relationship API target for ACL/RBAC models, useful for distributed authorization services.

6. Casbin  
   Repo: <https://github.com/casbin/casbin>  
   Use with ZIL: translate canonical facts into Casbin policy lines when embedding auth in app processes.

## Practical Integration Pattern

1. Author source-of-truth constraints in ZIL macros/facts.
2. Run local checks with `./bin/zil`.
3. Export/translate to selected enforcement runtime (OPA/OpenFGA/SpiceDB/Casbin/Keto).
4. Ingest runtime decision/audit artifacts back into ZIL for consistency and gap analysis.
