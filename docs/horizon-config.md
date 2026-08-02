# Horizon Configuration and Diagnostics

Horizon writes its server-specific configuration to `config/horizon.toml`.

This document explains the major Horizon-only sections. It is not a replacement for
the generated comments in the config file. Treat the generated config as the source of
the exact default values for a given build.

## Server Mode

Root key: `server_mode` (default: `"neoforge"`).

`"neoforge"` enables Horizon Extension client negotiation and custom registry content.
`"vanilla"` skips Horizon's NeoForge handshake and client payloads so unmodified vanilla
clients can connect. In vanilla mode, Extensions may still provide server-side logic, but
registration of client-visible items, blocks, block entities, entities, menus, particles,
fluids, custom registries, synchronized attachments/data maps, payloads, configuration tasks,
and client-synchronized configs is rejected during startup. The setting is startup-only and
requires a restart.

## Gameplay Defaults

Horizon creates new worlds with the `locator_bar` game rule disabled. This avoids the
continuous cross-region waypoint work used by the vanilla locator bar. Existing worlds
that already persist this game rule keep their saved value. Operators who want vanilla
locator-bar behavior can enable it per world with:

```text
/gamerule locator_bar true
```

## Performance Compatibility

Section: `[performance]`

| Key | Default | Purpose |
| --- | --- | --- |
| `faster-random-generator` | `true` | Uses Xoroshiro128++ for eligible non-shared Minecraft random sources; shared and thread-local sources retain concurrency-safe implementations. |
| `use-legacy-random-for-slime-chunks` | `false` | Keeps legacy slime-chunk placement even while the faster random source is enabled elsewhere. |

These settings are not output-only performance switches. Changing the random source
changes deterministic sequences used by world generation and, unless the compatibility
switch is enabled, slime-chunk placement. Existing terrain is not regenerated, so
changing the setting on an established world can make newly generated areas differ
from areas generated with the same seed under the old setting. Keep the values stable
when reproducibility, pregeneration, cross-server migration, or external seed tools
matter.

## Network

Section: `[network]`

| Key | Default | Purpose |
| --- | --- | --- |
| `optimize-non-flush-packet-sending` | `true` | Queues non-flush packet writes more efficiently to reduce Netty flush overhead. |
| `reduce-entity-move-packets` | `true` | Drops redundant entity move packets when the encoded movement would not change client state. |

These are low-risk network optimizations. Disable them first when debugging client
desyncs, entity interpolation issues, or packet-order-sensitive plugins.

### Chunk acknowledgement window

Section: `[network.chunk-ack-window]`

| Key | Default | Purpose |
| --- | --- | --- |
| `enabled` | `false` | Makes synchronous and asynchronous chunk publication obey the vanilla client's chunk-batch acknowledgements. |
| `max-unacknowledged-batches` | `10` | Maximum batches in flight after the first valid acknowledgement. The bootstrap limit is always one batch. |
| `max-outstanding-chunks` | `128` | Maximum sent but unacknowledged chunks for one connection. |
| `max-chunks-per-batch` | `64` | Maximum actual chunk packets enclosed by one batch. |

The client acknowledgement packet has no batch identifier, so Horizon records the
actual size of every sent batch and releases exactly one oldest batch per valid ACK.
Unsolicited or duplicate ACKs are ignored and cannot create credit. Start/finished
markers are emitted only when at least one chunk is really published, and an ACK-
blocked async packet keeps its watch generation and is woken by that connection's
next valid ACK. Admission occurs before watch callbacks and before the hierarchical
chunk byte budget, so a blocked connection neither changes client-watch state nor
consumes bandwidth credit. The ACK controller is per connection and never takes a
global monitor.

### Chunk bandwidth QoS

Section: `[network.chunk-bandwidth]`

These limits apply to synchronous and asynchronous chunk publication independently of
the `[async-chunk-sending].enabled` setting.

| Key | Default | Purpose |
| --- | --- | --- |
| `global-bytes-per-second` | `0` | Aggregate final wire-byte rate shared by all chunk sends. `0` disables this level. |
| `global-burst-bytes` | `16M` | Short-term global burst capacity. |
| `bind-address-bytes-per-second` | `0` | Per-bind-address final wire-byte rate. HAProxy destination is preferred over the channel local address. |
| `bind-address-burst-bytes` | `16M` | Short-term capacity for each active bind-address group. |
| `virtual-host-bytes-per-second` | `0` | Per-handshake-virtual-host final wire-byte rate. Host names are case-normalized and the port remains part of the key. |
| `virtual-host-burst-bytes` | `16M` | Short-term capacity for each active virtual-host group. |
| `upstream-bytes-per-second` | `0` | Per-TCP-upstream final wire-byte rate. Inet source ports are ignored, so connections through one proxy share a group. |
| `upstream-burst-bytes` | `16M` | Short-term capacity for each active upstream group. |

