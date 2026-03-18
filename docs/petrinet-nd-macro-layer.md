# Petri Net + N-D Abstraction Macro Layer

This document shows how to leverage Petri-net modeling inside ZIL using a
macro library, while keeping the language core unchanged.

It is informed by:

- Michael Weber, Ekkart Kindler, *The Petri Net Markup Language (PNML)*
  (`$HOME/Downloads/PNML_LNCS.pdf`)

## Why this fits ZIL

From the PNML paper, the key ideas are:

1. a generic labeled graph core (`place`, `transition`, `arc`),
2. type/profile separation (PNTD and conventions),
3. structure mechanisms (pages + references),
4. semantics by flattening/inlining (resolve references/modules to a simpler net).

ZIL macros are a direct fit for this layering:

- core ZIL remains tuple/rule semantics,
- Petri concepts are expressed as domain macros,
- PNML compatibility is a profile, not a hard-coded parser feature.

## Implemented macro layer

Runnable file:

- `examples/petrinet-nd-macro-layer.zc`

It contains:

1. Petri core macros:
   - `PN_NET`, `PN_PAGE`, `PN_PLACE`, `PN_TRANSITION`, `PN_ARC_PT`, `PN_ARC_TP`
2. PNML-style reference support:
   - `PN_REF_PLACE`, `PN_ARC_PT_REF`
   - flattening rules that resolve ref-place sources into canonical places
3. N-dimensional abstraction support:
   - `PN_DIMENSION`, `PN_LAYER_ROOT`, `PN_LAYER_CHILD`
   - `PN_COORD(place, dim, value)` for arbitrary-dimensional coordinates
   - `PN_ABSTRACTS(concrete, abstract)` + consistency/lifting rules
4. Analysis bridges:
   - TM bridge: `PN_TM_MAP(...)` validated against real `TM_ATOM` transition facts
   - pi bridge: `PN_PI_MAP(...)` validated by pi synchronization evidence (`can_tau`)
   - lambda-style bridge: `PN_LAMBDA_MAP(...)` validated by rewrite witness facts

## What “n-dimensional place abstraction” means here

Places are not only graph nodes; they can carry coordinates across multiple
independent dimensions (for example `stack`, `domain`, `trust`) and belong to
an abstraction layer hierarchy.

So a place can represent a concrete state at one layer and map upward to a
more abstract place at a higher layer:

- concrete place `abstracts_to` abstract place,
- layer ancestry is checked (`child -> parent -> ...`),
- token lifting provides summarized evidence on abstract places.

This gives you a way to encode and query abstraction layers without changing
Petri fundamentals.

## Semantics scope

The example intentionally provides:

- structural semantics (graph + references + layer metadata),
- analysis witnesses (enabled-from-token witnesses, bridge-valid predicates),
- compatibility hooks.

It does **not** claim full universal Petri firing semantics (for example,
weighted multiset arithmetic and inhibitor semantics) in this first layer.
Those can be added as stricter profiles later.

## Run it

```bash
cd zil
./bin/zil examples/petrinet-nd-macro-layer.zc
```

Key queries:

- `flatten_ref_targets`
- `transition_enabling_witnesses`
- `abstraction_consistency`
- `lifted_tokens_on_abstract_places`
- `tm_bridge_valid`
- `pi_bridge_valid`
- `lambda_bridge_valid`

## Recommended evolution path

1. Add PNML import/export adapter layer (XML <-> ZIL facts/macros).
2. Add a stricter Petri execution profile (weighted/inhibitor/reset semantics).
3. Add projection/export rules:
   - Petri-to-TM (state abstraction)
   - Petri-to-pi (communication abstraction)
   - Petri-to-LTS for TLA+/Lean4 bridge reuse.
