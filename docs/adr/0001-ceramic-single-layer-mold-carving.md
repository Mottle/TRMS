# ADR-0001: ceramic single-layer mold carving

- Status: Accepted
- Date: 2026-08-02
- Scope: TRMS Horizon Extension and TRMS NeoForge client Mod

## Context

TRMS needs a player-authored mold for the first casting gameplay loop. The mold
must be placeable, visibly reflect every carving, retain its pattern when
broken and re-placed, and remain safe to synchronize between a Horizon server
Extension and its required NeoForge client Mod.

The original template is a `16 × 16 × 2` voxel slab. Allowing all 512 cells to
be removed would permit invalid, empty molds and would force every placement to
handle an absent physical shell. Encoding every possible pattern as a static
resource model is also impossible.

## Decision

### Geometry

A Mold occupies one horizontal block position and is two Minecraft pixels high.
Its local coordinates are:

```text
x = 0..15
z = 0..15
y = 0..1
```

The full lower layer (`y = 0`) is permanently solid. On the upper layer
(`y = 1`), the outer ring is permanently solid; only coordinates where
`x ∈ [1, 14]` and `z ∈ [1, 14]` are carvable.

Consequently, a player may carve all 196 interior upper-layer cells, but can
never remove the ceramic bottom or rim. A fully carved interior remains a valid
ceramic tray.

### Pattern encoding

`MoldPattern` encodes only the mutable interior rather than storing redundant
bits for the fixed shell.

```text
carvableIndex = (z - 1) * 14 + (x - 1)
bit count      = 196
serialized size = 25 bytes
```

A set bit means the upper-layer cell is carved. The bit ordering, serialization
format, and future format revision are implemented by the side-neutral
`common` module and covered by shared test vectors. The Extension remains the
only endpoint that may authorize and persist a change.

Each placed Mold also has a horizontal block-state `facing`, selected so the
Mold faces its placer. `facing` rotates the presentation and block-use ray
mapping only; the stored `x/z` pattern coordinates and their 25-byte encoding
remain block-local. Dropping and re-placing a Mold therefore preserves its
pattern while choosing a new facing for the next placement.

### Carving interaction

Players place a Mold in the world and carve it in-place with any pickaxe in the
`minecraft:pickaxes` tag. The client maps the local block-use ray to one
visible cell and sends that coordinate only as a request. The server treats the
coordinate as untrusted input: it independently verifies the mold, range,
permissions, held tool, magnetic base, revision, and carving rule before it
applies the authoritative edit on the Mold's owner region.

The first carving may target any carvable cell. Every subsequent carving must
touch the existing Cavity by the current layer's eight-neighbor rule: a shared
side or a shared corner both connect cells. Existing carved cells cannot be
carved again.

Mold crafting recipes and recovery of removed ceramic are explicitly deferred;
initial test content may be obtained through normal development or creative
access. Each accepted non-creative carve consumes one pickaxe durability
through Minecraft's native durability path.

### Casting semantics

One Mold creates a one-pixel-thick planar casting profile. The initial intended
use is flat weapon/tool-like parts and silhouettes, not a complete volumetric
item. A Cavity is considered a single casting cavity under the same
eight-neighbor rule used for carving; casting fills the accepted component as a
whole instead of simulating fluid movement solely through face-adjacent cells.

Future volumetric casting, if required, will use a separate multi-part mold or
closed-mold design rather than weakening this Mold's immutable bottom and rim.

### Rendering

Only the client Mod renders models.

The ceramic bottom and rim are supplied by a normal static block model. The
upper interior is rendered by a client-only BlockEntityRenderer that builds
mesh faces for uncarved cells and exposes side faces adjacent to carved cells.
The mesh is cached by immutable MoldPattern and revision; it is rebuilt only
when the synchronized pattern changes. The matching item renderer uses the
same geometry builder so an itemized Mold displays its own pattern.

The server never creates model JSON, mesh data, or references client rendering
classes.

### Collision

The collision and outline shape are always the uncarved `16 × 16 × 2` slab.
Carved pixels affect visible geometry and casting data but do not create dynamic
collision holes.

### Persistence and compatibility

When a Mold is broken, its MoldPattern is written to a dedicated immutable item
data component. Placing that item restores the exact pattern in the new
BlockEntity. BlockEntity updates synchronize the pattern and a monotonic visual
revision to tracking clients.

TRMS uses a required configuration-phase protocol handshake. The client and
server must advertise the same protocol revision before TRMS content is used.
A mismatch rejects the connection with a version-specific explanation.

The initial material is ceramic; its texture, sounds, and later heat behavior
must all represent ceramic rather than a generic placeholder material.

## Consequences

- Every valid Mold remains physically present after carving because its shell is
  immutable.
- The mutable payload is compact: 25 bytes rather than a 512-cell coordinate
  list or a static model family.
- Diagonal cavities are intentionally gameplay-connected even though a
  real-world fluid would not travel through a point contact.
- The first casting outputs are thin planar parts. Three-dimensional casting is
  a future feature with a distinct closed-mold design.
- The dedicated client Mod is mandatory, because it supplies both the dynamic
  renderers and the protocol compatibility implementation.

## Deferred decisions

- Ceramic mold recipes.
- Removal costs and ceramic scrap recovery.
- Molten material types, heating, pouring, cooling, and result-item rules.
- Multi-part molds and three-dimensional casting.
- Player ownership beyond ordinary build/claim permissions.
