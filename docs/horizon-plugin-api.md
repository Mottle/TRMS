# Horizon Plugin API

Horizon keeps Bukkit and Paper plugin compatibility, but it also exposes a small
plugin-facing API for server features that cannot be represented by the legacy Bukkit
API surface.

The current Horizon plugin API lives in `horizon-api` under:

```text
moe.liar.horizon.plugin
```

## Dependency

Use the normal Horizon API artifact when compiling plugins:

```kotlin
repositories {
    mavenLocal()
    maven("https://maven.canvasmc.io/releases")
    maven("https://maven.canvasmc.io/public")
}

dependencies {
    compileOnly("moe.liar.horizon:horizon-api:26.1.2-tiramisu.8-SNAPSHOT")
}
```

Use the published release version for released builds. Do not shade `horizon-api` into
plugins. The server provides the API at runtime.

For runtime API exchange between plugins and extensions, see the
[Horizon API Registry](api-registry.md). Its contracts are lifecycle-bound and
have stricter shared-classloader requirements than ordinary plugin calls.

Plugins that only target Horizon can reference these types directly. Plugins that also
support non-Horizon servers must isolate Horizon API references behind reflection or a
conditionally loaded adapter: a non-Horizon server does not provide the `horizon-api`
classes, so directly resolving `HorizonContent` or `HorizonAuth` can fail before
`isAvailable()` is called. Do not shade `horizon-api` merely to make the classes exist.

## Mixed Authentication API

Entry point:

```java
import moe.liar.horizon.plugin.auth.HorizonAuth;
import moe.liar.horizon.plugin.auth.HorizonAuthApi;
import moe.liar.horizon.plugin.auth.HorizonOfflineAccountIdentity;
import moe.liar.horizon.plugin.auth.HorizonPlayerIdentity;
import java.util.UUID;
```

Use this API when persisting player-owned data on servers that enable Horizon mixed
authentication. In mixed mode, normal server-accepted profiles keep the UUID
provided by the server or proxy login chain, while Horizon offline fallback
players use Horizon-managed identities such as `horizon_offline:liar`.

```java
HorizonAuthApi auth = HorizonAuth.api();
HorizonPlayerIdentity identity = auth.identity(player).orElseThrow();

String persistenceKey = identity.accountId(); // profile:<uuid> or horizon_offline:<name>
```

Prefer `HorizonPlayerIdentity.accountId()` over a bare `Player#getUniqueId()` for new
mixed-mode-aware data stores when you want a readable typed key. `SERVER_PROFILE`
means the server or proxy login chain accepted the GameProfile; it does not by
itself guarantee Mojang authentication.

For Horizon offline fallback accounts that are not currently online, resolve the
same account id and UUID through the offline account resolver instead of copying
Horizon's UUID generation rule:

```java
HorizonOfflineAccountIdentity offline = auth.offlineAccount("liar").orElseThrow();

UUID uuid = offline.uuid();
String accountId = offline.accountId(); // horizon_offline:liar
String profileName = offline.profileName(); // ofl_liar with the default prefix
```

`offlineAccount(String)` accepts the unprefixed requested name and follows the
same validation, normalization, and `auth.offline-profile-prefix` rules as mixed
authentication login, `/offlineauth register`, and `/offlineauth setpassword`.

`HorizonAuthApi` exposes:

| Method | Purpose |
| --- | --- |
| `mixedModeEnabled()` | True when Horizon mixed authentication is enabled. |
| `identity(Player)` | Typed player identity, including account id, UUID, profile name, requested name, account type, and lock state. |
| `offlineAccount(String)` | Resolves UUID, account id, requested name, and profile name for a Horizon offline account that may not be online. |
| `offlineUuid(String)` | Convenience accessor for the Horizon offline account UUID. |
| `offlineAccountId(String)` | Convenience accessor for the Horizon offline account id. |
| `accountId(Player)` | Convenience accessor for the stable typed persistence key. |
| `isOffline(Player)` | True for Horizon-managed offline fallback accounts. |
| `isLocked(Player)` | True while an offline fallback player has not authenticated with `/login`. |

