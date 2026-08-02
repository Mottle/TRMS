# TRMS glossary

## Mold

A placeable `trms:mold` ceramic block and its corresponding item. A Mold keeps
its carved pattern when converted between the placed BlockEntity and the item.

## Mold shell

The permanently solid portion of a Mold: its entire lower layer and the outer
ring of its upper layer. It renders through the static client block model.

## Carvable cell

One of the 196 upper-layer cells whose local coordinates satisfy `x ∈ [1, 14]`
and `z ∈ [1, 14]`. A Carvable cell is one Minecraft pixel wide, long, and high:
`1/16 × 1/16 × 1/16` blocks.

## Cavity

The set of carved Carvable cells in a Mold. A Cavity uses eight-neighbor
connectivity: horizontal, vertical, and diagonal contact in the upper layer all
connect cells.

## MoldPattern

The immutable authoritative representation of a Cavity. It contains 196 bits
(25 bytes) and a protocol format revision. Bit `1` means the corresponding
Carvable cell has been removed from the upper layer.

## Single-layer casting

The casting mode of one Mold. It produces a one-pixel-thick, planar casting
profile, suitable for flat weapon or tool parts. It does not by itself create a
volumetric three-dimensional casting.

## Protocol revision

The TRMS client/server compatibility revision. A client whose revision differs
from the server's required revision is rejected before it can interpret TRMS
content or MoldPattern data.

## Casting and cooling

**Fill state**

The material state of a placed Mold: unfilled or filled with exactly one registered molten material. Filling locks carving while the material cools; breaking the block clears the fill while preserving the carved pattern in the dropped Mold item.

**Elapsed cooling ticks**

The persisted authoritative cold-time value of a filled Mold: `0..580` in increments of 20 loaded ticks. The thirtieth update completes casting at 600 ticks. It is not a seconds or visual-stage counter; the block state derives compact visual stages from it, retaining full brightness and effects through tick 280 and starting attenuation at tick 300.

**Fill material**

A namespaced, extensible material identity such as `trms:copper`, `trms:iron`, or `trms:gold`. A material provides its accepted input item, display name, tinted animated lava-like texture, and light emission behavior.

**Molten fill**

A visual-only occupation of every carved cell in the Mold's Cavity. It uses contiguous geometry with shared internal faces removed, does not create a Minecraft `FluidState`, and does not flow or alter collision.

**Weapon part**

One `trms:weapon_part` item emitted when a molten fill completes cooling. Its data component stores the source `MoldPattern` and `Fill material`; its client model is the same closed, one-pixel-thick player-authored silhouette. It is an assembly input, not yet a functional weapon.
