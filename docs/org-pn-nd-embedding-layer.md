# Organization + PetriNet + N-D Embedding Layer

This layer adds language-level constructs to model:

- organizations,
- which organizations use Petri nets,
- N-dimensional spaces and embeddings (`N-1 -> N`),
- software layers mapped to those spaces,
- connectivity between spaces and layers.

It also includes a lightweight LPPN-inspired marker that detects when a net
contains both reactive and declarative rule kinds.

Reference used for conceptual alignment:

- Giovanni Sileno, *Logic Programming Petri Nets* (`$HOME/Downloads/1701.07657v1.pdf`)

## Files

- `lib/org-pn-nd-embedding-macros.zc`
- `examples/org-pn-nd-embedding.zc`
- `libsets/org-pn-nd/org-pn-nd-embedding-macros.zc`

## Core macros

- `ORG(id, org_type)`
- `PN_NET(net, profile)`
- `ORG_USES_PETRINET(org, net)`
- `ND_DIMENSION(dim, rank)`
- `ND_DIM_SUCCESSOR(lower_dim, upper_dim)`
- `ND_SPACE(space, dim)`
- `PN_MODELS_SPACE(net, space)`
- `ND_EMBED_SPACE(lower_space, upper_space)`
- `ND_SPACE_LINK(link, from_space, to_space, link_kind)`
- `SW_LAYER(layer, space, layer_role)`
- `SW_EMBED_LAYER(lower_layer, upper_layer)`
- `SW_LAYER_LINK(link, from_layer, to_layer, protocol)`
- `LPPN_RULE(net, rule_id, rule_kind)`

## Derived semantics

- transitive dimension successor relation,
- transitive space embedding relation,
- valid one-step embedding witness from `dim(N-1) -> dim(N)`,
- valid software embedding witness from mapped space embeddings,
- organization-to-space usage through its Petri nets,
- layer-link alignment checks against embedding,
- LPPN hybrid marker (`reactive` + `declarative` in same net).

## Run

```bash
cd zil
./bin/zil preprocess examples/org-pn-nd-embedding.zc /tmp/org_pn_nd.pre.zc libsets/org-pn-nd
./bin/zil /tmp/org_pn_nd.pre.zc
```

Key queries:

- `orgs_using_petrinet_nd_spaces`
- `nd_space_embeddings`
- `nd_embedding_steps_valid`
- `sw_embedding_valid_pairs`
- `sw_links_alignment`
- `lppn_hybrid_nets`