## Custom Content Compatibility

Bukkit's `Material` and `EntityType` are enums. They cannot be extended at runtime, so
custom Horizon registry content cannot be represented precisely through legacy Bukkit
APIs.

Horizon therefore has two layers:

| Layer | Behavior |
| --- | --- |
| Legacy Bukkit/Paper APIs | Return stable fallback values for custom content. |
| Horizon Plugin API | Exposes the real registry key for custom content. |

Current fallback values are:

| Custom content | Legacy fallback |
| --- | --- |
| Custom block | `Material.BARRIER` |
| Custom item | `Material.BARRIER` |
| Custom entity | `EntityType.UNKNOWN` |

These fallback values are compatibility values only. They are not identity values.
For example, a `Material.BARRIER` item may be a real vanilla barrier or a fallback for a
custom item. Use the Horizon API to tell the difference.

## Content API

Entry point:

```java
import moe.liar.horizon.plugin.content.HorizonContent;
import moe.liar.horizon.plugin.content.HorizonContentApi;
```

Minimal use:

```java
HorizonContentApi content = HorizonContent.api();

if (content.isCustomItem(stack)) {
    NamespacedKey key = content.itemKey(stack).orElseThrow();
    // Use the real registry key, such as example:machine_core.
}
```

Availability check:

```java
if (!HorizonContent.isAvailable()) {
    // The Horizon API is present, but its server provider is not installed yet.
    return;
}
```

`HorizonContent.api()` is safe to call whenever the Horizon API classes are present.
Before Horizon installs its provider it returns an empty implementation:

- `itemKey`, `blockKey`, `blockDataKey`, and `entityTypeKey` return `Optional.empty()`.
- `spawnEntity(...)` returns `Optional.empty()`.
- `isCustomItem`, `isCustomBlock`, `isCustomBlockData`, and `isCustomEntity` return `false`.
- fallback material/type methods still return the documented fallback values.

## Ticking-Chunk Profiler API

`HorizonTickProfiler` starts bounded server-wide, world-wide, or single-chunk diagnostic windows
without exposing NMS or Folia region handles. Horizon performs the tick-path instrumentation and
publishes an immutable per-chunk report; the calling plugin owns commands, permissions, persistence,
and visualization. Use `EXACT` for short complete captures or `SAMPLED` for lower-overhead,
longer-running observation.

See [Ticking-chunk profiler API](tick-profiler-api.md) for supported categories, aggregation levels,
limits, lifecycle behavior, and Folia performance semantics.

## API Surface

`HorizonContentApi` exposes:

| Method | Purpose |
| --- | --- |
| `itemKey(ItemStack)` | Real item registry key for a stack. |
| `blockKey(Block)` | Real block registry key at a live block. |
| `blockDataKey(BlockData)` | Real block registry key for block data. |
| `entityTypeKey(Entity)` | Real entity type registry key. |
| `spawnEntity(Location, NamespacedKey)` | Creates a registered Horizon custom entity from its complete key. |
| `spawnEntity(Location, NamespacedKey, Consumer)` | Creates one and restores plugin-owned state before world insertion. |
| `isCustomItem(ItemStack)` | True when the item namespace is not `minecraft`. |
| `isCustomBlock(Block)` | True when the block namespace is not `minecraft`. |
| `isCustomBlockData(BlockData)` | True when the block data namespace is not `minecraft`. |
| `isCustomEntity(Entity)` | True when the entity type namespace is not `minecraft`. |
| `blockFallbackMaterial()` | Fallback used by legacy block APIs. |
| `itemFallbackMaterial()` | Fallback used by legacy item APIs. |
| `entityFallbackType()` | Fallback used by legacy entity APIs. |

## Player Permission Event

`moe.liar.horizon.plugin.event.player.PlayerPermissionEvent.Change` is a read-only Bukkit
event for a real online-player command-permission level transition caused by `OP` or `DEOP`.
It runs synchronously on the player's owner-region thread before Horizon sends the permission
packet, recalculates Bukkit permissions, and synchronizes the command tree.

