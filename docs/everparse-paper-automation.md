# EverParse Paper Automation (arXiv:2505.17335v2)

This automation script converts the paper PDF into:

- structured JSON extraction inputs, and
- a runnable ZIL extension scaffold tied to the existing EverParse interop macro layer.

## Script

- `tools/generate_everparse_2505_17335_extension.py`

## Inputs

- `~/Downloads/2505.17335v2.pdf`

## Outputs

- `examples/generated/everparse-2505-17335-model-inputs.json`
- `examples/generated/everparse-2505-17335-extension.zc`

## Usage

```bash
cd zil
python3 tools/generate_everparse_2505_17335_extension.py --pdf ~/Downloads/2505.17335v2.pdf
./bin/zil preprocess examples/generated/everparse-2505-17335-extension.zc /tmp/everparse_2505_17335.pre.zc libsets/everparse-interop
./bin/zil /tmp/everparse_2505_17335.pre.zc
```

## What it extracts

- paper metadata (`pdfinfo` + source path)
- the four explicit contribution statements (`Our first/second/third/fourth contribution ...`)
- claim coverage checks for PulseParse, EverCBOR, EverCDDL, COSE, DPE, guarantees, extraction path, and performance mentions
- a generated ZIL query surface for:
  - claim status
  - claim gaps
  - full paper-coverage check
  - generic EverParse contract gaps

## Notes

- The generated model is conservative: if a claim is not explicitly detectable in text, it is marked as a gap.
- In current paper text, `double_fetch_freedom` is typically a gap in this mapping unless explicitly stated.
- Use `--strict` to fail generation if the script cannot recover at least four contribution statements.
