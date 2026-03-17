# Reusing `vscode-wolfram` for ZIL

This note evaluates how to reuse the architecture of:

- https://github.com/WolframResearch/vscode-wolfram

for a future ZIL VS Code extension.

## Short answer

Reuse is feasible and high-value, but mostly as an architectural/template reuse,
not as a drop-in dependency.

Best path:

1. reuse extension shell patterns (language contribution, commands, notebook wiring),
2. reuse notebook serializer/queue/output panel patterns,
3. replace Wolfram-specific kernel/LSP transport with ZIL runtime commands and a ZIL LSP service.

## What is directly reusable

From `vscode-wolfram`:

1. VS Code extension wiring (`activate`, command registration, settings flow).
2. Notebook serializer pattern (JSON notebook format with output items).
3. Execution queue pattern for cell scheduling/cancel/end.
4. Output channel/status bar helper patterns.
5. Notebook renderer structure for HTML output payloads.

Relevant files:

- `src/extension/extension.ts`
- `src/extension/serializer.ts`
- `src/extension/notebook-kernel.ts`
- `src/extension/ui-items.ts`
- `src/client/index.ts`
- `package.json` contributions section

## What must be replaced for ZIL

1. Wolfram kernel discovery (`find-kernel.ts`) and path heuristics.
2. Wolfram-specific startup code and message protocol (`resources/init.wl`, ZeroMQ handshake).
3. Wolfram LSP launch command assumptions (`Needs["LSPServer`"]...`).
4. Wolfram language grammar/theme assets and language id.

In ZIL terms this should become:

1. command runner for `clojure -M -m zil.cli ...`,
2. optional long-lived ZIL evaluation service (JSON-RPC or stdio),
3. ZIL LSP server process (diagnostics/completion/hover),
4. ZIL grammar + snippets + semantic tokens (optional later).

## Mapping to current ZIL tooling

Current ZIL commands already map well to editor actions:

1. run file: `clojure -M -m zil.cli <file.zc>`
2. bundle check: `clojure -M -m zil.cli bundle-check <path> <profile>`
3. commit check: `clojure -M -m zil.cli commit-check <path> <profile>`
4. export TLA: `clojure -M -m zil.cli export-tla <path> ...`
5. export Lean: `clojure -M -m zil.cli export-lean <path> ...`

This means a ZIL VS Code extension can provide useful functionality before a full
ZIL LSP is implemented.

## Recommended implementation plan

## Phase 1 (fast MVP, low risk)

1. New extension `vscode-zil` (do not fork full repo yet).
2. Add language id `zil`, file extension `.zc`.
3. Implement commands that call existing CLI commands in integrated terminal/tasks.
4. Add diagnostics by parsing `bundle-check`/`commit-check` output.
5. Add basic syntax grammar (TextMate) for immediate usability.

Outcome: practical editing + validation + export workflows quickly.

## Phase 2 (notebook UX)

1. Reuse serializer/queue/output-channel design from `vscode-wolfram`.
2. Define notebook type (for example `zil-notebook`, extension `.vznb`).
3. Cell execution modes:
4. mode A: evaluate snippets via temporary `.zc` module wrappers,
5. mode B: command cells for `bundle-check`, `export-tla`, etc.

Outcome: structured model exploration in notebook form.

## Phase 3 (language intelligence)

1. Implement `zil-lsp` service in Clojure (or bridge process).
2. Support diagnostics from `compile-program` and declaration validators.
3. Add completion from declaration kinds/attrs, known entities, rule/query variables.
4. Add hover/signature-like help from spec/docs metadata.

Outcome: full IDE-grade language support.

## License and reuse constraints

`vscode-wolfram` top-level license includes permissive terms and bundled Apache-2.0
licensed components. Reuse is allowed, but keep attribution headers and notices in
copied files.

Always preserve upstream copyright/license text when copying source.

## Practical advice

Avoid a hard fork unless you need Wolfram interoperability in the same extension.

For ZIL, a cleaner approach is:

1. scaffold a minimal extension,
2. copy only selected utility modules/patterns,
3. keep a small compatibility note pointing to upstream origin.

This keeps maintenance cost low and avoids carrying Wolfram-specific dependencies
(`zeromq`, kernel bootstrap scripts, rendering assumptions) that ZIL does not need.
