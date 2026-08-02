# Horizon API Registry

`HorizonApiRegistry` lets Bukkit plugins and Horizon extensions publish and
discover runtime API implementations without depending on each other's main
implementation classes. Registrations are tied to their owner's lifecycle:

- plugin registrations are withdrawn after the plugin's disable callback;
- extension registrations are withdrawn when its `ExtensionContext` closes;
- closing an `ApiRegistration` withdraws that provider early;
- an `ApiReference` resolves the current provider on every access and therefore
  follows provider replacement and unloads.

This registry is for component interoperability. It is separate from Bukkit's
`ServicesManager` and from the extension content-service system exposed through
`ExtensionContext.services()`.

## Shared Contract Requirement

The registry uses the exact Java `Class` object as its key. The API interface
must therefore come from a class loader shared by both producer and consumer.
Putting separate copies of the same interface in a plugin jar and an extension
jar does not work: those copies are different Java types even if their bytecode
and names match.

Use an interface provided by `horizon-api` or `horizon-extension-api`. Horizon
does not currently install or manage third-party shared contract artifacts.
Advanced deployments may arrange another common parent class path themselves,
but putting the same contract jar separately in a plugin and an extension does
not create a shared type. Do not put Bukkit plugin implementation classes,
extension implementation classes, NMS types, or mutable world objects in the
shared contract.

## Plugin Publication

Publish from `onEnable`, when Bukkit considers the plugin enabled:

```java
ApiRegistration<WorldRecordApi> registration = HorizonApiRegistry.api().register(
    this,
    WorldRecordApi.class,
    new WorldRecordApiImpl()
);
```

Horizon automatically closes this registration after the plugin's normal
disable callback. Explicitly closing it is useful when a feature is turned off
while the plugin remains enabled.

## Extension Publication

Extensions use their owner-bound context, so they cannot accidentally register
an API under another extension's identity:

```java
context.apis().register(WorldProtectionApi.class, protectionApi);
```

The context accepts runtime registrations during extension startup and after
freeze, but rejects new registrations after it has closed.

## Discovery and Provider Selection

For one-time, short-lived use:

```java
HorizonApiRegistry.api().find(WorldRecordApi.class).ifPresent(api -> api.record(change));
```

For a dependency retained by a long-lived component, retain an `ApiReference`
instead of retaining the implementation:

```java
ApiReference<WorldRecordApi> records =
    HorizonApiRegistry.api().reference(WorldRecordApi.class);

records.get().ifPresent(api -> api.record(change));
```

Multiple owners may publish the same API type. Higher numeric priority wins;
equal-priority providers retain registration order. `providers(Class)` exposes
the complete ordered list for consumers that intentionally aggregate providers.
One owner may publish only one implementation for each exact API type. This is
an intentional, cooperative override mechanism similar to Bukkit's services
registry: any component able to publish the shared contract may become its
selected provider. It is not a security boundary. Contracts that require one
specific owner should inspect `providers(Class)` and select by `ownerId()` and
`ownerKind()` instead of using `find(Class)`.

## Threading and Lifetime

Registry operations are thread-safe. The registry does not move API calls onto
another scheduler and does not make Bukkit or NMS objects safe to access across
regions. Every shared contract must document its own threading requirements.

An implementation obtained from `find` is only a momentary lookup result. The
owner may unload immediately afterward, so callers must not cache it. A live
`ApiReference` avoids stale selection, but the registry provides discovery, not
an invocation lease: an owner may still unload between `get()` and the ensuing
method call. Each call must follow the provider contract and owner lifecycle
rules.
