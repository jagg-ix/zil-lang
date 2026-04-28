# Zil Lang

Zil Lang (short name: Zil) is a declarative tuple-and-rule language for general knowledge modeling,
verification, and policy reasoning.

This repository now includes both:
- language design artifacts (normative specs), and
- an initial Clojure + DataScript runtime scaffold that executes core ideas.

## Scope and Architecture

ZIL core is domain-agnostic and general-purpose.

Current built-in declarations are IT-oriented because they are the primary
immediate use case in this repository, but they are layered above the core
tuple/rule semantics and do not define language boundaries.

The declaration layer includes an explicit external `PROVIDER` concept so
models can reference external systems/adapters (for example OpenTofu/HCL
providers) without hardwiring provider semantics into the core language.

## DataScript Leverage

We leverage the Clojure DataScript engine as a concrete runtime target:
- immutable in-memory DB values for snapshot-friendly semantics,
- Datalog query/rule execution as the primary evaluation model,
- tuple attributes and unique constraints for composite identity and integrity,
- direct support for recursive rules and stratified negation patterns.

See:
- `spec/runtime-datascript-profile-v0.1.md`
- `docs/datascript-leverage.md`
- `src/zil/runtime/datascript.clj`

## Quick Start (Clojure)

```bash
clojure -M -e "(require 'zil.runtime.datascript) (println :ok)"
```

Core engine load:

```bash
clojure -M -e "(require 'zil.core) (println :ok)"
```

Minimal usage:

```clj
(require '[zil.runtime.datascript :as zr])

(def conn (zr/make-conn))

(zr/transact-facts! conn
  [{:object "app:svc1"
    :relation :depends_on
    :subject "service:db1"
    :revision 1
    :event :e1}
   {:object "service:db1"
    :relation :available
    :subject "value:true"
    :revision 1
    :event :e1}])

(zr/facts-at-or-before @conn 1)
```

Run a `.zc` file through the core engine:

```bash
clojure -M -m zil.cli examples/it-infra-minimal.zc
```

Import HCL/OpenTofu descriptions into ZIL:

```bash
clojure -M -m zil.cli import-hcl path/to/infra/ [output.zc] [module_name]
./bin/zil import-hcl path/to/infra/ /tmp/infra-imported.zc hcl.import.infra
```

Import external JSON/YAML/CSV into generated ZIL facts:

```bash
./bin/zil import-data examples/data/interop-sample.json /tmp/interop_from_json.zc interop.import json
./bin/zil import-data examples/data/interop-sample.yaml /tmp/interop_from_yaml.zc interop.import yaml
./bin/zil import-data examples/data/interop-sample.csv /tmp/interop_from_csv.zc interop.import csv
```

Export model outputs in JSON/YAML/CSV:

```bash
./bin/zil export-data examples/interop-json-yaml-csv.zc json /tmp/zil_queries.json queries
./bin/zil export-data examples/interop-json-yaml-csv.zc yaml /tmp/zil_queries.yaml queries
./bin/zil export-data examples/interop-json-yaml-csv.zc csv /tmp/service_states.csv service_states
```

Generate Kubernetes/Helm compatibility macro layer + runnable example automatically:

```bash
python3 tools/generate_k8s_helm_compat.py
./bin/zil preprocess examples/k8s-helm-compat.zc /tmp/k8s_helm_compat.pre.zc libsets/k8s-helm-compat
./bin/zil /tmp/k8s_helm_compat.pre.zc
./tools/k8s_helm_compat_smoke.sh
```

Extract AWS whitepaper model inputs and run AWS compatibility smoke:

```bash
python3 tools/extract_aws_overview_model_inputs.py
./bin/zil examples/generated/aws-overview-model-inputs.zc
./tools/aws_overview_compat_smoke.sh
./tools/aws_extension_icons_smoke.sh
```



## Standalone Runtime (No Clojure CLI Needed)

Build once (requires Clojure tooling on build machine):

```bash
cd zil
./bin/build-jar
```

Run anywhere with Java only:

```bash
cd zil
./bin/zil examples/it-infra-minimal.zc
./bin/zil bundle-check examples lts
./bin/zil export-tla examples/sshx11-vpn-system.zc /tmp/sshx11_bridge.tla SSHX11BridgeFromZil
java -jar dist/zil-standalone.jar bundle-check examples/quickstart-sshx11-beginner.zc lts
```