The four levels form one admission chain: global, bind address, virtual host, then
upstream. A send proceeds only when every enabled level has credit. A later denial
refunds every earlier reservation, so failed admissions do not leak global credit.
Each bucket is lock-free; normal region-thread admission never takes a global monitor
or waits. Per-route groups exist only while a connection or an in-flight charged send
references them, bounding virtual-host key churn.

Admission initially reserves a conservative packet estimate on the owner thread. The
Netty outbound path then reconciles it with the actual `ByteBuf` size after packet
encoding, compression, the VarInt frame prefix, and encryption. Root chunk packets
and recursively generated extra packets share one charge. Local in-memory connections,
which do not produce a socket `ByteBuf`, retain the estimate. Queue cancellation and
encoding failure refund bytes that never reached the final outbound path. A denied
packet remains queued until its eligibility tick, with stable zero-to-two-tick jitter
to spread simultaneous wakeups.

All values are bytes, not bits. Byte-size suffixes are binary: `16M` means
16 * 1024 * 1024 bytes, and a rate is bytes per second. When no proxy is used, the
upstream identity is the client's TCP address; behind a proxy it is the proxy node.
Only chunk payload traffic is controlled. Keepalive, interaction, entity, and other
latency-sensitive packets do not consume this budget.

## Mixed Authentication

Section: `[auth]`

Default: disabled.

Mixed authentication allows a failed online session to enter as a separate locked
Horizon offline identity. It changes the server trust model and must be deployed only
after online-mode or proxy forwarding is correctly secured.

Important controls:

| Key | Purpose |
| --- | --- |
| `mixed-mode-enabled` | Enables online-session-failure fallback. |
| `offline-profile-prefix` | Prefix added to fallback GameProfile names; prefix plus requested name must fit 16 characters. |
| `reserve-offline-profile-prefix` | Prevents raw logins from occupying generated fallback names. |
| `allow-self-registration` | Allows first-claim registration with `/register`; disabled by default. |
| `offline-allowlist-enabled` | Applies Horizon's separate fallback-account allowlist. |
| `offline-auth-timeout-seconds` | Locked-session timeout; `0` disables timeout kicks. |
| `offline-auth-max-password-attempts` | Invalid-login limit; `0` disables attempt-count kicks. |
| `offline-auth-min-password-length` | Minimum for new and reset passwords. |
| `offline-skin-*` | Optional third-party Yggdrasil texture lookup and timeout. |

See [Mixed authentication operations](mixed-authentication.md) before enabling the
feature. It documents the login pipeline, locked packet gate, commands, storage,
allowlist distinction, backup requirements, proxy implications, and skin-provider
privacy model.

## Diagnostics Settings

Section: `[diagnostics]`

| Key | Default | Purpose |
| --- | --- | --- |
| `event-thread-checks` | `true` | Warns when Horizon-owned extension event bridges dispatch block, chunk, entity, or selected level events from an unexpected thread. |

This check is diagnostic-only: it logs warnings and does not cancel, reschedule, or
block event dispatch. Warnings are rate-limited per event category. Disable it only
while investigating a known-safe noisy hook.

## Scripting

Section: `[scripting]`

Default: disabled.

Regional file-script callbacks use region-owned Graal Context shards. Bound their
retained memory with these controls:

| Key | Default | Purpose |
| --- | --- | --- |
| `max-region-contexts` | `256` | Global maximum number of retained regional Graal Context shards across all file scripts. |
| `max-region-contexts-per-script` | `64` | Maximum retained regional Graal Context shards owned by one file script. |

Both values have a minimum of `1`; `max-memory` applies separately to each Context, not
to the server-wide total. See [Horizon scripting](scripting.md) for the complete
configuration, Context lifecycle, memory model, and region-thread safety rules.

## Async Pathfinding

Section: `[async-pathfinding]`

Default: disabled.

Async pathfinding builds bounded world snapshots and computes eligible paths away from
the owning region thread. It is useful when pathfinding cost is visible in tick time,
especially with many mobs or expensive target selection.

Important controls:

