# AWS Overview Modeling Workflow

This workflow uses the AWS whitepaper PDF as a source to build a reusable ZIL baseline and compatibility checks.

## 1. Extract model inputs from PDF

```bash
cd zil
python3 tools/extract_aws_overview_model_inputs.py
```

Generated artifacts:

- `examples/generated/aws-overview-model-inputs.json`
- `examples/generated/aws-overview-model-inputs.zc`

## 2. Execute baseline model

```bash
./bin/zil examples/generated/aws-overview-model-inputs.zc
```

Baseline queries:

- `aws_services`
- `aws_regions`
- `aws_controls`

## 3. Run compatibility overlays (positive + negative)

Add-on models:

- `examples/aws-compat-positive.zc`
- `examples/aws-compat-negative.zc`

One-command smoke test:

```bash
./tools/aws_overview_compat_smoke.sh
```

Expected behavior:

- positive add-on produces `compat_ready` and no `missing_required_*` facts
- negative add-on produces missing markers (service/region/control/config)

