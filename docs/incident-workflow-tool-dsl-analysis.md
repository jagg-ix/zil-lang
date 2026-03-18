# Incident Workflow Tool/DSL Analysis (ZIL)

This model captures incident-response/postmortem workflow requirements and derives:

- recommended tool features
- recommended tool options
- recommended DSL constructs

from requirement tags, source traceability, and implication links.

## Files

- `lib/incident-workflow-macros.zc`
- `examples/incident-workflow-tool-dsl-analysis.zc`

## Run

```bash
./bin/zil preprocess examples/incident-workflow-tool-dsl-analysis.zc /tmp/iwf.pre.zc
./bin/zil /tmp/iwf.pre.zc
```

Key queries:

- `iwf_recommended_features`
- `iwf_recommended_options`
- `iwf_recommended_dsl_constructs`
- `iwf_feature_traceability`
- `iwf_dsl_traceability`
- `iwf_requirement_graph`
- `iwf_requirement_model_links`

## Derived feature set

Must-have:

- `feature:single_owner_lock`
- `feature:role_based_workspace`
- `feature:multi_plane_communications`
- `feature:state_machine_engine`
- `feature:incident_timeline`
- `feature:stakeholder_update_templates`
- `feature:test_gate_engine`
- `feature:fix_proposal_registry`
- `feature:postmortem_workspace`
- `feature:action_item_tracker`
- `feature:invariant_checker`

Should-have:

- `feature:resolution_summary_gate`

## Derived option set

Must-have:

- `option:exclusive_incident_lead_token`
- `option:lead_handoff_requires_ack`
- `option:role_specific_dashboards`
- `option:separate_internal_external_templates`
- `option:channel_to_state_binding`
- `option:strict_transition_validation`
- `option:reject_invalid_state_jumps`
- `option:auto_timeline_capture`
- `option:evidence_hash_for_timeline_entries`
- `option:external_updates_require_state_reference`
- `option:require_fix_risk_blast_radius_rollback_fields`
- `option:enforce_repro_mitigation_rollback_regression_tests`
- `option:block_postmortem_close_on_open_critical_actions`
- `option:postmortem_due_date_policy`
- `option:runtime_invariant_evaluator`

Should-have:

- `option:block_resolve_until_summary_present`

## Derived DSL core blocks

- `dsl:INCIDENT`
- `dsl:STATE`
- `dsl:EVENT`
- `dsl:TRANSITION`
- `dsl:ROLE`
- `dsl:CHANNEL`
- `dsl:PROBLEM`
- `dsl:TEST`
- `dsl:FIX_PROPOSAL`
- `dsl:POSTMORTEM`
- `dsl:ACTION_ITEM`
- `dsl:INVARIANT`
- `dsl:IMPLIES`

## Model-shape blocks for problem solving

- `problem`: `incident_id`, `severity`, `services`, `impact`, `symptoms`, `suspected_causes`, `evidence_refs`, `owner`, `state`
- `test_plan`: `repro_test`, `mitigation_safety_test`, `rollback_test`, `regression_test`, `acceptance_criteria`
- `fix_proposal`: `hypothesis`, `change_plan`, `risk`, `blast_radius`, `rollback`, `validation_signals`, `decision`, `post_deploy_result`
- `postmortem`: `trigger`, `root_cause`, `timeline`, `what_worked`, `what_failed`, `actions_owner_due_status`