| Key | Purpose |
| --- | --- |
| `enabled` | Master switch. |
| `threads` | Worker thread count. |
| `max-queued` | Submit queue limit. `0` uses the implementation default. |
| `snapshot-ttl-ticks` | Reuse window for nearby path snapshots. |
| `max-snapshot-sections-per-path` | Memory and blast-radius guard for one path. |
| `max-section-copies-per-tick` | Per-region-tick snapshot copying budget. |
| `max-region-chunk-radius` | Rejects snapshot requests whose region radius is too large. |
| `snapshot-reuse-block-size` | Spatial grouping used by nearby snapshot reuse; `0` disables grouping. |
| `target-set-cache-max-entries` | Per-region-tick bound for immutable target-set reuse; `0` disables that cache. |
| `max-cached-sections` | Global section-view cache bound; `0` disables retained section views. |
| `max-result-age-ticks` | Reject results that return too late. |
| `failure-threshold` / `circuit-breaker-delay-ticks` | Temporarily stop async admission after repeated worker failures. |
| `max-accepted-per-tick` | Per-region-tick async admission budget. |
| `max-queued-node-budget` | Global queued/running node-work estimate. |
| `max-targets-per-task` | Rejects oversized target sets before worker submission. |
| `fallback-max-accepted-per-tick` | Per-region-tick synchronous fallback limit for semantic misses. |
| `fallback-max-node-budget` / `fallback-max-average-mspt` | Per-region budget and MSPT guard preventing synchronous fallback from worsening an overloaded region. |
| `inline-max-*` | Per-region fast path for small, cheap path requests while MSPT is healthy. |
| `heuristic-*` | Weighted A* tuning, including stressed-mode behavior. |
| `pending-repath-*` / `min-repath-*` | Debounces repeated path requests while work is pending or a recent result is still useful. |
| `target-repath-distance-sqr` | Target movement required before replacing a recent path. |

Use `/horizon analysis pathfinding` to inspect runtime counters.

Async submission can be declined because the executor, per-region-tick admission budget,
queued node budget, target count, snapshot bounds, or circuit breaker rejects it.
Synchronous fallback is separately bounded by count, node budget, and average MSPT; it
is a correctness/availability escape hatch, not an unlimited guarantee. Inline and
fallback paths are suppressed under load so a saturated async optimization cannot turn
into an owner-region spike. Weighted heuristics trade path optimality for worker cost;
keep the default until workload-specific profiling justifies a change.

Requests that exceed the configured snapshot radius or section bound ask for synchronous
fallback immediately when the current region owns the complete search range. Queue
rejections, stale or expired results, and other async misses are counted independently
for each navigation; after three consecutive misses, its next request asks for the same
bounded fallback. A successful synchronous attempt or valid async completion clears the
counter. If the region is overloaded or the fallback budget is disabled or exhausted,
the mob continues normal retry/backoff behavior until a bounded fallback is available.

Each async result is also validated on its owner region before navigation installs it.
Any block or chunk mutation inside the captured search region invalidates both pending
work and completed-but-unapplied results. This avoids following a path through an
outdated snapshot, at the cost of additional retries around highly active doors,
redstone, or terrain edits.

Eligible vanilla walk, swim, deterministic fly, and amphibious evaluators run against
the frozen entity and region view. Evaluators with custom hooks, custom fluids, small
flyer start-node randomness, or other mutable behavior fall back to the bounded
synchronous path. The status command reports `snapshotCancelledDuringSearch` separately
from `staleSnapshotDiscards`, plus average and maximum worker queue wait, so tune queue
size and snapshot limits from observed pressure rather than moving rejected searches
onto the region thread.

## Async Chunk Sending

Section: `[async-chunk-sending]`

Default: disabled.

Async chunk sending prepares chunk packet data on worker threads and dispatches the
finished packet from the correct region owner. Temporary executor, byte-budget, and
per-player saturation keeps the chunk at the head of Moonrise's send queue for a
later tick; it does not fall back to synchronous serialization and spike the owner
region. A chunk whose own estimate exceeds the entire configured byte budget still
uses the synchronous path so it cannot be deferred forever.

Unready chunk packets are not inserted into the connection's general packet queue.
Unrelated traffic can continue while preparation runs. The owner-region commit first
revalidates the player, dimension, chunk watch generation, and loaded chunk, then
publishes the ready chunk packet before oversized block-entity packets, attachment
state, auxiliary light, and tracking effects. Leaving view cancels client-invisible
work without sending a redundant chunk-unload packet.

Captured chunk data carries a mutation version covering block/biome state,
heightmaps, block-entity client data, and published light changes. If that version
changes while a worker is preparing the packet, the owner-region commit rebuilds
from current state. A plugin watch callback that changes the chunk also forces the
synchronous current-state path before the packet is published.

