# Horizon Scripting

Horizon scripting embeds GraalJS for runtime server automation.

Scripts are not a replacement for extensions. Use scripts for lightweight automation,
event glue, diagnostics, and server-specific behavior. Use extensions for stable
content, registries, networking, services, and performance-sensitive systems.

## Configuration

Section: `[scripting]` in `config/horizon.toml`.

| Key | Default | Purpose |
| --- | --- | --- |
| `enabled` | `false` | Load `.js` files from the script directory after server start. |
| `script-directory` | `scripts` | Directory scanned for script files. |
| `allow-shell` | `true` | Allows the console-only script shell. |
| `max-memory` | `128M` | Maximum retained Graal guest heap per script context when supported by the runtime. |
| `max-region-contexts` | `256` | Maximum retained regional Graal contexts across all scripts, including contexts pending asynchronous close. |
| `max-region-contexts-per-script` | `64` | Maximum retained regional Graal contexts for one script. |
| `max-listener-failures` | `16` | Auto-close an event subscription after repeated callback failures; `0` disables automatic closure. |
| `max-subscriptions-per-script` | `256` | Maximum active event subscriptions for one script. |
| `slow-listener-warning-ms` | `10` | Warn when a script listener callback takes at least this long. |

Memory limiting depends on GraalVM sandbox support. If the runtime cannot enforce
`sandbox.MaxHeapMemory`, Horizon logs a warning. `max-memory` applies to each context
shard, not to the sum of all contexts for one script. Unsafe scripts can still allocate
memory through JVM objects outside guest-heap accounting.

## Safe and Unsafe Channels

File scripts are safe by default.

Safe mode:

- Can access only the exposed `horizon` API.
- Cannot use `Java.type(...)`.
- Cannot access raw JVM event objects.

Unsafe mode:

- Enabled for a file script only when the first line is exactly:

```javascript
#!UNSAFE
```

- Can access JVM classes through Graal host access.
- Can call unsafe-only event methods such as `event.raw()` and `event.rawLevel()`.
- Still does not enable native access.

The server shell is a separate channel and is unsafe by design. It is intended for
console-only administration, diagnostics, and emergency inspection. Shell expressions
cannot register event listeners, because a shell callback could otherwise escape its
single global context and be invoked concurrently by region threads.

## Basic Script

```javascript
horizon.log.info("script loaded");

horizon.events.on("player.join", event => {
  horizon.log.info("joined: " + event.playerName());
});
```

## Events

Scripts use `horizon.events`.

```javascript
const subscription = horizon.events.on("block.break", {
  priority: "normal",
  receiveCancelled: false
}, event => {
  if (event.cancelled()) {
    return;
  }

  horizon.log.info("block break at " + event.blockX() + ", " + event.blockY() + ", " + event.blockZ());
});
```

One-shot listeners:

```javascript
horizon.events.once("server.tick", event => {
  horizon.log.info("first tick observed");
});
```

List event names:

```javascript
horizon.events.names();
```

Console command:

```text
/horizon script event-types
```

Chat events are exposed as `player.chat` and `player.mention`.

```javascript
horizon.events.on("player.chat", event => {
  if (event.message().startsWith("!")) {
    event.cancel();
  }
});

horizon.events.on("player.mention", event => {
  horizon.log.info(event.player().name() + " mentioned " + event.targetName());
});
```

`player.chat` runs on the backing async chat pipeline when `event.asynchronous()` is
true. Keep callbacks short and avoid mutable world access from that callback.
`player.mention` is dispatched later on the sender player's region thread for
recognized `@name` tokens. Online player names are recognized automatically; Horizon
extensions can register additional names such as NPC ids.

## Subscription Lifetime

Every event subscription is tracked against the script scope that created it.

File scripts must register listeners during top-level initialization. A later call to
`horizon.events.on(...)` or `once(...)` from a callback or scheduled function is
rejected. Horizon replays the file's top-level source when a region first needs the
script and verifies that listener type, priority, cancellation option, and one-shot
mode exactly match the original registration order. A mismatch disables that script
for the affected region rather than invoking a callback from the wrong context.

When a script is unloaded or reloaded:

- all subscriptions created by that script are closed;
- closed subscriptions are removed from dispatch;
- later events do not call the old callback.

Useful commands:

