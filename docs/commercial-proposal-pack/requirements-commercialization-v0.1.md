# Commercialization Requirements v0.1

This requirement set is derived from the latest recommendation set and scoped
to proposal qualification, pilot execution, and scale decisions.

## Functional Requirements

- `REQ-001 Beachhead Selection`:
  The system shall require selecting exactly one primary industry/use case for
  each proposal before pilot approval.
- `REQ-002 Baseline Capture`:
  The system shall capture baseline KPI values and data source references before
  a pilot can start.
- `REQ-003 Pilot Gates`:
  The system shall enforce explicit pilot gates (`start`, `midpoint`,
  `exit`) with pass/fail outcomes.
- `REQ-004 ROI Calculator Use`:
  The system shall require ROI table completion (benefit, cost, payback) before
  commercial decision status can become `approved`.
- `REQ-005 Risk Register`:
  The system shall record top risks, mitigations, and owner for each proposal.
- `REQ-006 KPI Threshold Policy`:
  The system shall support per-industry KPI thresholds and compare actual pilot
  outcomes against those thresholds.
- `REQ-007 Evidence Traceability`:
  The system shall link each claim in the one-pager to measurable evidence
  (metric, report, or artifact path).
- `REQ-008 Packaging Strategy`:
  The system shall record whether the offer is OSS-only, commercial support, or
  enterprise bundle.
- `REQ-009 Pricing Guardrails`:
  The system shall validate that pricing assumptions remain within configured
  min/max ranges by industry profile.
- `REQ-010 Expansion Readiness`:
  The system shall require at least one validated reference pilot before
  multi-team rollout status can be marked `ready`.
- `REQ-011 Operator Summary`:
  The system shall generate an operator-facing decision summary with statuses:
  `GO`, `NO_GO`, `CONDITIONAL`.
- `REQ-012 Audit Snapshot`:
  The system shall export a snapshot including assumptions, KPI deltas, risks,
  and decision status.

## Non-Functional Requirements

- `NFR-001 Repeatability`:
  Re-running evaluation on unchanged inputs shall produce the same decision
  status and same KPI comparison results.
- `NFR-002 Explainability`:
  Every `NO_GO` or `CONDITIONAL` outcome shall include explicit unmet
  requirement IDs.
- `NFR-003 Low Friction`:
  Required input fields shall be limited to essentials needed for qualification
  and payback computation.

## Acceptance Mapping

- Qualification: `REQ-001`, `REQ-002`, `REQ-005`, `REQ-008`
- Pilot governance: `REQ-003`, `REQ-006`, `REQ-007`
- Commercial decision: `REQ-004`, `REQ-009`, `REQ-011`
- Scale and audit: `REQ-010`, `REQ-012`, `NFR-001..003`
