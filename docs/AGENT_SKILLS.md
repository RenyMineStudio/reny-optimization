# Agent skills profile

Status: **planned integration; no skill sync implemented in this repository**.

`RenyMineStudio/agent-skills` is the canonical source of reusable worker skills. A Reny Optimization worker should normally remain scoped to this repository.

## Intended groups

- `common` — execution/review discipline;
- `minecraft` — shared Forge/Minecraft workflows;
- `optimization` — profiling, benchmarks, frame-time/tail-latency and compatibility-oriented optimization practice.

MDT-specific skills may be selected for tasks that exercise `minecraft-dev`, but this does not make MDT the owner of organizational skills or a runtime dependency of Reny Optimization.

## Isolation

The future manifest resolver must not merge manifests from sibling repositories. Access to `../minecraft-dev-toolkit`, `../reny-shaders`, `../The-Reawakening` or another provider/consumer requires an explicitly cross-repo task.

Cross-repo skills may guide contract ordering and joint verification, but never grant filesystem or GitHub authority.

## Planned materialization

Selected skills are materialized flat under `.agents/skills/<skill-id>/SKILL.md`. On the normal sibling workspace, prefer relative per-skill symlinks to `../agent-skills`; the future lock records the exact catalog revision.

Canonical design: `RenyMineStudio/agent-skills/docs/superpowers/specs/2026-09-25-agent-skills-distribution-design.md`.
