# ZIL Tooling and Expected Workflows

This guide is the practical "how we operate" reference for ZIL tooling.

It answers:

1. which tools to run,
2. when to run them,
3. what artifacts are expected in normal team workflows.

## Tooling Map

| Tool | Command/API | Use |
|---|---|---|
| Execute one model | `./bin/zil <file.zc>` | Fast local authoring loop for facts/rules/queries. |
| Bundle policy gate | `./bin/zil bundle-check <path> [tm.det\|lts\|constraint]` | Validate a bundle before share/review. |
| Commit-unit policy gate | `./bin/zil commit-check <path> [tm.det\|lts\|constraint] [--allow-mixed]` | Enforce per-file unit contracts in CI. |
| TLA bridge export | `./bin/zil export-tla <path> [output.tla] [module]` | Emit TLA skeleton from `LTS_ATOM` vocabulary. |
| Lean4 bridge export | `./bin/zil export-lean <path> [output.lean] [namespace]` | Emit Lean `State/Event/step` skeletons. |
| TLM formal one-shot pipeline | `./tools/tlm_formal_ci.sh [model] [out_dir] [module] [namespace]` | Run LTS gate + constraint gate + TLA export + Lean export in one command. |
| Runtime ingest one-shot | `zil.runtime.ingest/ingest-all!` | Pull from `DATASOURCE` declarations once. |
| Runtime ingest continuous | `zil.runtime.ingest/start-all-pollers!` | Poll `DATASOURCE` declarations with `poll_mode=interval`. |

## Build Once, Run With Java

When Clojure CLI is not available on runtime hosts:

```bash
cd zil
./bin/build-jar
./bin/zil examples/quickstart-sshx11-beginner.zc
```

`./bin/zil` runs `java -jar dist/zil-standalone.jar` when the jar exists.

## Workflow A: Local Authoring Loop (Default)

Use this when writing or editing models.

1. Create or edit a `.zc` model.
2. Run direct execution.
3. Inspect emitted facts and query rows.
4. Repeat until model behavior matches intent.

Example:

```bash
cd zil
./bin/zil examples/quickstart-sshx11-beginner.zc
./bin/zil examples/sshx11-extension-vscode.zc
```

Expected output shape:

- `:module`
- `:facts`
- `:declarations`
- `:queries`

## Workflow B: Gate Before Share

Use this before opening a PR or sharing a model bundle.

1. Choose profile by intent:
2. `tm.det` for deterministic machine atoms (`TM_ATOM`).
3. `lts` for workflow/state-transition units (`LTS_ATOM`).
4. `constraint` for policy satisfiability (`POLICY` with Z3 checks).

Commands:

```bash
cd zil
./bin/zil bundle-check examples lts
./bin/zil bundle-check examples constraint
```

Note:

- `constraint` profile requires `z3` available in `PATH`.

## Workflow C: CI Merge Gate

Use strict commit-unit checks in CI.

1. Enforce compile success.
2. Enforce exactly one profile unit per `.zc` file.
3. Keep strict mode by default.
4. Use `--allow-mixed` only when needed.

Example CI step:

```bash
cd zil
./bin/zil commit-check models lts
```

Relaxed variant:

```bash
cd zil
./bin/zil commit-check models lts --allow-mixed
```

## Workflow D: Formal Methods Track

Use this when synchronizing with TLA+ and Lean4 work.

1. Run `bundle-check` first (`lts`, then `constraint`).
2. Export TLA from the same model source.
3. Export Lean skeleton from the same model source.
4. Continue model checking/proofs in downstream toolchains.

Commands:

```bash
cd zil
./bin/zil export-tla examples/sshx11-vpn-system.zc /tmp/sshx11.tla SSHX11BridgeFromZil
./bin/zil export-lean examples/sshx11-vpn-system.zc /tmp/sshx11.lean Zil.Generated.SSHX11
```

For the TLM formal bridge example, run the one-shot wrapper:

```bash
cd zil
./tools/tlm_formal_ci.sh
```

## Workflow E: Live Observation / Ingest

Use this to attach models to real data feeds.

1. Define one or more `DATASOURCE` declarations.
2. Compile model.
3. Start pollers.
4. Transact observational facts into DataScript.
5. Stop pollers on shutdown.

Minimal runtime snippet:

```clj
(require '[zil.core :as core]
         '[zil.runtime.datascript :as zr]
         '[zil.runtime.ingest :as ing])

(def compiled (core/compile-program (slurp "examples/quickstart-sshx11-beginner.zc")))
(def conn (zr/make-conn))
(def handles (:handles (ing/start-all-pollers! conn compiled)))
;; ... query conn / evaluate derived state ...
(ing/stop-all-pollers! handles)
```

Polling behavior:

- `poll_mode=event` runs once.
- `poll_mode=interval` runs continuously with `poll_every_ms`.

## Workflow F: Policy and IaC Integration

Use this for plan-time and runtime policy enforcement chains.

1. Generate OpenTofu plan JSON.
2. Evaluate with OPA.
3. Enforce/audit in Gatekeeper (Kubernetes runtime).
4. Ingest decision/audit artifacts into ZIL facts.

Reference:

- `docs/opa-gatekeeper-opentofu-integration.md`

## Expected Team Artifact Flow

Normal artifact lifecycle:

1. `.zc` files are source-of-truth model units.
2. `bundle-check` report validates bundles pre-review.
3. `commit-check` acts as CI merge gate.
4. Exported TLA/Lean files are derived artifacts from the same `LTS_ATOM` source.
5. Ingested runtime facts are observational evidence, not replacements for model source.

## Suggested Repository Conventions

Use a stable structure for predictable automation:

1. `models/` for active `.zc` model units.
2. `examples/` for reference/tutorial inputs.
3. `spec/` for normative language/profile specs.
4. `docs/` for operational guidance and architecture.
5. `verification_results/` for generated run/check evidence (outside normative specs).

## Troubleshooting

Common issues and fixes:

1. `bundle-check ... constraint` fails with solver unavailable.
2. Install Z3 and confirm `z3 -version`.
3. `commit-check` fails unit count.
4. Split mixed declarations into separate files or use `--allow-mixed`.
5. Export commands fail for missing `LTS_ATOM`.
6. Ensure at least one valid `LTS_ATOM` declaration in target input path.

## Recommended Default Workflow

For most teams:

1. Author locally with direct execute command.
2. Run `bundle-check` in pre-commit.
3. Run `commit-check` in CI.
4. Export TLA/Lean from the same model vocabulary when formal tracks are active.
5. Add ingest pollers only for environments that need continuous observation.
