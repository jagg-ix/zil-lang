const assert = require("node:assert/strict");
const {
  Before,
  Given,
  Then,
  When,
  setWorldConstructor,
} = require("@cucumber/cucumber");

const ALLOWED_PACKAGING = new Set([
  "oss-only",
  "commercial-support",
  "enterprise-bundle",
]);

const REQUIREMENT_IDS = new Set([
  "REQ-001",
  "REQ-002",
  "REQ-003",
  "REQ-004",
  "REQ-005",
  "REQ-006",
  "REQ-007",
  "REQ-008",
  "REQ-009",
  "REQ-010",
  "REQ-011",
  "REQ-012",
]);

function requirementId(label) {
  const m = String(label).match(/REQ-\d{3}/);
  assert.ok(m, `Missing REQ id in label: ${label}`);
  assert.ok(REQUIREMENT_IDS.has(m[0]), `Unknown requirement id: ${m[0]}`);
  return m[0];
}

function normalizeStatus(value) {
  return String(value || "").trim().toUpperCase();
}

function isHigherBetter(kpi) {
  return String(kpi).includes("reduction");
}

class CommercializationWorld {
  constructor() {
    this.profile = null;
    this.templatesAvailable = false;
    this.proposal = null;
    this.qualificationStatus = null;
    this.reqStatus = {};
    this.roi = null;
    this.pilotOutcomes = {};
    this.thresholds = {};
    this.decisionSummaryStatus = null;
    this.unmet = new Set();
    this.pricingWithinGuardrails = false;
    this.pilotAllThresholdsSatisfied = false;
    this.commercialDecisionStatus = null;
    this.referencePilots = 0;
    this.rolloutStatus = null;
    this.auditExported = false;
    this.pilotGates = [];
    this.gateOutcomes = {};
    this.claimEvidence = new Map();
    this.operatorSummary = null;
  }

  resetDerived() {
    this.reqStatus = {};
    this.decisionSummaryStatus = null;
    this.unmet = new Set();
  }

  setRequirement(id, satisfied) {
    this.reqStatus[id] = Boolean(satisfied);
    if (!satisfied) this.unmet.add(id);
  }
}

setWorldConstructor(CommercializationWorld);

Before(function () {
  this.resetDerived();
});

Given("requirement profile {string} is active", function (profile) {
  this.profile = profile;
  assert.equal(profile, "commercialization-v0.1");
});

Given("the proposal pack templates are available", function () {
  this.templatesAvailable = true;
});

Given(
  "a proposal for {string} with beachhead {string}",
  function (industry, beachhead) {
    this.proposal = {
      industry,
      beachhead,
      baselineRecorded: false,
      risksRecorded: false,
      packaging: null,
      qualified: false,
    };
  }
);

Given("baseline KPIs are recorded", function () {
  assert.ok(this.proposal, "proposal must be initialized");
  this.proposal.baselineRecorded = true;
});

Given("risks with owner and mitigation are recorded", function () {
  assert.ok(this.proposal, "proposal must be initialized");
  this.proposal.risksRecorded = true;
});

Given("packaging strategy is {string}", function (packaging) {
  assert.ok(this.proposal, "proposal must be initialized");
  this.proposal.packaging = packaging;
});

When("qualification is evaluated", function () {
  assert.ok(this.templatesAvailable, "templates must be available");
  assert.ok(this.proposal, "proposal must exist");

  const hasSingleBeachhead =
    typeof this.proposal.beachhead === "string" &&
    this.proposal.beachhead.trim().length > 0 &&
    !this.proposal.beachhead.includes(",");
  const hasBaseline = this.proposal.baselineRecorded;
  const hasRiskRegister = this.proposal.risksRecorded;
  const validPackaging = ALLOWED_PACKAGING.has(this.proposal.packaging);

  this.setRequirement("REQ-001", hasSingleBeachhead);
  this.setRequirement("REQ-002", hasBaseline);
  this.setRequirement("REQ-005", hasRiskRegister);
  this.setRequirement("REQ-008", validPackaging);

  const pass = hasSingleBeachhead && hasBaseline && hasRiskRegister && validPackaging;
  this.proposal.qualified = pass;
  this.qualificationStatus = pass ? "GO" : "NO_GO";
});

Given("a qualified proposal for {string}", function (industry) {
  this.proposal = {
    industry,
    beachhead: "prequalified",
    baselineRecorded: true,
    risksRecorded: true,
    packaging: "commercial-support",
    qualified: true,
  };
  this.qualificationStatus = "GO";
});

Given("ROI table is completed", function () {
  this.roi = {
    annualBenefit: 100000,
    annualCost: 50000,
    paybackMonths: 6,
  };
});

Given(
  "pilot outcome {string} has actual value {string}",
  function (kpi, actual) {
    this.pilotOutcomes[kpi] = Number(actual);
  }
);

Given("threshold for {string} is {string}", function (kpi, threshold) {
  this.thresholds[kpi] = Number(threshold);
});

When("exit gate is evaluated", function () {
  const keys = Object.keys(this.thresholds);
  assert.ok(keys.length > 0, "at least one threshold is required");

  const allPass = keys.every((kpi) => {
    const actual = this.pilotOutcomes[kpi];
    const threshold = this.thresholds[kpi];
    assert.ok(Number.isFinite(actual), `missing actual for ${kpi}`);
    assert.ok(Number.isFinite(threshold), `missing threshold for ${kpi}`);
    return isHigherBetter(kpi) ? actual >= threshold : actual <= threshold;
  });

  this.setRequirement("REQ-006", allPass);
  this.decisionSummaryStatus = allPass ? "GO" : "NO_GO";
});

