# Horizon Snapshot and Restore API

Horizon provides `HorizonContent.api().snapshots()` for plugins that must persist
and restore real Horizon content without reaching into CraftBukkit, NMS, NBT, or an
extension's private implementation. It is intended for systems such as rollback,
world history, inventories, and durable records.

```java
import moe.liar.horizon.plugin.content.HorizonContent;
import moe.liar.horizon.plugin.content.snapshot.HorizonSnapshotApi;

HorizonSnapshotApi snapshots = HorizonContent.api().snapshots();
```

The API is available only when Horizon's server-side content provider is installed.
Plugins which also run on non-Horizon servers must keep references to Horizon classes
behind a conditionally loaded adapter; see [Horizon Plugin API](horizon-plugin-api.md)
for the compatibility pattern.

## Contract and persistence boundary

`ItemSnapshot`, `BlockSnapshot`, `BlockStateSnapshot`, `BlockEntitySnapshot`, and
`EntitySnapshot` are immutable transport values. Their `encoded()` values are opaque
Horizon data:

- Store the bytes unchanged, together with application-owned record metadata such as
  location and capture time.
- Reconstruct item and block values with `decodeItemSnapshot(bytes)` or
  `decodeBlockSnapshot(bytes)` before restoring them.
- Do not parse, edit, concatenate, or create the encoded bytes. They are neither an
  SNBT/NBT contract nor a Bukkit serialization format.
- Do not use the descriptive registry keys or format fields to manufacture a snapshot.
  Restoration validates the complete encoded envelope and returns a structured failure
  if its metadata and payload disagree.

The current Horizon snapshot format is versioned and records the source Minecraft data
version. Horizon upgrades known item, block-state, and block-entity payloads through
Mojang's DataFixer when restoring data from an older supported game data version.
Snapshots from a newer game data version, a newer Horizon format, malformed data, or
missing registry entries are rejected instead of being silently converted to fallback
`BARRIER` items or `UNKNOWN` entities.

An application should retain the original encoded bytes after a failed restore. It may
become restorable again after the relevant extension is installed or content is made
available. A successful `decode*` call can be stored in memory as its snapshot value,
but persistence must always use a fresh `encoded()` copy.

## Items

`captureItem(ItemStack)` captures the real registry key, amount, data components, and
Horizon extension data for a non-empty server item. It returns an `ItemSnapshot` on
success. `restoreItem(ItemSnapshot)` reconstructs an ordinary Bukkit `ItemStack` only
when the complete payload can be resolved on the current server.

```java
SnapshotResult<ItemSnapshot> captured = snapshots.captureItem(stack);
if (captured.failure().isPresent()) {
    logSnapshotFailure(captured.failure().orElseThrow());
    return;
}

byte[] persisted = captured.value().orElseThrow().encoded();
// Store a defensive copy in your database or record format.

SnapshotResult<ItemSnapshot> decoded = snapshots.decodeItemSnapshot(persisted);
if (decoded.failure().isPresent()) {
    logSnapshotFailure(decoded.failure().orElseThrow());
    return;
}
SnapshotResult<ItemStack> restored = snapshots.restoreItem(decoded.value().orElseThrow());
```

Never replace an item restore failure with `Material.BARRIER`. A barrier is a valid
vanilla item and is not a lossless representation of missing custom content. Preserve
the record and report the failure code to the operator instead.

Item capture is stack-local, so it does not require a world region thread. The item
stack still follows normal Bukkit ownership rules: do not concurrently mutate a stack
while it is being captured.

## Blocks and block entities

`captureBlock(Block)` captures the complete real block state and, where present, its
block entity data. This includes supported custom block and block-entity content as
well as ordinary containers, signs, spawners, and other block entities. It does not
load a chunk.

`restoreBlock(Block, BlockSnapshot)` validates and constructs the state and block entity
before changing the world, then applies both at the target. It does not create a
cross-region task, load a chunk, or provide an undo transaction. Both calls must run on
the block's owner region thread, and the chunk must already be loaded and ready.

When an event or other callback gives you the old/new `BlockState` rather than a live
`Block`, use `captureBlockState(BlockState)`. It reads the state object's retained NMS
handle and never consults the current block location, so it is appropriate after the
world position has changed or disappeared. It preserves Horizon custom-block identity
even where Bukkit reports the compatibility `BARRIER` material.

The input must be a server-created `CraftBlockState` (for example, a state provided by
a Bukkit/Paper event or `Block#getState()`), not a plugin-created implementation. Horizon
also needs self-contained block entity data. Horizon custom blocks and custom block
entities retain this data on their fallback state, including before/after states created
from internal `LevelAccessor` event paths. Changing the retained state's block type
invalidates that block-entity data; changing properties of the same block type keeps it.
For ordinary vanilla `TileState`s, Canvas must have tile-entity snapshot creation enabled
when obtaining the state; if it does not, `captureBlockState` returns `UNSUPPORTED`
rather than reading a possibly changed live block entity.

```java
BlockState beforeState = /* the state supplied by your event/callback */;
SnapshotResult<BlockSnapshot> before = snapshots.captureBlockState(beforeState);
before.value().ifPresent(snapshot -> saveBlockRecord(recordLocation, snapshot.encoded()));
```

