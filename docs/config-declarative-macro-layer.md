# Declarative Config Macro Layer

This macro layer lets you model configuration-file patterns directly, without
rewriting each config concept as low-level ZIL facts by hand.

## Files

- library module: `lib/config-declarative-macros.zc`
- runnable demo: `examples/config-declarative-macros.zc`

## What it covers

- config document and profile metadata
- blocks/sections
- key/value entries
- references to other entities (`CFG_REF`)
- list items with index
- env-var bindings with defaults
- secret provider + secret reference
- endpoint blocks (`scheme`, `host`, `port`, `path`)
- retry + timeout + feature flags + cron schedule
- include chains and profile overrides

## Minimal usage

```zc
USE CFG_DOC(runtime_prod, yaml, "configs/runtime-prod.yaml").
USE CFG_PROFILE(runtime_prod, prod).
USE CFG_BLOCK(runtime_prod, web, service).
USE CFG_SERVICE(runtime_prod, web, opencv_whisper_bridge, prod, high).
USE CFG_KV(runtime_prod, web, webrtc_enable, true).
USE CFG_ENDPOINT(runtime_prod, web, mcp_api, http, "127.0.0.1", 8788, "/mcp").
USE CFG_ENV_BIND(runtime_prod, web, model_path, WHISPER_MODEL_PATH, "/models/ggml-base.bin").
USE CFG_SECRET_REF(runtime_prod, web, openai_api_key, local_keychain, "secret/openai/api_key").
USE CFG_OVERRIDE(runtime_prod, prod, web, timeout_ms, 2000).
```

## Run

Use preprocess with the focused libset so only this macro package is loaded:

```bash
cd zil
./bin/zil preprocess examples/config-declarative-macros.zc /tmp/config.pre.zc libsets/config-declarative
./bin/zil /tmp/config.pre.zc
```

## Built-in query pack

- `cfg_documents`
- `cfg_blocks`
- `cfg_entries`
- `cfg_entry_refs`
- `cfg_env_bindings`
- `cfg_secret_refs`
- `cfg_overrides`
