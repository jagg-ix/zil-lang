# FTS Sync Migration Project Pack

This project pack imports and hosts the migration-sync domain model from
`zil-mft-sync-migration-app`.

## Why this is in `projects/`

- It is a specific problem-domain implementation (IT/FTS migration).
- It should not be confused with ZIL language core/runtime.
- It can evolve independently while still using shared core libs/macros.

## Layout

- `lib/mft-sync-macros.zc` : sync DSL + MFT compatibility wrappers
- `models/examples/*.zc` : generic + TLM + Seeburger/AWS cases
- `plugins/*.zc` : optional plugin layer
- `docs/layered-update-workflow.md` : layering guidance
- `docs/main-repo-integration.md` : command mapping for this main repo
- `docs/crux-integration.md` : Crux/XTDB leverage guidance
- `IMPORT-METADATA.md` : source snapshot reference

## Run examples from main ZIL repo

From `zil/`:

```bash
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-generic.zc
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-tlm.zc
./tools/project_run_model.sh fts-sync-migration models/examples/seeburger-to-aws-mft-sync.zc
./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-crux-interop.zc
```

## Re-import/sync from source repo

```bash
./tools/project_import_fts_sync.sh
```

Optional source path override:

```bash
./tools/project_import_fts_sync.sh /absolute/path/to/zil-mft-sync-migration-app
```

## Layering behavior

`tools/project_stage_lib.sh` composes libraries in this order:

1. core layer (`lib/`, opt-in with `INCLUDE_ZIL_CORE_LIB=1`)
2. shared TLM macro file (`lib/tlm-macros.zc`, on by default)
3. project plugins (`projects/fts-sync-migration/plugins`)
4. project app layer (`projects/fts-sync-migration/lib`)
