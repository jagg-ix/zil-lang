# Language Design Notes

## Core vs Macro Rule

Core should define semantics and correctness contracts.
Macros should provide syntax sugar and reusable templates that expand into core forms.

Core examples:
- causal semantics
- constraint semantics
- aggregation semantics

Macro examples:
- IT DSL convenience forms (`DATACENTER`, `RULESET`, `MIRROR_CHECK`)
- policy shorthand expansion

