# External Providers in ZIL + HCL/OpenTofu Import

This document defines how ZIL models external providers (similar to
OpenTofu/Terraform provider concepts) and how HCL descriptions are imported.

## External Provider Concept in ZIL

ZIL now supports a first-class declaration:

```zc
PROVIDER aws [source="hashicorp/aws", version="~> 5.0", language=hcl, engine=opentofu].
```

Design intent:

1. separate core language semantics from external execution backends,
2. identify external capability boundaries explicitly,
3. make provider usage queryable and verifiable in model facts.

Lowering behavior:

- `PROVIDER <name> [...]` emits `provider:<name>#kind@entity:provider`.
- Any declaration using `provider=...` or `providers=[...]` is normalized to
  `provider:<name>` references.
- Provider usage emits inverse links: `provider:<name>#provides_for@<entity>`.

Validation behavior:

- `PROVIDER` requires `source=...`.
- provider references must resolve to declared `PROVIDER` entities.

## HCL/OpenTofu Import

A new CLI command imports `.tf`/`.hcl` into executable ZIL text:

```bash
./bin/zil import-hcl <file-or-dir> [output.zc] [module_name]
```

Minimal provider-only example in-repo:

- `examples/provider-external-minimal.zc`

Current importer handles these top-level block types:

- `terraform { required_providers { ... } }`
- `provider "..." { ... }`
- `resource "..." "..." { ... }`
- `data "..." "..." { ... }`
- `module "..." { ... }`
- `variable "..." { ... }`
- `output "..." { ... }`

Output model shape:

- emits `PROVIDER` declarations derived from required/inferred providers,
- emits resource/data/module/variable/output facts,
- links resources/data to provider identities,
- records parsed top-level assignment attributes as `#attr` facts.

## Why this matches OpenTofu/HCL semantics

OpenTofu documents providers as plugins with:

- required provider declaration (`required_providers`) including local name,
  `source`, and optional version constraints,
- provider configuration via `provider "name" { ... }`,
- resource/provider resolution based on local provider names and resource type
  prefixes unless overridden via provider meta-argument.

This maps directly to:

- ZIL `PROVIDER` declarations for provider identity + source/version,
- provider configuration facts on `provider:<name>`,
- resource/data provider edges (`...#provider@provider:<name>`).

## References

- OpenTofu Provider Requirements:
  https://opentofu.org/docs/v1.9/language/providers/requirements/
- OpenTofu Provider Configuration:
  https://opentofu.org/docs/language/providers/configuration/
- HashiCorp HCL Native Syntax Spec:
  https://raw.githubusercontent.com/hashicorp/hcl/main/hclsyntax/spec.md
- KodeKloud OpenTofu/HCL tutorial page (user-provided):
  https://notes.kodekloud.com/docs/OpenTofu-A-Beginners-Guide-to-a-Terraform-Fork-Including-Migration-From-Terraform/Getting-Started-with-OpenTofu/Installing-OpenTofu-and-HashiCorp-Configuraton-Language-HCL-Basics/page#hcl-configuration-files

Note: the KodeKloud page is useful as a tutorial onboarding reference, but
importer semantics here are aligned to primary OpenTofu/HCL sources above.
