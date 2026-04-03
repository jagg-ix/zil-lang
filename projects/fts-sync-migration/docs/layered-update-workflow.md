# Layered Update Workflow

This app supports three independent change layers:

## 1) App layer (`models/lib`, `models/examples`)

Use for:

- migration-domain semantics specific to this repository
- corporation-specific conditions, signals, and sync policies

Impact:

- local to this app repo
- lowest coordination overhead

## 2) Plugin layer (`models/plugins`)

Use for:

- optional variants by program, region, or business unit
- experimental policy extensions
- togglable domain packs without changing app core

Impact:

- local to this app, but more modular than direct app edits
- can be versioned independently by plugin file

## 3) ZIL core/lib layer (`ZIL_CORE_LIB_DIR`, usually `../zil/lib`)

Use for:

- generic language/library behavior useful across multiple repos
- reusable macros/rules not tied to a single migration case

Impact:

- higher blast radius; coordinate and test carefully
- can benefit many ZIL apps

## Shared TLM layer (`ZIL_SHARED_TLM_LIB`, default `../zil/lib/tlm-macros.zc`)

Use for:

- canonical TLM macro definitions shared across app repos
- avoiding copy/paste drift of `TLM_*` macro semantics
- keeping app-level `SYNC_TLM_*` wrappers thin and stable

Impact:

- medium blast radius (shared by TLM-enabled apps)
- should be versioned and reviewed as a reusable contract

## How scripts compose layers

`bin/build-layered-lib-dir.sh` builds a temporary lib directory in this order:

1. core (`ZIL_CORE_LIB_DIR`)
2. shared TLM (`ZIL_SHARED_TLM_LIB`, enabled by default)
3. plugins (`PLUGIN_LIB_DIR`)
4. app (`APP_LIB_DIR`)

Notes:

- core is opt-in (`INCLUDE_ZIL_CORE_LIB=1`) so day-to-day app output remains
  focused on this migration domain.
- shared TLM is on by default (`INCLUDE_ZIL_SHARED_TLM_LIB=1`) so TLM wrappers
  work consistently across repos.

Then `run-example.sh` and `sync-summary.sh` preprocess against that staged lib.
