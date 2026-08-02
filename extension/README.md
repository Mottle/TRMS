# TRMS Horizon Extension

`extension` is the authoritative Horizon Extension subproject of the TRMS
ceramic-mold prototype. It targets Minecraft `26.1.2`, Java 25, and the locally
published Horizon `26.1.2-tiramisu.7-SNAPSHOT` development bundle. It is an
Extension, not a Bukkit/Paper plugin, and builds only against artifacts in
`mavenLocal()`.

The matching NeoForge **client-only** Mod is in `../mod/`. Both runtime
artifacts embed `../common/`, which owns only pure protocol and mold-pattern
semantics. Neither endpoint compiles against the other's implementation, and
the Extension never depends on a Horizon source checkout.

## First playable loop

The Extension explicitly registers these individual entries:

- `trms:mold` block and block item;
- `trms:mold_pattern` immutable data component;
- `trms:mold` block entity.

A placed mold has a fixed `16 x 16 x 2` physical shell.  Its lower layer and
its one-pixel upper rim are permanent.  Only the upper interior (`x/z = 1..14`)
is mutable, as a fixed 196-bit (25-byte) pattern.  The collision and selection
shape intentionally remain the full thin slab even after carving.

Molds have a horizontal `facing` state. A new mold faces its placer, and the
same stored pattern is rotated with the block so its visible carving direction,
guide, and click coordinates remain aligned. The pattern's 25-byte wire and
item format stays block-local and unchanged; re-placing an item chooses a new
facing from the new placer.

A mold may only be placed on a dry, rigid full-cube support block. Its support
must have a complete collision cube and a top face that can rigidly support a
block. Leaves, water or waterlogged supports, slabs, and other partial or
non-rigid blocks are rejected; the mold also breaks if its valid support is
removed later.

With any pickaxe in the `minecraft:pickaxes` item tag, the client first picks
one visible upper cell and sends its block position, cell coordinates, hand,
and rendered revision in `trms:carve_mold`. The server independently validates
the normal block-interaction range, permissions, mold identity, lodestone base,
held pickaxe, revision, and requested cell:

- the first interior cell may be carved anywhere;
- each later cell must be 8-neighbour adjacent to an existing hole, including
  diagonals;
- repeats, boundary cells, lower-layer clicks, disconnected cells, and
  non-pickaxe interactions fail.

Every accepted carving consumes one point of the held pickaxe's durability
outside creative mode. The Extension uses the native item durability path, so
Unbreaking and unbreakable-item behavior apply automatically. Rejected
interactions never consume durability.

Carving is enabled only while the mold's direct support block is
`minecraft:lodestone`. A mold may still be placed on any valid rigid full-cube
support, but a pickaxe interaction on every other base is rejected before a
cell is changed or durability is consumed.

The block entity persists `MoldFormat`, `Pattern`, `Revision`, and its optional
placed-only `FillMaterial` ID; it emits a standard block-entity update packet
after every accepted carving or filling. A non-empty, unfilled mold accepts one
right-click with a copper or iron ingot without requiring a lodestone. Survival
and adventure consume one ingot, while creative does not. Filling is one-way:
it blocks further carving and refilling, emits light level 15, and never creates
a real fluid. Breaking a mold writes only its pattern to the dropped item, so
re-placement always clears the fill. Invalid persisted data is rejected
explicitly rather than silently resetting a player's mold.

During the configuration phase the Extension requires the paired client mod.
It sends a nonce-bound `trms:protocol_challenge`, requires
`trms:protocol_response`, and rejects a mismatched protocol revision with a
clear disconnect message.

## Build, test, and local server

From the `TRMS/` root:

```bash
./gradlew :extension:test
./gradlew :extension:build
./gradlew :extension:runHorizonServer
```

`runHorizonServer` resolves the Horizon paperclip and installs this project's
current Extension JAR under the Horizon-managed
`extension/run/server/extensions/` directory. Runtime worlds, logs, libraries,
and server configuration live under `extension/run/` and are not source or
build inputs. Do not manually copy an Extension JAR into that directory.

The managed local server is configured for the TRMS test port `25565`.  It is
only a development target; do not use it as a production server configuration.
