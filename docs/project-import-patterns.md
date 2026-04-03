# Project Import Patterns for Domain-Specific ZIL Implementations

This document describes how to bring external/domain repositories (like
`zil-mft-sync-migration-app`) into main `zil` while keeping core vs
problem-specific boundaries clear.

## Target structure

Use:

```text
projects/
  <project-name>/
    lib/
    models/
    plugins/
    docs/
```

This keeps domain implementations explicit and avoids mixing them into
top-level `lib/` (core language library).

## Import options

### Option A: Git submodule

Use when source repo has a stable remote and independent lifecycle.

Pros:

- clean source-of-truth boundary
- explicit pinned version
- easy upstream contribution path

Cons:

- requires submodule workflow discipline
- extra git friction for some contributors

### Option B: Git subtree

Use when you want one-repo UX and still preserve upstream history.

Pros:

- no submodule commands needed for consumers
- can pull/push subtree changes

Cons:

- subtree history operations can be heavy/confusing
- less obvious version pin than submodule SHA

### Option C: Snapshot import (implemented now)

Use when source may be local/private/early-stage or remote is not yet stable.

Pros:

- simple operations
- no git-submodule dependency
- works with local-only source repo

Cons:

- manual re-import needed to sync updates
- commit linkage must be documented

Implemented scripts:

- `tools/project_import_fts_sync.sh` : import/update snapshot
- `tools/project_stage_lib.sh` : compose layered macro libs
- `tools/project_run_model.sh` : preprocess + execute project model

## Current recommendation for FTS migration pack

1. Keep it under `projects/fts-sync-migration/`.
2. Use snapshot import while repo topology is still evolving.
3. Migrate to submodule when remote governance/versioning is stable.

## Typical workflow

```bash
cd zil
./tools/project_import_fts_sync.sh
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-generic.zc
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-tlm.zc
./tools/project_run_model.sh fts-sync-migration models/examples/seeburger-to-aws-mft-sync.zc
```
