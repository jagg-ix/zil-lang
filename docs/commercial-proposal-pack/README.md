# Commercial Proposal Pack (Fillable PDF + Markdown)

This pack provides one-page proposal templates for the four target industries:

- Finance and regulated IT
- Chemical and pharma process operations
- Aeronautical and avionics
- Software platform and SRE/managed services

It also includes a requirement baseline and Cucumber acceptance scenarios for
commercialization governance.

## Files

Markdown templates:

- `finance-regulated-it.md`
- `chemical-pharma-process.md`
- `aeronautical-avionics.md`
- `software-platform-sre.md`

ROI calculator sheet:

- `../../examples/data/commercial-proposal-roi-calculator.csv`

Fillable PDF sources:

- `fillable/proposal_form_common.tex`
- `fillable/finance-form.tex`
- `fillable/chemical-form.tex`
- `fillable/aeronautical-form.tex`
- `fillable/software-form.tex`

Requirements and Cucumber:

- `requirements-commercialization-v0.1.md`
- `cucumber/commercialization-governance.feature`
- `cucumber/steps/commercialization.steps.js`
- `cucumber/package.json`

Build script:

- `../../tools/build_proposal_pack_pdf.sh`

## Build Fillable PDFs

```bash
cd zil
./tools/build_proposal_pack_pdf.sh
```

Generated PDFs are written to:

- `docs/commercial-proposal-pack/fillable/out/`

## How to Use

1. Start with the industry Markdown one-pager and fill placeholders.
2. Fill ROI assumptions in `commercial-proposal-roi-calculator.csv`.
3. Copy the computed totals (Annual Benefit, Annual Cost, ROI, Payback Months)
   back into the one-pager.
4. Use the fillable PDF for sponsor review/sign-off.

## Run Cucumber Acceptance Checks

```bash
cd zil/docs/commercial-proposal-pack/cucumber
npm install
npm test
```
