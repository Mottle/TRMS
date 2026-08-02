# TRMS NeoForge Client Mod

`mod` is the client-only NeoForge subproject of the TRMS ceramic-mold
prototype. It targets Minecraft `26.1.2`, NeoForge `26.1.2.76`, Java 25, and
ModDevGradle `2.0.142`. Install its built JAR on clients that connect to the
paired Horizon Extension in `../extension/`; never install it on the dedicated
Horizon server.

The side-neutral protocol and pattern implementation lives in `../common/` and
is embedded into this built Mod JAR. `mod` never imports the Extension module.

## Client responsibilities

The mod defines the exact client registry mirrors for the individually
registered `trms:mold`, `trms:mold_pattern`, `trms:weapon_part`, and mold block
entity. The
196-bit pattern is encoded as exactly 25 raw bytes on the wire, with the same
row-major `x/z = 1..14` mapping as the Extension. Carving uses normal pickaxes
from the `minecraft:pickaxes` item tag; the client mod does not register a
separate tool.

Placed molds render as a static terracotta bottom/rim plus a dynamic
block-entity renderer.  The renderer creates the solid upper cells and their
visible hole walls from the received immutable pattern; it caches by
pattern/revision rather than rebuilding every frame.  The physical collision
shape remains the fixed `16 x 16 x 2` thin slab.

When the server marks a non-empty mold filled, the renderer covers its carved
cells with a contiguous, visual-only molten surface. It sits slightly below the
ceramic rim, removes internal shared faces, uses project-owned greyscale copies
of the vanilla lava animations, and applies a stable copper, silver-iron, or
gold tint. The server synchronizes accumulated cooling ticks in 20-tick increments.
The first 300 ticks keep emitted light at 15 and retain full tint and effects;
from tick 300, derived visual stages lower emitted light to 1 and tint
brightness to 35%, while client-local lava sparks, smoke, pop sounds, and
ambient audio decline on the same curve.
This is not a `FluidState`: it cannot flow, heat, or change collision.
Unsupported future material IDs use a conspicuous fallback tint.

At the final 600-tick cooling update the server emits a `trms:weapon_part`
item. Its dynamically rendered model is the exact closed single-pixel-thick
`MoldPattern` silhouette without the ceramic shell. Copper and iron use the same
material colour as their molten source; gold uses a more restrained warm-gold tint
than the brighter molten fill. It is a future weapon
assembly input, with no independent use behavior in this iteration.

Each placed mold also has a horizontal `facing` state and faces the player who
placed it. The client rotates the static model, dynamic mesh, lighting, carving
guide, and hit-coordinate conversion together, while the stored pattern stays
in its stable block-local coordinate system.

The mirrored mold block applies the same placement rule locally as the
Extension: its support must be a dry, rigid full cube. This rejects leaves,
water or waterlogged blocks, slabs, and other partial or non-rigid supports
before the server has to correct a predicted placement.

When the player holds a pickaxe and aims at a mold that sits on a lodestone
and still has a legal next carving, the world renderer outlines every currently
legal next cell in a thick white grid. Before the first carve all 196 interior
cells are legal; later, only uncarved eight-neighbour cells of the existing
hole are outlined. During the same aimed carving state, each already-carved
cell receives a uniform semi-transparent white fill on its exposed lower
surface. If the crosshair hits one currently legal cell, that exact cell gains
a pulsing amber fill to preview the next carve. On a right click of that legal
cell, the client sends `trms:carve_mold` with the selected cell, actual hand,
and current block-entity revision, then suppresses the normal use-item packet.
The guide is client-only; the Extension remains authoritative for range,
permissions, mold state, connectivity, and durability.

Mold items use an explicit 26.1 item definition at
`assets/trms/items/mold.json` and the `trms:mold_special` item-model type.
The native `minecraft:display_context` selector uses a stable baked thumbnail
for GUI and hotbar slots; all non-GUI contexts use the dynamic mesh and retain
the item's received carving pattern.  All generated vertices provide UV0,
UV1/overlay, UV2/light, color, and normal attributes required by the 26.1
entity-solid vertex format.

At configuration time the mod receives `trms:protocol_challenge` and replies
through the configuration payload context, before a play-phase connection
exists. It advertises protocol version 1 and is rejected by the Extension if
that version differs.

## Build, test, and run

From the `TRMS/` root:

```bash
./gradlew :mod:test
./gradlew :mod:build
./gradlew :mod:runClient
```

`mod/build/libs/` contains the distributable client JAR. For the managed local
pair, start `:extension:runHorizonServer`, then launch the client with a
multiplayer quick-play target of `127.0.0.1:25565`.

### Windows quick-play helper

With the Extension server running in WSL, double-click
`scripts/start-windows-test-client.cmd` from Windows. It launches the NeoForge
client through Windows PowerShell and Gradle, resolves the locally published
Horizon artifacts from the WSL Maven repository, and quick-connects to
`127.0.0.1:25565`. The helper checks the server connection before launching.
