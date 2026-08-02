# ADR-0002: visual molten mold fills

- Status: Superseded by ADR-0003
- Date: 2026-08-02
- Scope: TRMS Horizon Extension and TRMS NeoForge client Mod

## Context

TRMS initially needed an extensible, immediately testable filling stage before it had
recipes, temperature, fluid simulation, cooling, or casting outputs. The
existing protocol must remain v1 and must not gain a filling payload. A future
reader could otherwise reasonably mistake the rendered material for a Minecraft
fluid or expect the material to survive as item data.

## Decision

A placed Mold has a server-authoritative fill state: unfilled or exactly one
namespaced fill-material ID. The fill state belongs only to the placed BlockEntity;
it persists through world and client reloads, increments the normal mold revision
when changed, and is stripped by every conversion to a Mold item. A re-placed Mold
therefore keeps its carved pattern but starts unfilled at revision zero.

The `common` module owns stable material IDs. The Extension owns item-to-material
validation and persistence, while the Mod owns per-material names, colors, and
rendering. Copper, iron, and gold accept only their corresponding vanilla
ingots. A non-empty unfilled Mold accepts one use without a lodestone; survival
and adventure consume one ingot, while creative does not. Filled Molds reject
both refilling and further carving.

Filling uses ordinary server-authoritative block interaction and the existing
BlockEntity update path, so `TrmsProtocol.VERSION`, `trms-handshake-1`, and
`trms-carving-1` remain unchanged. The client renders every carved cell as a
continuous, visual-only fill with its surface slightly below the ceramic rim;
shared internal faces are removed. Project-owned greyscale animated texture
bases are derived from vanilla `lava_still` (top) and `lava_flow` (sides), so
each material's fixed tint can change the texture's hue rather than merely
darkening orange lava. Filled molds locally emit occasional lava-like sparks,
smoke, and ambience. Copper, iron, and gold emit level-15 light like lava, but create
no `FluidState`, flow, collision, heat, or casting result.

## Consequences

- This decision describes the original visual-only filling baseline. ADR-0003
  adds the current scheduled cooling and weapon-part output while retaining
  the no-`FluidState` boundary and v1 protocol.
- New materials require paired Extension and Mod descriptions under the same ID.
- The project is in active development, so the new placed-Mold data format does
  not need to read pre-fill world data.
