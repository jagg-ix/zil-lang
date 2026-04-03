Feature: Commercial proposal qualification and pilot governance
  As a delivery owner
  I want commercialization decisions to be evidence-based
  So that budget and rollout risk are controlled

  Background:
    Given requirement profile "commercialization-v0.1" is active
    And the proposal pack templates are available

  Scenario Outline: A proposal is qualified with baseline and risk controls
    Given a proposal for "<industry>" with beachhead "<beachhead>"
    And baseline KPIs are recorded
    And risks with owner and mitigation are recorded
    And packaging strategy is "<packaging>"
    When qualification is evaluated
    Then requirement "REQ-001 Beachhead Selection" is satisfied
    And requirement "REQ-002 Baseline Capture" is satisfied
    And requirement "REQ-005 Risk Register" is satisfied
    And requirement "REQ-008 Packaging Strategy" is satisfied
    And qualification status is "GO"

    Examples:
      | industry                | beachhead                  | packaging          |
      | finance-regulated-it    | change-failure-reduction   | enterprise-bundle  |
      | chemical-pharma-process | deviation-reduction        | commercial-support |
      | aeronautical-avionics   | compliance-cycle-reduction | enterprise-bundle  |
      | software-platform-sre   | incident-mttr-reduction    | commercial-support |

  Scenario Outline: Pilot gate fails if KPI threshold is missed
    Given a qualified proposal for "<industry>"
    And ROI table is completed
    And pilot outcome "<kpi>" has actual value "<actual>"
    And threshold for "<kpi>" is "<threshold>"
    When exit gate is evaluated
    Then requirement "REQ-006 KPI Threshold Policy" is violated
    And decision summary status is "NO_GO"
    And unmet requirements include "REQ-006 KPI Threshold Policy"

    Examples:
      | industry             | kpi                 | actual | threshold |
      | finance-regulated-it | mttr_reduction_pct  | 8      | 15        |
      | software-platform-sre| change_failure_rate | 20     | 10        |

  Scenario: Pilot lifecycle defines explicit gates and outcomes
    Given a qualified proposal for "finance-regulated-it"
    And pilot gates "start,midpoint,exit" are defined
    And gate outcomes "start=pass,midpoint=pass,exit=pass" are recorded
    When pilot governance is evaluated
    Then requirement "REQ-003 Pilot Gates" is satisfied

  Scenario: Commercial approval requires ROI and pricing guardrails
    Given a qualified proposal for "software-platform-sre"
    And pilot outcomes satisfy all KPI thresholds
    And ROI calculator has annual benefit, annual cost, and payback months
    And pricing assumptions are within industry guardrails
    When commercial decision is evaluated
    Then requirement "REQ-004 ROI Calculator Use" is satisfied
    And requirement "REQ-009 Pricing Guardrails" is satisfied
    And decision summary status is "GO"

  Scenario: Rollout readiness requires at least one reference pilot
    Given commercial decision status is "GO"
    And there are 0 validated reference pilots
    When rollout readiness is evaluated
    Then requirement "REQ-010 Expansion Readiness" is violated
    And rollout status is "CONDITIONAL"

  Scenario: Audit snapshot is exportable and explainable
    Given a proposal decision has been produced
    When audit snapshot is exported
    Then requirement "REQ-012 Audit Snapshot" is satisfied
    And no-go or conditional decisions include unmet requirement IDs

  Scenario: Claims are traceable to evidence artifacts
    Given a qualified proposal for "aeronautical-avionics"
    And claim "latency_budget_met" links to evidence artifact "artifacts/latency-report.json"
    And claim "uptime_target_met" links to evidence artifact "artifacts/slo-dashboard.csv"
    When evidence traceability is evaluated
    Then requirement "REQ-007 Evidence Traceability" is satisfied

  Scenario: Operator summary exposes status and unmet requirements
    Given operator summary is generated with status "CONDITIONAL"
    And operator summary includes unmet requirement "REQ-006 KPI Threshold Policy"
    When operator summary is evaluated
    Then requirement "REQ-011 Operator Summary" is satisfied
    And decision summary status is "CONDITIONAL"
