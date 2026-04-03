# Kubernetes/Helm Compatibility Automation

This workflow generates:

- `lib/k8s-helm-compat-macros.zc`
- `libsets/k8s-helm-compat/k8s-helm-compat-macros.zc`
- `examples/k8s-helm-compat.zc`

from a repeatable script instead of manual editing.

## Generator command

Run from `zil/`:

```bash
python3 tools/generate_k8s_helm_compat.py
```

This creates a runnable scaffold using default sample values.

## Generate from real Helm/CRD inputs

```bash
python3 tools/generate_k8s_helm_compat.py \
  --chart /path/to/Chart.yaml \
  --values /path/to/values.yaml \
  --crd /path/to/crds \
  --template /path/to/rendered-manifests \
  --release prod_migration \
  --namespace platform \
  --required-values image_repository,image_tag,service_port
```

Notes:

- `--crd` and `--template` are repeatable and accept files or directories.
- `--max-values` controls how many flattened values are included in the example.
- a focused libset copy is generated so preprocess can avoid unrelated repo-wide stratification cycles.

## Validate generated model

```bash
./bin/zil preprocess examples/k8s-helm-compat.zc /tmp/k8s_helm_compat.pre.zc libsets/k8s-helm-compat
./bin/zil /tmp/k8s_helm_compat.pre.zc
```

Key generated queries:

- `khc_releases`
- `khc_missing_required_values`
- `khc_drift_candidates`
- `khc_release_requirements`

## Positive + Negative smoke test

```bash
./tools/k8s_helm_compat_smoke.sh
```

This runs:

- positive model: `examples/kubernetes-compat.zc` (expects no missing required values and no drift)
- negative model: `examples/kubernetes-compat-negative.zc` (expects missing required `image_tag` and one drift candidate)
