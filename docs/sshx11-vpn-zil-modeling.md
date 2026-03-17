# Modeling SSHX11 + VPN in ZIL

## Goal

Use ZIL to describe your SSHX11/VPN system in three layers:

1. topology/assets (`HOST`, `SERVICE`, `DATASOURCE`, `METRIC`, `EVENT`)
2. operational behavior (`LTS_ATOM`)
3. formal policy gates (`POLICY`, solver-backed in `constraint` profile)

Reference architecture details in this repo:

- `docs/workstation/SSHX11_CONTROL_DATA_PLANE_STACK.md`
- `docs/workstation/SSHX11_REMOTE_SYSTEM_TEST.md`

## Ready Example

- `examples/sshx11-vpn-system.zc`

This example encodes:

- control/data plane and VPN components
- key local artifacts as file datasources (control/data events, plane state)
- session progression as a state machine (`LTS_ATOM`)
- relay gate and consistency constraints as SMT-checked policies

## How To Check It

Compile and run queries:

```bash
clojure -M -m zil.cli examples/sshx11-vpn-system.zc
```

Validate behavior model presence:

```bash
clojure -M -m zil.cli bundle-check examples/sshx11-vpn-system.zc lts
```

Validate constraints with SMT solver:

```bash
clojure -M -m zil.cli bundle-check examples/sshx11-vpn-system.zc constraint
```

## Recommended Repo Pattern

For collaboration with strict commit checks:

- put LTS units one-per-file for `commit-check ... lts`
- put policy checks one-per-file for `commit-check ... constraint`
- keep shared topology in separate files and gate those via `bundle-check`

This keeps model meaning explicit while preserving strict, reviewable units.
