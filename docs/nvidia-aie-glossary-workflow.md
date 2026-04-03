# NVIDIA AIE Glossary Workflow

Equivalent to the AWS glossary/model-input extraction flow, this workflow builds a structured glossary model from NVIDIA AI Enterprise docs.

## Source

- URL:
  `https://docs.nvidia.com/ai-enterprise/release-7/latest/troubleshooting/glossary.html`

## Script

- `tools/extract_nvidia_aie_glossary_inputs.py`

## Outputs

- `examples/generated/nvidia-aie-glossary-inputs.json`
- `examples/generated/nvidia-aie-glossary-inputs.zc`

## Run

```bash
cd zil
python3 tools/extract_nvidia_aie_glossary_inputs.py
./bin/zil examples/generated/nvidia-aie-glossary-inputs.zc
./tools/nvidia_aie_glossary_smoke.sh
```

## Offline/controlled-input mode

If you already captured the HTML page:

```bash
python3 tools/extract_nvidia_aie_glossary_inputs.py --html-file /path/to/glossary.html
```

## What gets extracted

- page metadata (`title`, `docbuild:last-update`, `docs_version`, etc.)
- glossary sections
- term/definition pairs with section attribution
- generated ZIL queries:
  - `nvidia_glossary_sections`
  - `nvidia_glossary_terms`
  - `nvidia_glossary_terms_by_section`

## Current extraction snapshot

From the current page version during generation:

- sections: `6`
- terms: `86`
- docs version: `7.4`
- last-update: `Mar 11, 2026`
