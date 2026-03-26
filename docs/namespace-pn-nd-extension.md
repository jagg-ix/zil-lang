# Namespace + PetriNet + N-D Extension

This extension adds a namespace-specific domain layer to ZIL, based on:

- hierarchical namespaces,
- n-dimensional namespace composition (product contexts),
- category-style morphism reachability,
- Petri-net style dynamic resolution (token + transition witnesses).

It is derived from the concepts discussed in:

- `$HOME/Downloads/petrinets-namespaces-ndimensional.md`

## Files

- `lib/namespace-pn-nd-macros.zc`
- `libsets/namespace-pn-nd/namespace-pn-nd-macros.zc`
- `examples/namespace-pn-nd-extension.zc`
- `tools/namespace_pn_nd_smoke.sh`

## Core macros

- namespace dimensions and hierarchy:
  - `NS_DIMENSION`, `NS_DIM_SUCCESSOR`, `NS_CONTEXT`, `NS_CHILD`
- n-dimensional product contexts:
  - `NS_PRODUCT_CONTEXT`
- name semantics:
  - `NS_BIND`, `NS_ALIAS`
- category flavor:
  - `NS_MORPHISM`
- Petri-style dynamics:
  - `NS_PN_NET`, `NS_RESOLVE_TRANSITION`, `NS_TOKEN`

## Derived semantics

Rules derive:

- transitive descendants and dimension successors,
- visible names from local, ancestor, alias, and product projection,
- morphism reachability closure,
- product dimension validity,
- transition enabling and resolution witnesses.

## Run

```bash
cd zil
./bin/zil preprocess examples/namespace-pn-nd-extension.zc /tmp/namespace_pn_nd.pre.zc libsets/namespace-pn-nd
./bin/zil /tmp/namespace_pn_nd.pre.zc
./tools/namespace_pn_nd_smoke.sh
```

Key queries:

- `namespace_hierarchy_edges`
- `namespace_descendants`
- `namespace_visible_names`
- `namespace_product_contexts`
- `namespace_morphism_reachability`
- `namespace_transition_enabling`
- `namespace_resolution_witnesses`
- `namespace_binding_conflict_pairs`

## Note on conflict query

`namespace_binding_conflict_pairs` is a pair-level diagnostic query.
It intentionally returns candidate binding pairs for the same `(context, local_name)`.
Post-processing can ignore reflexive pairs (`b1 == b2`) if needed.
