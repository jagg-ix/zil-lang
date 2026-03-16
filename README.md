# Z-Core Language

Z-Core is a declarative tuple-and-rule language for general knowledge modeling, verification, and policy reasoning.

This repository starts with language design artifacts only (no runtime implementation yet).

## Repository Goals

1. Define a stable core language with formal semantics.
2. Separate core semantics from optional profiles (e.g., Zanzibar compatibility).
3. Keep implementation-language concerns out of language design.
4. Provide worked examples for IT infrastructure, dependencies, failover, and drift detection.

## Structure

- `spec/` normative language specs
- `profiles/` optional profile specs (e.g., zanzibar)
- `docs/` explanatory guides and rationale
- `examples/` illustrative models and queries
- `notes/` open issues and design decisions

## Current Status

- Core: Draft
- Time model: Draft (causal core + optional time profiles)
- Zanzibar compatibility profile: Draft