Notes:

- `./bin/zil` prefers `dist/zil-standalone.jar` when present.
- if jar is missing, it falls back to `clojure -M -m zil.cli`.
- if neither Java+jar nor Clojure is available, it prints a build hint.

SSHX11 + VS Code extension-host modeling example:

```bash
./bin/zil examples/sshx11-extension-vscode.zc
./bin/zil bundle-check examples/sshx11-extension-vscode.zc lts
./bin/zil bundle-check examples/sshx11-extension-vscode.zc constraint
```

Native host + WASM/JS screen automation modeling example:

```bash
./bin/zil examples/native-host-wasm-screen-automation.zc
./bin/zil bundle-check examples/native-host-wasm-screen-automation.zc lts
./bin/zil bundle-check examples/native-host-wasm-screen-automation.zc constraint
```

Declarative config macro-layer example:

```bash
./bin/zil preprocess examples/config-declarative-macros.zc /tmp/config.pre.zc libsets/config-declarative
./bin/zil /tmp/config.pre.zc
```

Transaction-level modeling (TLM) macro-layer example:

```bash
./bin/zil examples/tlm-domain-macros.zc
```

TLM formal backend bridge example (Z3/TLA+/Lean4):

```bash
./bin/zil bundle-check examples/tlm-formal-bridge.zc lts
./bin/zil bundle-check examples/tlm-formal-bridge.zc constraint
./bin/zil export-tla examples/tlm-formal-bridge.zc /tmp/tlm_bridge.tla TLMBridgeFromZil
./bin/zil export-lean examples/tlm-formal-bridge.zc /tmp/tlm_bridge.lean Zil.Generated.TLM
```

## Native Macro System

Zil has its own language-level macro system (independent of Clojure macros):

```zc
MACRO link_pair(a,b):
EMIT {{a}}#connected_to@{{b}}.
EMIT {{b}}#connected_to@{{a}}.
ENDMACRO.

USE link_pair(location:dcA, location:dcB).
```

Rules:
- define with `MACRO name(params): ... ENDMACRO.`
- each body line is `EMIT ...`
- invoke with `USE name(args).`
- placeholders are `{{param}}`

Domain library layer examples using macros:

- `docs/domain-library-macros.md`
- `examples/domain-library-macros.zc`
- `docs/tlm-macro-layer.md`
- `lib/tlm-macros.zc`
- `examples/tlm-domain-macros.zc`
- `examples/tlm-formal-bridge.zc`
- `examples/vstack-refinement-minimal.zc`
- `docs/tpl-macro-layer.md`
- `examples/tpl-macro-layer.zc`
- `docs/pi-calculus-vc-macro-layer.md`
- `examples/pi-calculus-vc-macro-layer.zc`
- `docs/temporal-pi-quickstart.md`
- `examples/temporal-pi-quickstart.zc`
- `docs/petrinet-nd-macro-layer.md`
- `examples/petrinet-nd-macro-layer.zc`
- `docs/provider-model-hcl-import.md`
- `docs/json-yaml-csv-interop-layer.md`
- `lib/interop-macros.zc`
- `examples/interop-json-yaml-csv.zc`
- `docs/config-declarative-macro-layer.md`
- `lib/config-declarative-macros.zc`
- `libsets/config-declarative/config-declarative-macros.zc`
- `examples/config-declarative-macros.zc`
- `docs/backend-formal-contract.md`
- `lib/backend-formal-contract-macros.zc`
- `lib/acl2-interop-macros.zc`
- `examples/backend-formal-contract-demo.zc`
- `examples/acl2-proof-obligation-log.zc`
- `examples/provider-external-minimal.zc`
- `docs/needs-provider-macro-layer.md`
- `lib/needs-provider-macros.zc`
- `examples/needs-provider-macro-layer.zc`
- `docs/request-form-modeling.md`
- `lib/request-form-macros.zc`
- `docs/rbac-dac-macro-layer.md`
- `lib/rbac-dac-macros.zc`
- `examples/rbac-dac-orange-book.zc`
- `docs/theorem-impact-macro-layer.md`
- `lib/theorem-impact-macros.zc`
- `examples/theorem-impact-devops-sre.zc`
- `examples/request-form-recursive.zc`



## Current Status

- Core: Draft
- Time model: Draft (causal core + optional time profiles)
- Zanzibar compatibility profile: Draft
- DataScript runtime profile: Draft
