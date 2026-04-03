# Main ZIL Repo Integration Notes

This project pack was imported from `zil-mft-sync-migration-app`.

The upstream document `layered-update-workflow.md` references scripts in that
standalone repo (`bin/build-layered-lib-dir.sh`, `bin/run-example.sh`).

Inside main `zil`, use these equivalents:

- import/update snapshot:
  - `./tools/project_import_fts_sync.sh`
- stage layered libs:
  - `./tools/project_stage_lib.sh fts-sync-migration`
- preprocess + execute model:
  - `./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-generic.zc`
  - `./tools/project_run_model.sh fts-sync-migration models/examples/system-sync-migration-tlm.zc`
  - `./tools/project_run_model.sh fts-sync-migration models/examples/seeburger-to-aws-mft-sync.zc`

Environment overrides:

- `ZIL_BIN` override runtime path
- `INCLUDE_ZIL_CORE_LIB=1` include top-level `lib/*.zc`
- `INCLUDE_ZIL_SHARED_TLM_LIB=0` disable shared `lib/tlm-macros.zc`
- `ZIL_SHARED_TLM_LIB=/custom/path/tlm-macros.zc` custom shared TLM file