Concurrent viewers of the same unchanged chunk share its in-flight capture and
worker preparation. The sharing key includes the loaded level instance, chunk
position, and mutation version, so a changed chunk or reloaded world cannot reuse
old data. Watch generations, cancellation, owner-region validation, packet wrappers,
and commits remain independent for every player.

Completed chunks may commit ahead of an earlier admission that is still being
prepared, avoiding per-player head-of-line blocking. Chunk packets have no protocol
dependency on admission order; each chunk's packet and dependent post-effects still
commit together in the required order on their owner region.

Important controls:

| Key | Purpose |
| --- | --- |
| `enabled` | Master switch. |
| `threads` | Worker thread count. |
| `max-queued` | Submit queue limit. `0` uses the implementation default. |
| `max-accepted-per-tick` | Per-region-tick submit budget. |
| `max-queued-bytes` | Memory estimate budget. Supports byte-size suffixes such as `128M`, `1G`, and `512K`. |
| `max-per-player-in-flight` | Per-player in-flight packet limit. |
| `max-result-age-ticks` | Age after which prepared snapshots are considered stale. |
| `max-commits-per-tick` | Maximum prepared chunk packets committed by one region per tick; excess ready packets wait for the next tick. |
| `max-commit-nanos-per-tick` | Per-region measured-time budget for packet publication, watch callbacks, and post-send effects. |
| `max-stale-rebuilds-per-tick` | Per-region budget for optionally rebuilding stale results on the owner thread. |
| `max-sync-fallbacks-per-tick` | Per-region count limit for synchronous recovery after worker failures or repeated version changes. Minimum `1`; synchronous recovery is the liveness guarantee after bounded async retries. |
| `max-sync-fallback-nanos-per-tick` | Per-region measured-time admission budget for synchronous recovery; minimum `1`, and an in-progress recovery is not interrupted. |
| `extra-bytes-per-chunk-estimate` | Conservative overhead added to each queued chunk estimate. |

Anti-xray is incompatible with async chunk sending and should remain disabled when
this optimization is enabled.

If a chunk changes before its prepared packet is committed, Horizon captures the new
version and resubmits it asynchronously. Worker failures and repeated version changes
receive at most two asynchronous recovery attempts before entering the per-region
synchronous count/time budget. Each region tick reserves one slot for this converging
recovery, so optional stale rebuilds cannot consume it; excess recovery is deferred to
a later region tick.
When initial admission is deferred by worker or byte-capacity backpressure, Moonrise
retains that player's send-queue head and stops their send loop for the tick. A rising
`deferredTasks` count together with one player's chunks pausing therefore indicates
backpressure saturation; inspect `threads`, `max-queued`, and `max-queued-bytes`.

Runtime status includes submitted unique preparations, coalesced viewer requests,
active shared preparations, deferred, cancelled, fallback, failed, and completed
counts; current reserved bytes and total prepared payload bytes; and cumulative
capture, worker-queue, preparation, drain-queue, and owner-commit time.

## Async Mob Spawn

Section: `[async-mob-spawn]`

Default: disabled.

Async mob spawning plans candidate spawn work away from the owner thread, then commits
bounded results back on the correct region thread.

Important controls:

| Key | Purpose |
| --- | --- |
| `enabled` | Master switch. |
| `threads` | Worker thread count. |
| `max-queued` | Number of planner tasks that may wait. |
| `planner-interval-ticks` | Planner cadence. |
| `max-plan-age-ticks` | Reject old plans. |
| `max-commits-per-tick` | Per-tick commit budget. |
| `max-candidates-per-player` | Candidate cap per nearby player. |
| `fallback-to-vanilla` | Use vanilla spawning when async planning cannot provide a plan. |

Use `/horizon analysis mob-spawn` to inspect runtime counters.

## Async Entity Tracking

Section: `[async-entity-tracking]`

Default: disabled.

Async entity tracking computes visibility changes away from the owner thread and
applies accepted add/remove operations under bounded per-tick budgets.

Important controls:

| Key | Purpose |
| --- | --- |
| `enabled` | Master switch. |
| `threads` | Worker thread count. |
| `interval-ticks` | How often visibility snapshots are submitted. |
| `max-result-age-ticks` | Reject stale visibility results. |
| `max-accepted-per-tick` | Submit budget. |
| `max-applies-per-tick` | Total apply budget. |
| `max-adds-per-tick` | Start-tracking budget. |
| `max-removes-per-tick` | Stop-tracking budget. |
| `validation-interval-ticks` | Cadence for running one bounded segment of full visibility validation; it is not a hard maximum validation delay. Cross-region ownership cleanup still runs every tick. |
| `max-pending-changes` | Bound on unapplied visibility changes retained for later owner ticks. |
| `max-submit-average-mspt` | Stops new async batches when region MSPT is too high; `0` disables this gate. |
| `max-sync-validations-per-tick` | Per-region, per-sweep-tick budget for synchronous correctness validation. |
| `stressed-average-mspt` | Threshold selecting the stressed apply/validation budgets. |
| `stressed-max-applies-per-tick` | Total apply budget while stressed. |
| `stressed-max-adds-per-tick` | Start-tracking budget while stressed. |
| `stressed-max-removes-per-tick` | Stop-tracking budget while stressed. |
| `stressed-max-sync-validations-per-tick` | Validation budget while stressed. |
| `fallback-to-vanilla` | Use vanilla tracking if async submit is rejected or remains busy beyond the result-age window. |

