# AWS + Namespace Integration

This layer composes the AWS compatibility model with namespace-scoped resolution.

## Purpose

Without the bridge, `AWS_TARGET_REQ_SERVICE` and `AWS_TARGET_REQ_REGION` check
catalog existence globally.

With the bridge, a target can be scoped to a namespace context and validated
against what is visible in that context.

## Files

- `lib/aws-namespace-bridge-macros.zc`
- `libsets/aws-namespace/aws-model-macros.zc`
- `libsets/aws-namespace/namespace-pn-nd-macros.zc`
- `libsets/aws-namespace/aws-namespace-bridge-macros.zc`
- `examples/aws-namespace-compat.zc`
- `tools/aws_namespace_smoke.sh`

## Bridge macros

- `AWS_NS_SCOPE_TARGET(target, ctx)`
- `AWS_NS_BIND_SERVICE(ctx, local_name, service_id)`
- `AWS_NS_BIND_REGION(ctx, local_name, region_id)`

These macros connect:

- namespace visibility (`?ctx#visible_name@?name`)
- AWS entities (`aws:service:*`, `aws:region:*`)
- scoped target requirements (`?m#scope_ctx@?ctx`)

## Derived scoped checks

- `?m#scoped_missing_required_service@?svc`
- `?m#scoped_missing_required_region@?region`

Any scoped missing requirement marks:

- `?m#has_missing_any@value:true`

which feeds the existing AWS `compat_ready` rule.

## Run

```bash
cd zil
./bin/zil preprocess examples/aws-namespace-compat.zc /tmp/aws_ns.pre.zc libsets/aws-namespace
./bin/zil /tmp/aws_ns.pre.zc
./tools/aws_namespace_smoke.sh
```
