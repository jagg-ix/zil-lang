# Git-Native Model Workflow Plan for Zil

## Goal

Use Git as the transport and audit layer where each commit represents a formal
model change (problem statement, hypothesis, evidence, solution, validation).

Zil provides the language + evaluator so commits are machine-checkable, not just text diffs.

## Core Idea

- A commit is a model transition: `M(n) -> M(n+1)`.
- Zil files encode typed declarations, rules, queries, and evidence links.
- CI evaluates each transition and publishes verdict artifacts.

## Repository Shape

```text
models/
  domains/
  systems/
  incidents/
evidence/
  datasource/
  captures/
queries/
checks/
profiles/
reports/
```

Recommended file roles:

- `models/**.zc`: canonical facts/rules/declarations
- `evidence/**`: raw or normalized data references
- `checks/**.zc`: invariants/SLO constraints/formal checks
- `reports/**`: generated evaluation output per commit/PR

## Commit Contract (Policy)

Each model-changing commit should include trailers:

- `Zil-Change-Type: problem|hypothesis|evidence|solution|refactor`
- `Zil-Model-Scope: <path-or-domain>`
- `Zil-Why: <short rationale>`
- `Zil-Expected-Effect: <expected model/query change>`

Optionally:

- `Zil-Evidence-Ref: evidence/...`
- `Zil-Related-Issue: #...`

## PR/CI Evaluation Pipeline

For each PR:

1. Parse + lower all changed Zil files.
2. Validate declaration graph (deps, enums, references).
3. Run ingest snapshots for declared datasources (mock in CI, real in controlled env).
4. Execute rules/queries/checks on base and head commits.
5. Compute semantic delta:
   - added/removed facts
   - changed query answers
   - invariant pass/fail deltas
6. Post a structured report into `reports/` + PR comment.

## Semantic Delta Model

Represent commit impact as facts:

- `commit:<sha>#adds_fact@fact:<id>`
- `commit:<sha>#removes_fact@fact:<id>`
- `commit:<sha>#changes_query@query:<name>`
- `commit:<sha>#violates@check:<name>`

This makes Git history directly queryable with Zil itself.

## Human Workflow (Problem -> Solution)

1. Create branch for one problem thread.
2. Add/modify model facts and hypothesis rules.
3. Attach or reference evidence datasources/captures.
4. Add checks expressing expected behavior.
5. Run local evaluator and inspect semantic delta.
6. Commit with model trailers.
7. Open PR; CI validates and compares with mainline model.
8. Merge when semantic checks and review pass.

## Tooling Surface (Incremental)

Planned CLI commands:

- `zil check` : parse/lower/validate
- `zil eval` : execute queries/checks
- `zil ingest` : one-shot or interval ingest for datasource sets
- `zil delta <base> <head>` : semantic diff between commits
- `zil report` : render machine + human readable summary

## Why This Works Better Than Plain Git Diffs

- commits carry executable meaning, not only textual edits
- formal checks run on every change
- evidence is tied to model state and reviewable in history
- hypotheses and solutions are comparable as query/check outcomes
- asynchronous contributors still converge on one coherent model
