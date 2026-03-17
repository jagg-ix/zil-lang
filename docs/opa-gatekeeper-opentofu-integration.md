# Integrating ZIL with OPA, Gatekeeper, and OpenTofu

This document describes how to integrate ZIL with:

- OPA (policy decision engine)
- Gatekeeper (Kubernetes admission/audit policy controller built on OPA)
- OpenTofu (IaC planning/apply engine)

## Integration intent

Use each system for what it does best:

- OpenTofu: computes intended infrastructure change set (`plan`).
- OPA: evaluates policy decisions over JSON inputs.
- Gatekeeper: enforces policy continuously in Kubernetes admission + audit loops.
- ZIL: captures cross-system model state, rules, evidence, and collaboration history.

## Official integration surfaces (current)

- OPA REST APIs (`/v1/data`, policy/data/query/compile/health):  
  `https://www.openpolicyagent.org/docs/rest-api`
- OPA bundles/discovery for fleet policy distribution:  
  `https://www.openpolicyagent.org/docs/management-bundles`  
  `https://www.openpolicyagent.org/docs/management-discovery`
- OPA Terraform plan policy workflow (applicable to OpenTofu JSON plan pattern):  
  `https://www.openpolicyagent.org/docs/terraform`
- Gatekeeper ConstraintTemplates + Constraints + audit + external data:
  - `https://open-policy-agent.github.io/gatekeeper/website/docs/constrainttemplates/`
  - `https://open-policy-agent.github.io/gatekeeper/website/docs/audit/`
  - `https://open-policy-agent.github.io/gatekeeper/website/docs/externaldata/`
  - `https://open-policy-agent.github.io/gatekeeper/website/docs/gator/`
- Gatekeeper policy library:
  `https://github.com/open-policy-agent/gatekeeper-library`
- OpenTofu plan/show/json interfaces:
  - `https://opentofu.org/docs/cli/commands/plan/`
  - `https://opentofu.org/docs/cli/commands/show/`
  - `https://opentofu.org/docs/internals/json-format/`

## Best-practice architecture

1. OpenTofu creates a saved plan artifact.
2. OpenTofu plan is converted to JSON.
3. ZIL ingests plan JSON + context metadata as facts.
4. OPA evaluates allow/deny/reasons over normalized plan input.
5. ZIL ingests OPA decision output and correlates with commit/run/session facts.
6. If target is Kubernetes, Gatekeeper enforces runtime admission/audit policy.
7. ZIL ingests Gatekeeper audit/violation exports for closed-loop drift visibility.

## Concrete pipeline (recommended starting point)

### A) OpenTofu -> JSON

```bash
tofu plan -out=tfplan.bin
tofu show -json tfplan.bin > tfplan.json
```

### B) OPA decision over plan JSON

Two common modes:

1. CLI mode:

```bash
opa eval -d policy.rego -i tfplan.json 'data.zil.tofu.deny'
```

2. REST mode:

```bash
curl -sS -X POST http://127.0.0.1:8181/v1/data/zil/tofu/deny \
  -H 'Content-Type: application/json' \
  -d @input.json
```

### C) ZIL ingestion/correlation

Use ZIL datasource adapters (`file`, `rest`, `command`) to ingest:

- `tfplan.json` artifacts
- OPA decision payloads
- Gatekeeper audit/export payloads

Current adapter/runtime entrypoints:

- `src/zil/runtime/adapters/file.clj`
- `src/zil/runtime/adapters/rest.clj`
- `src/zil/runtime/adapters/command.clj`
- `src/zil/runtime/ingest.clj`

## Gatekeeper-specific integration model

Gatekeeper is not a replacement for OpenTofu plan-time checks.
Use Gatekeeper for Kubernetes runtime enforcement/audit, and OPA for plan-time IaC checks.

Recommended split:

- Plan-time IaC policy: OpenTofu JSON + OPA.
- Runtime Kubernetes policy: Gatekeeper ConstraintTemplates/Constraints.
- Shift-left template validation: `gator` in CI.
- Optional dynamic context in Gatekeeper: external data provider.

## Mapping ZIL to OPA/Gatekeeper/OpenTofu concepts

| ZIL Concept | OPA / Gatekeeper / OpenTofu Mapping |
|---|---|
| `POLICY` declaration | Rego rule intent (`allow/deny/violation`) or Gatekeeper ConstraintTemplate+Constraint behavior |
| `DATASOURCE` | plan JSON artifact, OPA decision endpoint, Gatekeeper audit/export endpoint |
| `EVENT` | plan run/apply run/audit cycle metadata |
| `LTS_ATOM` | workflow state machine for policy pipeline stages (`planned`, `evaluated`, `approved`, `applied`, `audited`) |
| query/rule outputs | release gates, exception reports, compliance evidence facts |

## What to implement next in ZIL (high impact)

1. OpenTofu plan normalizer profile:
   - flatten plan `resource_changes` into canonical facts.
2. Rego export profile:
   - generate starter Rego modules from selected ZIL policies/rules.
3. OPA decision ingest profile:
   - normalized facts for decision result, reasons, and `decision_id`.
4. Gatekeeper artifact generator:
   - produce ConstraintTemplate/Constraint YAML skeletons from policy library metadata.
5. CI commands:
   - `zil check`
   - `zil eval`
   - `zil delta`
   - `zil gate opa`
   - `zil gate gatekeeper`

## Security and operations notes

- `tofu show -json` can include sensitive values in plain text; treat artifacts as secrets.
- Prefer OPA bundles/discovery for multi-instance policy distribution instead of ad-hoc pushes.
- In Gatekeeper external data, keep provider timeouts short and batch keys.
- Use Gatekeeper audit exports for periodic compliance evidence, not only admission-time decisions.