```java
// Execute this body with the target block's region scheduler.
SnapshotResult<BlockSnapshot> captured = snapshots.captureBlock(block);
captured.value().ifPresent(snapshot -> saveBlockRecord(block.getLocation(), snapshot.encoded()));

BlockRestoreResult result = snapshots.restoreBlock(block, savedSnapshot);
if (!result.restored()) {
    logSnapshotFailure(result.failure().orElseThrow());
}
```

`BlockSnapshot` is intentionally a complete state snapshot, not just block-entity
data. Do not restore a captured chest, sign, or custom block entity onto an arbitrary
different block type. Horizon checks that the block entity matches the restored state
and returns `TYPE_MISMATCH` or `INVALID_SNAPSHOT` if it does not.

Item and block capture enforce the same encoded-size limits as decode and restore. An
oversized value is rejected during capture with `FORMAT_TOO_LARGE`, so a successfully
captured snapshot is not immediately unusable at the persistence boundary.

## Multi-location restores

Use `restoreBlocks(Plugin, Collection<BlockRestoreRequest>)` when a rollback spans
multiple locations. Horizon schedules each request on its target region and completes
the returned `CompletionStage` after every request has an outcome.

Requests in the same world chunk are coalesced into one region-owned segment. Large
segments are capped and continued on later ticks so a rollback cannot monopolize one
region tick merely because many recorded blocks share a chunk.

```java
List<BlockRestoreRequest> requests = records.stream()
    .map(record -> new BlockRestoreRequest(record.block(), record.snapshot()))
    .toList();

snapshots.restoreBlocks(plugin, requests).thenAccept(result -> {
    for (BlockRestoreOutcome outcome : result.outcomes()) {
        if (!outcome.result().restored()) {
            logSnapshotFailure(outcome.result().failure().orElseThrow());
        }
    }
});
```

The result preserves the supplied request order through `requestIndex`. Region segments
may execute concurrently and the operation is **not an atomic cross-region
transaction**. A caller that needs all-or-nothing business semantics must plan its own
compensation/rollback strategy. Do not access the affected live blocks from the completion
callback without scheduling back to their owner regions.

The `Plugin` argument owns the scheduled region tasks. Keep the plugin enabled until
the returned stage completes; do not use this API during plugin disable as a way to
outlive its scheduler ownership.

## Entity snapshots and Extension ownership

Entity snapshots deliberately do not expose generic entity NBT. `captureEntity(Entity)`
and `restoreEntity(Location, EntitySnapshot)` support state contributed by the Extension
that owns a custom entity. Both calls require the relevant entity/location owner thread.

An entity snapshot contains the real entity type key, contributor key, contributor format
version, and opaque contributor payload. Horizon checks that restoration returns a real
server entity of the recorded type. It does not know how to interpret an Extension's
private fields.

Extensions register a contributor during their mutable initialization phase:

```java
public final class ExampleExtension implements HorizonExtension {
    @Override
    public void onInitialize(ExtensionContext context) {
        context.snapshots().registerEntityContributor(new MachineGuardSnapshots());
    }
}
```

The contributor's `id()` must be a stable, globally unique `Identifier`; its
`formatVersion()` and byte payload are the Extension's persistence contract. A
contributor must return `SnapshotResult.failure(...)` rather than throwing for expected
decode or compatibility failures. It should only claim entities from `supports(Entity)`
that it can safely capture, and it must restore at the supplied location on the target
owner thread.

Contributors are automatically removed when their Extension unloads. Existing records
are retained by the calling plugin but restore with `UNSUPPORTED` until the owner
Extension and matching contributor are loaded again. This is intentional: a generic
plugin must never attempt to write an unloaded Extension's private entity data.

## Failures and retry guidance

Every capture, decode, and entity restore uses `SnapshotResult<T>`; block restore uses
`BlockRestoreResult`. Inspect `SnapshotFailure.code()` for program logic and reserve the
message for logs and diagnostics.

| Failure code | Typical handling |
| --- | --- |
| `WRONG_THREAD` | Reschedule onto the owner region/entity thread. |
| `CHUNK_NOT_READY` | Wait for or explicitly arrange the chunk to be loaded, then retry. |
| `REGISTRY_ENTRY_MISSING`, `UNSUPPORTED` | Keep the record; install/enable the needed content or Extension before retrying. |
| `FORMAT_TOO_NEW`, `FORMAT_UNSUPPORTED` | Preserve the record and upgrade the server or provide a compatible migration. |
| `INVALID_SNAPSHOT`, `FORMAT_TOO_LARGE` | Treat the stored input as corrupt or untrusted; do not retry unchanged data. |
| `TYPE_MISMATCH` | Correct the target/record mapping; do not coerce it to another type. |
| `FAILED` | Log the diagnostic and retry only if the operation is known to be transient. |

Snapshot payloads can be supplied by a database, file, or network peer. Decode them
before use and treat failure as a normal, contained result; never deserialize them with
plugin-owned NBT code or assume they are trusted merely because their Java type is a
snapshot.

## What this API does not provide

The snapshot API does not yet provide a block-mutation event stream or a globally atomic
rollback transaction. A history plugin is still responsible for choosing what to record,
grouping rollback requests, applying its own authorization rules, and deciding how to
resolve partial restore outcomes. The API focuses on lossless Horizon-owned
serialization, validation, game-data migration, and correct region-thread application.