Given("pilot gates {string} are defined", function (gatesCsv) {
  this.pilotGates = String(gatesCsv)
    .split(",")
    .map((x) => x.trim())
    .filter(Boolean);
});

Given("gate outcomes {string} are recorded", function (pairsCsv) {
  const pairs = String(pairsCsv)
    .split(",")
    .map((x) => x.trim())
    .filter(Boolean);
  this.gateOutcomes = {};
  for (const pair of pairs) {
    const [gate, outcome] = pair.split("=").map((x) => String(x || "").trim());
    assert.ok(gate.length > 0, `invalid gate outcome pair: ${pair}`);
    this.gateOutcomes[gate] = outcome;
  }
});

When("pilot governance is evaluated", function () {
  const expected = ["start", "midpoint", "exit"];
  const hasAllExpected = expected.every((g) => this.pilotGates.includes(g));
  const outcomesValid = expected.every(
    (g) => this.gateOutcomes[g] === "pass" || this.gateOutcomes[g] === "fail"
  );
  this.setRequirement("REQ-003", hasAllExpected && outcomesValid);
});

Given("pilot outcomes satisfy all KPI thresholds", function () {
  this.pilotAllThresholdsSatisfied = true;
});

Given(
  "ROI calculator has annual benefit, annual cost, and payback months",
  function () {
    this.roi = {
      annualBenefit: 120000,
      annualCost: 60000,
      paybackMonths: 8,
    };
  }
);

Given("pricing assumptions are within industry guardrails", function () {
  this.pricingWithinGuardrails = true;
});

When("commercial decision is evaluated", function () {
  assert.ok(this.proposal && this.proposal.qualified, "proposal must be qualified");
  const roiComplete =
    this.roi &&
    Number.isFinite(this.roi.annualBenefit) &&
    Number.isFinite(this.roi.annualCost) &&
    Number.isFinite(this.roi.paybackMonths);

  this.setRequirement("REQ-004", roiComplete);
  this.setRequirement("REQ-009", this.pricingWithinGuardrails);

  const pass =
    this.pilotAllThresholdsSatisfied && roiComplete && this.pricingWithinGuardrails;
  this.decisionSummaryStatus = pass ? "GO" : "NO_GO";
  this.commercialDecisionStatus = this.decisionSummaryStatus;
});

Given("commercial decision status is {string}", function (status) {
  this.commercialDecisionStatus = normalizeStatus(status);
});

Given("there are {int} validated reference pilots", function (count) {
  this.referencePilots = count;
});

When("rollout readiness is evaluated", function () {
  const ready =
    this.commercialDecisionStatus === "GO" && this.referencePilots >= 1;
  this.setRequirement("REQ-010", ready);
  this.rolloutStatus = ready ? "GO" : "CONDITIONAL";
});

Given("a proposal decision has been produced", function () {
  this.decisionSummaryStatus = "CONDITIONAL";
  this.unmet.add("REQ-006");
});

When("audit snapshot is exported", function () {
  this.auditExported = true;
  this.setRequirement("REQ-012", true);
});

Given(
  "claim {string} links to evidence artifact {string}",
  function (claim, artifactPath) {
    this.claimEvidence.set(String(claim), String(artifactPath));
  }
);

When("evidence traceability is evaluated", function () {
  const hasClaims = this.claimEvidence.size > 0;
  const allArtifactsPresent = [...this.claimEvidence.values()].every(
    (path) => typeof path === "string" && path.trim().length > 0
  );
  this.setRequirement("REQ-007", hasClaims && allArtifactsPresent);
});

Given("operator summary is generated with status {string}", function (status) {
  this.operatorSummary = {
    status: normalizeStatus(status),
    unmet: new Set(),
  };
});

Given(
  "operator summary includes unmet requirement {string}",
  function (requirementLabel) {
    assert.ok(this.operatorSummary, "operator summary must be initialized");
    const id = requirementId(requirementLabel);
    this.operatorSummary.unmet.add(id);
  }
);

When("operator summary is evaluated", function () {
  assert.ok(this.operatorSummary, "operator summary must be initialized");
  const status = this.operatorSummary.status;
  const validStatus = ["GO", "NO_GO", "CONDITIONAL"].includes(status);
  const unmetValid =
    status === "GO" || this.operatorSummary.unmet.size > 0;
  this.setRequirement("REQ-011", validStatus && unmetValid);
  this.decisionSummaryStatus = status;
  if (status !== "GO") {
    for (const id of this.operatorSummary.unmet) {
      this.unmet.add(id);
    }
  }
});

Then("requirement {string} is satisfied", function (label) {
  const id = requirementId(label);
  assert.equal(this.reqStatus[id], true, `${id} expected to be satisfied`);
});

Then("requirement {string} is violated", function (label) {
  const id = requirementId(label);
  assert.equal(this.reqStatus[id], false, `${id} expected to be violated`);
});

Then("qualification status is {string}", function (status) {
  assert.equal(this.qualificationStatus, normalizeStatus(status));
});

Then("decision summary status is {string}", function (status) {
  assert.equal(this.decisionSummaryStatus, normalizeStatus(status));
});

Then("unmet requirements include {string}", function (label) {
  const id = requirementId(label);
  assert.ok(this.unmet.has(id), `${id} expected in unmet requirements`);
});

Then("rollout status is {string}", function (status) {
  assert.equal(this.rolloutStatus, normalizeStatus(status));
});

Then(
  "no-go or conditional decisions include unmet requirement IDs",
  function () {
    const status = normalizeStatus(this.decisionSummaryStatus);
    if (status === "NO_GO" || status === "CONDITIONAL") {
      assert.ok(this.unmet.size > 0, "expected unmet requirement IDs");
    }
  }
);
