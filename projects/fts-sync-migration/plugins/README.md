# Plugin Layer

Put optional domain extensions in this directory as `*.zc`.

These files are loaded by `bin/build-layered-lib-dir.sh` between:

1. `ZIL_CORE_LIB_DIR` (upstream ZIL libs, optional)
2. `models/plugins` (this directory)
3. `models/lib` (app DSL)

This lets you evolve plugin behavior without modifying ZIL core or app DSL.

Prefer targeting generic `SYNC_*` concepts in plugins.
Use `MFT_*` compatibility only when adapting older case-specific models.
