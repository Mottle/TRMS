# NeoForge to Horizon Extension Migration Notes

Horizon Extension API maps common server-side modding concepts onto a
region-threaded server. It is not a one-for-one clone of NeoForge, but the major
concepts have direct migration paths.

## Concept Map

| NeoForge concept | Horizon Extension API |
| --- | --- |
| Mod entrypoint | `HorizonExtension` |
| `mods.toml` | `extension.toml` |
| Deferred register | `ExtensionRegistrationSet` or `DeclarativeRegistrar` |
| Registry objects | `ExtensionRef<T>` and registrate entries |
| Event bus | `ExtensionEventContext` and event classes |
| Capability | `HorizonServices` and `ServiceKey<T>` |
| Attachment/data attachment | `AttachmentType` and `AttachmentAccess` |
| Data maps | `DataMapType`, datapack JSON, mergers/removers, holder lookup and `DataMapsUpdatedEvent` |
| Custom packets | `ExtensionNetworkContext`, `PayloadRegistrar`, `PayloadDistributor` |
| Config spec | `ExtensionConfigSpec` |
| Global loot modifier | `IGlobalLootModifier` and content type registration |
| Custom fluid helpers | `ExtensionFluidDefinition`, `ExtensionFluidSet`, `FluidType` |
| Spawn data | `ComplexSpawnEntity` |

## Entrypoint

NeoForge mods generally initialize through mod constructors and event buses. Horizon
extensions use an explicit lifecycle interface:

```java
public final class ExampleExtension implements HorizonExtension {
    @Override
    public void onInitialize(ExtensionContext context) {
        // Register content, services, events, networking, and config.
    }
}
```

See [Extension lifecycle](../horizon-extension/docs/lifecycle.md).

## Descriptor

Horizon uses `extension.toml` instead of `mods.toml`.

```toml
schemaVersion = 1
id = "example"
name = "Example Extension"
version = "1.0.0"
entrypoint = "com.example.horizon.ExampleExtension"
requiredClient = false
dependencies = []
contentNamespaces = []
```

See [Extension descriptor](../horizon-extension/docs/extension-descriptor.md).

Extension projects also need a Mojmap/NMS compile classpath because the public API
uses Minecraft server types. Apply `moe.liar.horizon.userdev` so paperweight consumes
the matching Horizon development bundle. Do not add `io.papermc:mache` directly;
Mache is a set of build inputs rather than a compiled Minecraft classpath.

## Registration

Small extensions can register directly from `ExtensionContext`.

Larger extensions should use `DeclarativeRegistrar`, which is closest to a compact
deferred-registration style:

```java
public final class ExampleExtension implements HorizonExtension {
    private final DeclarativeRegistrar registrar = DeclarativeRegistrar.create("example");
    private final ItemEntry<Item> item = registrar.item("example_item").register();

    @Override
    public void onInitialize(ExtensionContext context) {
        registrar.registerAll(context);
    }
}
```

See [Declarative registration](../horizon-extension/docs/declarative-registration.md).

## Services Instead of Capabilities

Horizon services are intentionally holder-safe for a region-threaded runtime.

Register a service provider on a registry subject:

```java
context.services().registerItem(
    HorizonServices.ITEM_STORAGE,
    MY_ITEM,
    ServiceProvider.pure(Void.class, MyStorage.INSTANCE)
);
```

Resolve services from the correct owner thread and use the provided self wrapper for
holder access.

See [Service API](../horizon-extension/docs/service-api.md).

## Threading Difference

This is the most important migration difference.

NeoForge server code often assumes a single main server thread for world state.
Horizon runs on a region-threaded model. Live world objects are owned by region
threads.

Migration rules:

- Do not access blocks, chunks, block entities, or entities from arbitrary threads.
- Do not schedule all world work to a global main thread.
- Use `ExtensionConcurrencyContext` ownership checks and region scheduling helpers.
- Treat event handlers as owner-thread callbacks for the affected object.

See [Threading rules](../horizon-extension/docs/threading.md).

## Networking

NeoForge channel setup maps to Horizon payload registration and configuration tasks.

Use required payloads only when the client must understand the extension protocol.
Use optional payloads for server-only extensions or graceful client degradation.

See [Networking](../horizon-extension/docs/networking.md).

## Data Attachments

NeoForge data attachments on entities, block entities, chunks, and levels map to Horizon
`AttachmentType` and `AttachmentAccess`. Horizon keeps the same separation between transient,
persistent, and synchronized state, while enforcing region ownership for mutation.

Use `AttachmentType.builder` for advanced migrations:

- a holder-aware NeoForge default factory maps to `AttachmentDefaultFactory` and `AttachmentOwner`;
- Codec persistence maps to `persistent(codec)`;
- custom ValueInput/ValueOutput serialization maps to `AttachmentPersistence`;
- sync predicates and initial/update context map to `AttachmentSyncHandler`;
- player-death and mob-conversion copying map to `AttachmentCopyHandler` and `AttachmentCopyReason`.

Local attachments do not require a network codec. Synchronized attachments do. Horizon performs
recipient filtering on the holder owner thread, so predicates must be side-effect-free and
non-blocking.

Copy handlers should derive values from the supplied attachment value. A death source owner may be
retired or reused by the time the target is restored and is not a safe route back into live world
state.

Horizon does not expose ProtoChunk attachments. Chunk attachment access starts at `LevelChunk`.
ItemStack attachments should migrate to data components, matching NeoForge's current guidance.

See [Attachments and data maps](../horizon-extension/docs/attachments-data.md).

## Resource Reload Listeners

NeoForge reload listeners map to Horizon `ExtensionReloadListener<T>`. Horizon invokes prepare on a
background preparation executor, waits for a complete barrier, then applies immutable snapshots in
listener dependency order after tags, reloadable registries, data components, and Data Maps are
ready.

Unlike a traditional single-main-thread server, Horizon's apply context does not own Folia regions.
Publish global immutable data during apply and submit any later entity/chunk mutations through the
Extension concurrency context. Initial loading has no live `MinecraftServer`; inspect
`ExtensionReloadApplyContext.initialLoad()` or its optional server value.

See [Extension resource reloads](../horizon-extension/docs/reload.md).

## Known Non-Goals

Horizon does not try to emulate every NeoForge API:

- There is no hidden main-thread fallback for region-owned work.
- Bukkit/Paper plugin APIs remain compatibility APIs, not custom content APIs.
- Some NeoForge client-side concepts only make sense when paired with a client mod.
- Unsafe direct NMS access is possible from Java but is not a stable extension contract.

When migrating, prefer Horizon's service, attachment, networking, and concurrency
contracts over direct translation of implementation details.
