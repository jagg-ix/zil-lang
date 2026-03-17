# ZIL + TLA+ + Lean4 Integration (Current Repo Pattern)

This note documents how to use ZIL with the existing SSHX11 TLA+ and Lean4
specifications in this repository.

## What works today

Validated locally in this repo:

```bash
# 1) ZIL model checks
cd zil
clojure -M -m zil.cli bundle-check examples/sshx11-vpn-system.zc lts
clojure -M -m zil.cli bundle-check examples/sshx11-vpn-system.zc constraint

# 2) Python+TLA contract verification
cd ..
python3 tools/verification/verify_sshx11_fsm_python_tla.py \
  --output verification_results/stack_audits/sshx11_fsm_python_tla_validation.json

# 3) TLA extension-set check (schema + optional TLC)
python3 tools/verification/verify_sshx11_extension_set_tla.py
python3 tools/verification/verify_sshx11_extension_set_tla.py --run-tlc --tlc-target temporal

# 4) Lean4 checks
cd verification/lean
lake env lean CAT_EPT/Networking/SSHX11VPNBridgeSpec.lean
lake env lean CAT_EPT/Networking/SSHX11VPNSystemDecomposition.lean
```

## Practical role of each layer

- ZIL: authoring/gating language for model units (`LTS_ATOM`, `POLICY`, etc.).
- TLA+: explicit transition/invariant/trace contract used by verification tooling.
- Lean4: mechanized theorem layer over the protocol/state machine semantics.

## Canonical mapping contract

Use this mapping to avoid semantic drift:

- ZIL `LTS_ATOM` states -> TLA `*States` string members -> Lean `inductive ...State`.
- ZIL `LTS_ATOM` transition label -> TLA transition trigger string -> Lean `...Event`.
- ZIL `LTS_ATOM` transition target -> TLA transition destination -> Lean `step...` branch result.
- ZIL `POLICY condition=...` -> (today) Z3 gate via `constraint` profile.
  - Optional alignment target:
    - TLA `InvariantRules` / dependency-gate obligations
    - Lean theorem obligations (`systemInvariant`, trace lemmas).

## Current limitation (important)

There is now a direct bridge exporter for LTS declarations:

```bash
cd zil
clojure -M -m zil.cli export-tla examples/sshx11-vpn-system.zc
clojure -M -m zil.cli export-tla examples/sshx11-vpn-system.zc /tmp/sshx11_bridge.tla SSHX11BridgeFromZil
clojure -M -m zil.cli export-lean examples/sshx11-vpn-system.zc
clojure -M -m zil.cli export-lean examples/sshx11-vpn-system.zc /tmp/sshx11_bridge.lean Zil.Generated.SSHX11
```

What this bridge emits:

- `<Actor>States`
- `<Actor>Initial`
- `<Actor>Transitions`
- optional `<Actor>TransitionEffects` (when LTS effects are present)
- `<Actor>CanonicalTrace`
- `CanonicalSystemTrace`

Naming controls on each `LTS_ATOM`:

- `actor=SSHClient` to force exported actor prefix (`SSHClientStates`, etc.).
- `actor_key=sshClient` to force trace actor key tuples.

Lean exporter emits per actor:

- `inductive <Actor>State`
- `inductive <Actor>Event`
- `def initial<Actor>State`
- `def step<Actor>`
- `def <Actor>CanonicalTrace`

Still not automated yet:

- theorem/proof generation from ZIL semantics.
- full Zanzibar/profile-specific proof obligation generation.

## Recommended authoring pattern for formal alignment

1. Keep one actor per `LTS_ATOM` (instead of one aggregated machine) when
   targeting the SSHX11 decomposition.
2. Reuse actor names/events already present in:
   - `verification/tla/SSHX11VPNSystemDecomposition.tla`
   - `verification/lean/CAT_EPT/Networking/SSHX11VPNSystemDecomposition.lean`
3. Keep TLA event labels and Lean constructor naming stable; treat renames as
   schema migrations.
4. Run ZIL checks first, then Python+TLA checks, then Lean checks.

## Formal Methods Comparison Matrix

| Formal Goal | Best Path | Command | Why this is best |
|---|---|---|---|
| Check model syntax/shape before formal work | ZIL `bundle-check` (`lts`) | `clojure -M -m zil.cli bundle-check models lts` | Fast structural gate before heavier verification steps. |
| Validate invariant constraints early | ZIL `bundle-check` (`constraint`) | `clojure -M -m zil.cli bundle-check models constraint` | SMT catches inconsistent guard conditions up front. |
| Keep TLA contract synchronized with model vocabulary | `export-tla` | `clojure -M -m zil.cli export-tla models/system.zc /tmp/system.tla ModuleName` | Single-source transition/state naming from `LTS_ATOM`. |
| Validate TLA data contract + extension index | extension-set verifier | `python3 tools/verification/verify_sshx11_extension_set_tla.py` | Confirms required TLA definitions and cross-module coverage. |
| Run actual model checking on temporal contract | TLC run mode | `python3 tools/verification/verify_sshx11_extension_set_tla.py --run-tlc --tlc-target temporal` | Executes TLC instead of only schema/parse checks. |
| Verify behavior-level trace conformance | Python+TLA hybrid verifier | `python3 tools/verification/verify_sshx11_fsm_python_tla.py --output verification_results/...json` | Replays canonical/alt/error traces with strict transition/gate checks. |
| Bootstrap Lean formalization from same model | `export-lean` | `clojure -M -m zil.cli export-lean models/system.zc /tmp/system.lean Zil.Generated.System` | Generates consistent `State`/`Event`/`step` skeletons quickly. |
| Typecheck generated Lean module immediately | Lean check | `cd verification/lean && lake env lean /tmp/system.lean` | Immediate guard against malformed generated terms. |
| Continue with theorem proving on stable vocabulary | Lean authoring over generated skeletons | `lake env lean CAT_EPT/Networking/...` | Keeps proof effort focused on properties, not boilerplate FSM setup. |

Recommended formal workflow:

1. `bundle-check` (`lts`, then `constraint`).
2. `export-tla` and run TLA verifiers (plus TLC when needed).
3. `export-lean` and immediately typecheck with `lake env lean`.
4. Add/maintain proofs on top of generated Lean skeletons.

## Minimal next step to automate

Add proof-obligation scaffolding generation on top of exported Lean skeletons:

- invariant theorem placeholders
- trace-reachability theorem placeholders
- dependency-gate lemma placeholders

Then keep theorem authoring in Lean as the proof layer over generated vocab.