Login/respawn refreshes and same-level updates are not reported. The event exposes `oldLevel()`,
`newLevel()`, and `cause()`; it is not cancellable. Listeners must not mutate region-owned player
state from another thread.

```java
@EventHandler
public void onPermissionChange(PlayerPermissionEvent.Change event) {
    plugin.getLogger().info(event.getPlayer().getName()
        + " permission level " + event.oldLevel() + " -> " + event.newLevel()
        + " (" + event.cause() + ")");
}
```

## Threading

The Horizon Plugin API does not bypass the region-threaded server model. Treat Bukkit
world objects the same way you would on Folia:

- Only read live blocks and entities from their owner thread.
- Avoid resolving live world state from arbitrary async callbacks.
- If your plugin supports Folia scheduling, use the same owner-region scheduling for
Horizon.

`itemKey(ItemStack)` is stack-local and does not inspect world state. `blockKey(Block)`
and `entityTypeKey(Entity)` inspect live world objects and must follow owner-thread
rules.

`spawnEntity(...)` is also owner-thread-only: schedule the operation on the region
that owns the destination location. It accepts a complete key such as
`example:machine_guard`; never reduce the key to its path or route it through
`EntityType.fromName(...)`. It returns empty when the custom type is not registered or
the spawn is cancelled. In particular, an empty result is not a successful restoration
and should not be marked as rolled back.

```java
NamespacedKey type = NamespacedKey.fromString("example:machine_guard");
HorizonContent.api().spawnEntity(location, type, entity -> {
    entity.getPersistentDataContainer().set(restoredKey, PersistentDataType.BYTE, (byte) 1);
}).ifPresentOrElse(
    entity -> restorePluginOwnedState(entity),
    () -> logRestoreFailure(type, location)
);
```

The callback can restore state that Bukkit exposes, including PDC data. Extension-owned
private entity state remains the extension's contract: an extension that needs it
restored must expose its own stable restoration service instead of asking a generic
plugin to write internal entity NBT.

## Snapshot and Restore API

`HorizonContent.api().snapshots()` captures and restores opaque, versioned item, block,
block-entity, and extension-owned custom-entity state without relying on plugin NMS
reflection. It reports structured failures and never substitutes legacy Bukkit fallback
values for custom content that cannot be restored.

Use `captureBlockState(BlockState)` for an event-provided before/after state that no
longer has a matching live world block; unlike `captureBlock(Block)`, it captures the
retained detached state instead of reading the current location.

See [Snapshot and restore API](snapshot-api.md) for persistence limits, retained custom
BlockEntity data, Folia/Horizon owner-thread requirements, batch semantics, failure
handling, and Extension entity contributors.

## Recommended Plugin Pattern

Use legacy Bukkit APIs for ordinary vanilla behavior, and only opt into Horizon checks
where custom content identity matters.

```java
public boolean isProtectedItem(ItemStack stack) {
    HorizonContentApi content = HorizonContent.api();
    Optional<NamespacedKey> key = content.itemKey(stack);

    if (key.isPresent()) {
        return key.get().equals(new NamespacedKey("example", "protected_core"));
    }

    return stack != null && stack.getType() == Material.NETHER_STAR;
}
```

Avoid this pattern:

```java
// Wrong: BARRIER may be a vanilla barrier or a custom item fallback.
if (stack.getType() == Material.BARRIER) {
    // ...
}
```

Prefer this:

```java
HorizonContentApi content = HorizonContent.api();
if (content.isCustomItem(stack)) {
    NamespacedKey key = content.itemKey(stack).orElseThrow();
    // Handle custom item key.
}
```

## Compatibility Promise

Horizon's goal for upstream Bukkit/Paper APIs is:

- Existing plugins should not crash simply because custom Horizon content exists.
- Legacy APIs may return fallback values for content they cannot represent.
- Plugins that need exact custom content identity should use the Horizon Plugin API.

The fallback layer is a compatibility layer, not a modding API. New custom blocks,
items, entities, fluids, menus, networking, services, and registries belong in the
Extension API.