```text
/horizon script status
/horizon script reload
/horizon script events
/horizon script events <scriptId>
/horizon script reload <scriptId>
/horizon script unload <scriptId>
/horizon script event-types
/horizon script shell
```

`status` reports whether scripting and memory enforcement are active, the resolved
script directory, loaded-script/subscription counts, shell state, routable and retained
region Context counts, admission failures, initialization timing, and close backlog. `reload` without
an id reloads the complete script directory. `shell` starts the console-only interactive
shell; it is always unsafe, can call `Java.type(...)`, and remains disabled when
`allow-shell=false`. Type `.exit` on the server console to leave it.

## Safe Event View

Script event callbacks receive a `ScriptEventView`. The view exposes common event data
without giving safe scripts direct access to server internals.

Common operations:

| Operation | Purpose |
| --- | --- |
| `event.type()` | Script event name. |
| `event.cancelled()` | Current cancellation state, when supported. |
| `event.cancel()` / `event.setCancelled(true)` | Cancel, when supported. |
| `event.player()` | Player view for player-related events. |
| `event.entity()` | Entity view when available. |
| `event.block()` | Block view when available. |
| `event.level()` | Level view when available. |
| `event.message()` | Chat or mention source message when available. |
| `event.asynchronous()` | Whether `player.chat` came from the async chat pipeline. |
| `event.targetName()` | Mention target name without the leading `@`. |
| `event.mentionStartIndex()` / `event.mentionEndIndex()` | Mention range in the source message, with the end index exclusive. |
| `event.registeredTarget()` | Whether the mention target was an online player or extension-registered target. |
| `event.targetPlayer()` | Online mentioned player, or `null` for extension-owned targets. |
| `event.raw()` | Unsafe only. Returns the backing extension event. |
| `event.rawLevel()` | Unsafe only. Returns the backing level object where available. |

The event view is intentionally smaller than the Java Extension API. If a script needs
uncovered behavior, either add a safe wrapper method or use an unsafe script with care.

## Stability Rules

Script callbacks run on the same thread as the event they observe. Many Horizon events
run on region owner threads, not a single global main thread.

Each Folia region that first handles a matching file-script event lazily receives its
own GraalJS context for that script.
Callbacks in different regions therefore do not share a JavaScript context or callback
lock. Events that do not run on a region tick runner use the script's global context.
Within one context, callbacks remain serialized so a Graal value is never executed by
a different context. `horizon.scheduler.server(...)` and its
`horizon.runOnServer(...)` alias are available only in the global
file context and shell. Region contexts reject it because a closure can capture a live
event or world object that cannot safely move to the global server executor. Direct
server API operations such as broadcast and command execution copy their string input
before enqueuing server work and remain available.

This changes JavaScript state lifetime in important ways:

- `globalThis` and module-level variables are independent in every region context.
- A region split, merge, or retirement discards that region's script state; replacement
  regions start with fresh state on first use.
- Top-level source is evaluated once globally and again for every region context on
  first use. Logging, commands, and other top-level side effects therefore repeat per
  region. Keep top-level code deterministic and limit it to initialization. Calling
  `horizon.scheduler.server(...)` or `horizon.runOnServer(...)` at top level makes
  regional replay fail because that operation is intentionally global-context-only.
- The first matching event in a region synchronously creates a Context and replays the
  full top-level source on that region thread. This one-time initialization can stall
  that region; Horizon records initialization count, total/max time, and emits the
  configured slow-listener warning when replay crosses the same threshold.
- `once(...)` remains logical-script-wide: the first region that actually enters the
  callback consumes the subscription for all regions.
- The configured `max-memory` guest-heap limit is per context shard. The total possible
  guest heap grows with active regions multiplied by loaded scripts. The regional
  context caps bound this growth. Retired contexts retain their admission slot until
  Graal cancellation/close actually completes, so a blocked close cannot oversubscribe
  the memory bound. A rejected region retries after a short backoff and never falls
  back to the global Context.

Keep callbacks short:

- Do not block on network, disk, database, or long CPU work.
- Do not store live world objects in global variables.
- Do not use unsafe JVM access unless the script is operationally trusted.
- Prefer unloading or reloading a script over editing live global state by hand.

Repeated callback failures close the offending subscription once the configured failure
limit is reached. A limit of `0` keeps failing subscriptions active. A failing listener
does not stop later listeners for the same event; full stack traces are rate-limited
while compact failure messages continue to report the count.
