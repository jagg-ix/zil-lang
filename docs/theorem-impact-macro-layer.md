# Theorem Impact Macro Layer

This layer formalizes proof contracts in ZIL and enables backward breakage
analysis from assumptions/signals/components to lemmas/theorems.

Files:

- `lib/theorem-impact-macros.zc`
- `examples/theorem-impact-devops-sre.zc`

## What It Adds

1. theorem/lemma declarations with proof witnesses,
2. explicit dependency DAG edges (`depends_on_direct`, `depends_on`),
3. assumption/signal/component state propagation,
4. theorem status classes:
   - `PROVED`
   - `CONDITIONAL`
   - `BROKEN`
   - `WEAK`
5. backward impact from broken roots (`is_break_root`, `impacted_by_root`).

## Core Modeling Pattern

Think in layers:

1. Runtime health produces component/signal states.
2. Signals support or break assumptions.
3. Assumptions support or break lemmas.
4. Lemmas and assumptions support or break theorems.
5. Break roots explain impacted theorem surfaces.

This turns “what can break?” into a queryable graph instead of a narrative.

## Main Macros

Structure:

- `THM_COMPONENT`, `THM_SIGNAL`, `THM_ASSUMPTION`, `THM_LEMMA`, `THM_THEOREM`

Dependencies:

- `THM_ASSUMPTION_FROM_SIGNAL`
- `THM_LEMMA_REQUIRES_ASSUMPTION`
- `THM_LEMMA_REQUIRES_LEMMA`
- `THM_THEOREM_REQUIRES_ASSUMPTION`
- `THM_THEOREM_REQUIRES_LEMMA`

Evidence:

- `THM_COMPONENT_HEALTHY`, `THM_COMPONENT_BROKEN`
- `THM_SIGNAL_HEALTHY`, `THM_SIGNAL_BROKEN`
- `THM_ASSUME_HOLDS`, `THM_ASSUME_BROKEN`
- `THM_LEMMA_PROOF`, `THM_THEOREM_PROOF`

## Status Semantics

Theorem status is derived as:

1. `BROKEN`: any required assumption/lemma is broken.
2. `PROVED`: proof witness exists and no missing/broken requirements.
3. `CONDITIONAL`: proof witness exists, not broken, but at least one requirement is not currently established.
4. `WEAK`: no proof witness and not broken.

## Backward Breakage Semantics

The layer computes:

1. transitive dependency edges (`depends_on`),
2. broken roots (`is_break_root`) as broken nodes without a broken parent,
3. impacted nodes (`impacted_by_root`) via transitive dependencies.

This supports “if X breaks, what theorems are affected?” analysis directly.

## Run

```bash
cd zil
./bin/zil preprocess examples/theorem-impact-devops-sre.zc /tmp/theorem_impact.pre.zc
./bin/zil /tmp/theorem_impact.pre.zc
```

Useful queries:

- `thm_theorem_statuses`
- `thm_conditional_dependencies`
- `thm_break_roots`
- `thm_impacted_theorems_by_root`
- `thm_dependency_dag`
- `thm_proof_registry`

Demo-focused queries:

- `demo_theorem_statuses`
- `demo_conditional_on`
- `demo_break_roots`
- `demo_impacted_theorems`