Use `/horizon analysis entity-tracking` to inspect runtime counters.
The capture diagnostics report cumulative batches, entities, pairs, average owner-thread
capture time, batch-local shared references, unchanged cross-batch COW states, cache
evictions, and reserved/submitted/not-admitted pairs. `notAdmittedPairs` counts captured
pairs that did not reach a worker, including reservations whose later submission failed.
An increasing `cacheEvictions` value means the bounded per-region player-state cache has
reached its capacity; it is not a tracking correctness failure.

The stressed budgets deliberately exchange tracking convergence latency for lower tick
cost. Pending changes and synchronous validation preserve eventual correctness, but
setting their budgets too low can make client visibility updates noticeably late.
Standard Bukkit/Paper `hideEntity`, `showEntity`, and visible-by-default changes update
tracking immediately. The periodic sweep covers dynamic conditions without such a bridge,
including chunk-tracked and entity broadcast eligibility. To approach vanilla's every-tick
revalidation, set `validation-interval-ticks` to `1` and size
`max-sync-validations-per-tick` to cover the region's tracked pairs; the interval alone is
not a maximum-delay guarantee because validation remains budgeted.
`fallback-to-vanilla` applies to rejected async submission; it does not convert every
deferred or budget-limited apply into an unbounded synchronous pass.
When it is `false`, sustained worker rejection, saturation, or MSPT gating can prevent new
tracking pairs from converging until async submissions recover. Watch `rejectedTasks` and
in-flight saturation in `/horizon analysis entity-tracking` when using this mode.

## Async Sensors

Section: `[async-sensors]`

Default: disabled.

Async sensors move the safe, entity-only nearest-living sensor planning phase to
worker threads using a region-owned snapshot. Sensor memory mutation, all visibility
raycasts, and custom or POI sensors remain on the owning region thread.

Important controls:

| Key | Purpose |
| --- | --- |
| `enabled` | Master switch for the safe async sensor subset. |
| `threads` | Worker thread count. |
| `max-queued` | Maximum queued or running sensor plans. |

Synchronous fallback is mandatory: a saturated queue, failed worker task, stale
result, or failed validation runs the nearest-living sensor on its owner region
thread for that tick.

## Region Files

Section: `[region-files]`

Horizon supports `mca`, `bilinear`, and `buffered_bilinear` backends. Their file
extensions are `.mca`, `.bilinear`, and `.buffered_bilinear`; the buffered mode also
uses a `.swp` journal while state is dirty.

Bilinear and buffered_bilinear are Horizon bilinear storage formats. They are intended
for servers that prefer compact sequential region storage and can accept the
conversion and operational tradeoffs. Always test conversion on a copy of
production worlds first.

Only automatic `mca -> selected Bilinear format` conversion is supported. Horizon
refuses mixed current formats and obsolete `.linear`/`.b_linear` files rather than
silently choosing one. Changing the setting after conversion is therefore not a
rollback mechanism.

See [Bilinear region files](../detail/linear-region-files.md) for every key, extensions,
transactional source deletion, master/swap recovery, format-switch refusal, limits,
backup requirements, and current test coverage.

## Diagnostics

Command root:

```text
/horizon analysis
```

Useful subcommands:

| Command | Purpose |
| --- | --- |
| `/horizon analysis status` | Summary of Horizon async optimization state. |
| `/horizon analysis pathfinding` | Async pathfinding counters. |
| `/horizon analysis chunk-sending` | Async chunk sending counters. |
| `/horizon analysis mob-spawn` | Async mob spawning counters. |
| `/horizon analysis entity-tracking` | Async entity tracking counters. |

The command output uses color to highlight enabled, disabled, fallback, overloaded,
and failure states.

For region MSPT and region count, use the server's supported profiler path for the
current scheduler environment. Some scheduler modes do not support the region profiler;
that means the profiler cannot safely sample per-region timing under that scheduler,
not that regions are disabled.
