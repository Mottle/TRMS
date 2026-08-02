# ADR-0003: mold cooling and weapon parts

- Status: Accepted
- Date: 2026-08-02
- Scope: TRMS Horizon Extension and TRMS NeoForge client Mod

## Context

The established molten fill has a player-authored single-layer silhouette but
previously never became gameplay output. TRMS needs a small, deterministic
casting loop without turning the visual material into a Minecraft fluid or
changing the existing v1 configuration and carving transports.

## Decision

A successful copper- or iron-ingot fill starts at cooling stage `0`. The
Extension schedules one authoritative block tick every 20 loaded server ticks.
Stages `0..9` are persisted on the placed Mold BlockEntity; the tenth scheduled
update, 200 ticks after pouring, creates exactly one `trms:weapon_part` item
above the mold. Its `trms:weapon_part` data component contains the non-empty
`MoldPattern` and namespaced `MoldFillMaterial`. The Mold then clears fill and
cooling state but retains its pattern for reuse. Breaking a still-cooling mold
produces no part and continues to drop only the reusable patterned Mold.

Cooling progress pauses with an unloaded chunk and resumes from the saved
stage. The derived `cooling_stage` block state reduces emitted light from 15
to 1 across the ten visible stages. The client receives ordinary block-state
and block-entity updates, darkens the existing material tint to 35%, and
reduces local sparks, smoke, and lava ambience with the same curve. No new
payload is registered: `TrmsProtocol.VERSION`, `trms-handshake-1`, and
`trms-carving-1` remain v1.

`trms:weapon_part` uses a dynamic client item model that renders the exact,
closed one-pixel-thick cavity silhouette. Copper and iron parts use the same
predefined copper and silver colors as their molten material, over a neutral
solid-metal texture; they do not use the animated lava texture.

## Consequences

- The server alone advances cooling and creates the output item.
- A fixed 200-tick loaded-time cost applies to every non-empty outline.
- The result is a data-carrying intermediate item for later weapon assembly,
  not a usable weapon yet.
- Molten fill remains visual-only: it has no `FluidState`, flow, heat, or
  collision behavior.
