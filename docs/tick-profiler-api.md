# Ticking-chunk profiler API

Horizon exposes a bounded profiler control API for plugins that need an Observable-style view of
tick work across all active chunks. Horizon owns the NMS instrumentation and Folia-safe collection;
plugins own commands, permissions, report storage, ranking, visualization, heat maps, and export.

## Scope

The profiler supports `TickProfileMode.EXACT` and `TickProfileMode.SAMPLED`. Exact mode measures
every supported selected operation during a short wall-clock window. Sampled mode deterministically
selects one complete owner-region tick per configured interval and measures every selected operation
inside that tick. Targets are explicit:

- `TickProfileTarget.serverWide()` dynamically includes every ticking chunk encountered in every loaded server world;
- `TickProfileTarget.worldWide(worldKey)` includes every ticking chunk whenever that logical world is loaded;
- `TickProfileTarget.chunk(worldKey, chunkX, chunkZ)` keeps the focused single-chunk diagnostic mode.

Only one session may be active or finalizing server-wide. The slot remains busy until the closed
report has been merged, which prevents multiple maximum-size reports from accumulating in the
finalizer queue. `HYBRID` remains reserved and currently returns `UNSUPPORTED_MODE`.

Exact collection can independently include:

- scheduled block and fluid ticks;
- random block and fluid ticks;
- block entity ticks;
- non-passenger and passenger entity ticks, including inactive ticks.

It does not scan or estimate the cost of static blocks. A block which performs no supported tick
work produces no result. Chunk loading, generation, lighting, network encoding, plugin tasks,
neighbor updates, redstone propagation, and spawning are not attributed in this version.

## Starting a server-wide window

```java
TickProfileRequest request = new TickProfileRequest(
    TickProfileTarget.serverWide(),
    TickProfileMode.EXACT,
    TickProfileGranularity.TYPE,
    Set.of(
        TickProfileCategory.ENTITY,
        TickProfileCategory.BLOCK_ENTITY,
        TickProfileCategory.SCHEDULED_BLOCK,
        TickProfileCategory.SCHEDULED_FLUID
    ),
    new TickProfileLimits(
        Duration.ofSeconds(10),
        20_000,
        100_000
    )
);

TickProfileStartResult started = HorizonTickProfiler.api().start(plugin, request);
TickProfileSession session = started.session().orElseThrow();

session.completion().thenAccept(result -> {
    result.report().ifPresent(report -> saveReport(report));
});
```

For a lower-overhead broad pass, supply explicit sampling settings:

```java
TickProfileRequest request = new TickProfileRequest(
    TickProfileTarget.serverWide(),
    TickProfileMode.SAMPLED,
    TickProfileGranularity.TYPE,
    categories,
    limits,
    Optional.of(TickProfileSampling.every(10))
);
```

An interval of 10 measures one complete tick out of every 10 ticks of each Folia region. The phase
is deterministically staggered from the session and stable region ID, so regions do not deliberately
produce one synchronized profiling spike. Valid intervals are 2–1,024. `SAMPLED` requires sampling settings;
`EXACT` and `HYBRID` reject one rather than silently ignoring it.

`SUMMARY` and `TYPE` profiles may additionally request approximate invocation-duration
percentiles:

```java
TickProfileRequest request = new TickProfileRequest(
    target,
    mode,
    TickProfileGranularity.TYPE,
    categories,
    limits,
    sampling,
    TickProfileDistribution.PERCENTILES
);
```

Each retained result then exposes `p50`, `p95`, and `p99` through
`TickProfileResult.percentiles()`. Percentiles use a fixed, mergeable logarithmic histogram with two
buckets per power of two. They are approximate upper-bound values with at most 50% bucket error for
positive durations; single-invocation results are exact, and estimates are clamped to the measured
maximum. `NONE` remains the default because histograms add work per invocation and allocate bounded
storage for results which receive multiple observations. Percentiles are deliberately rejected for
`TICK` and `INSTANCE`, where server-wide result cardinality can be very high.

`maximumDuration` is the requested wall-clock observation window. A session completes automatically
with `DURATION_LIMIT`; callers can end it earlier with `stop(session.id())`. Disabling the owner
plugin also stops its active session. The report timestamps expose the actual window, which can be
slightly longer than requested when the duration timer itself is delayed. Report merging happens
after that timestamp and does not extend the recorded observation window.

Starting a world or chunk target does not look up or load that world. The session may be started
from an ordinary plugin thread while the world is absent; it begins collecting if a matching world
appears during the window. A server-wide session likewise admits worlds loaded after it starts.

The server rejects limits above two minutes, 50,000 retained chunks/buckets, or 100,000 retained
result keys. These are safety ceilings rather than recommended defaults. A typical broad profile
should run for 5–30 seconds. `SUMMARY` is the lowest-cost heat-map pass; `TYPE` and especially
`INSTANCE` should use shorter windows and deliberate result limits.

## Chunk identity and normalization

Each `TickProfileResult` contains a `TickProfileChunk`, made of the stable world UUID, Bukkit world
key, and chunk X/Z. UUID prevents two different world instances which reuse a key from being mixed;
an unload/reload of the same world identity is merged.
The report contains two per-chunk counters:

```java
Map<TickProfileChunk, Long> eligibleChunkTicks
Map<TickProfileChunk, Long> sampledChunkTicks
```

`eligibleChunkTicks` counts owner-region ticks in which the chunk participated in `tickChunk` or
executed measured work. `sampledChunkTicks` counts the subset in which operation timing was enabled.
The maps are equal in exact mode. Chunks which load late, unload early, or temporarily stop ticking
therefore retain independent denominators. Consumers should calculate raw heat-map values such as:

```text
measured nanoseconds per sampled chunk tick = chunk result total / sampledChunkTicks[chunk]
```

They must not divide every chunk by one global server tick counter. Folia regions advance
independently, and summing all chunk tick counts does not produce a meaningful global sample count.
The API deliberately returns raw measured durations rather than an estimated total. A consumer may
scale by `eligibleChunkTicks / sampledChunkTicks`, but must label that value as an estimate and warn
when the sample count is small or zero; bursty workloads are not guaranteed to extrapolate linearly.

An entity is attributed using its world and chunk at tick entry. If it crosses a chunk boundary
during the window, it may correctly contribute to different chunk results over time. Block
positions and entity UUIDs are immutable report values and are not live world lookups.

## Granularity and truncation

The aggregation levels are:

| Granularity | Retained dimensions |
| --- | --- |
| `SUMMARY` | chunk and category |
| `TICK` | chunk-local tick index and category |
| `TYPE` | chunk, category, and registry key |
| `INSTANCE` | chunk, category, registry key, and block position or entity UUID |

Every result always includes invocation count, total duration, and maximum single-invocation
duration. When `PERCENTILES` was requested, it also includes approximate p50, p95, and p99 invocation
durations. In sampled mode these describe only measured invocations and are not claimed to be exact
full-window percentiles.

All four levels work with all three target scopes. `TICK` and `INSTANCE` can produce large reports
when used server-wide; the caller is responsible for selecting a short duration and bounded limits.
The server uses allocation-free primitive position/tick maps and nested type maps in region-local
shards, but the retained cardinality is inherently proportional to the requested detail.

`maximumChunks` bounds both stable chunk identities and worker-local aggregation buckets. The latter
matters when a Folia region moves between worker threads: the stable chunk remains one report entry,
but each worker needs private mutable storage. Excessive migration is therefore truncated instead
of multiplying memory by `chunks × workers`. If either budget is exhausted, the report exposes:

- `chunkLimitReached()` and `resultLimitReached()`;
- `droppedChunkObservations()` and `droppedResultObservations()`.

A visualization must display this truncation instead of presenting the report as a complete server
image. Existing retained keys continue accumulating even after new keys are refused.

Reports, their maps, and result lists are immutable snapshots. Durations are inclusive for the
selected operation. For example, entity time includes work invoked inside that entity's `tick()`;
nested supported work can also appear in another category, so categories must not be blindly summed
as mutually exclusive server time.

## Folia and performance semantics

At the beginning of each owner-region tick, Horizon acquires a lifecycle scope and makes the sampling
decision once. All chunks actually ticked by that region are recorded in a region-worker-local
primitive map. In sampled mode, an unselected operation hook returns without reading a timer,
registry identity, entity chunk, or UUID. Region completion updates eligible and sampled counters
and releases the scope. Stopping closes new admission and waits for both already-started region ticks
and already-started timing observations before finalizing.

No per-tick Bukkit event or plugin callback runs on the hot path. Region workers never contend on a
shared result map. Composite API keys are created only during final report assembly; broad `SUMMARY`,
`TICK`, and `TYPE` collection do not allocate a composite key for every measured invocation.
Registry keys and entity UUIDs are read only when the selected granularity needs them.

When no session is active, each operation hook performs one volatile active-session read and a null
branch. It does not call `System.nanoTime()`, create a timing token, update a map, or inspect entity
identity. An inactive region performs one null check at region start and one empty thread-local check
at region completion.

During an exact window, every selected invocation pays two timer reads and chunk-local aggregation.
During a sampled window, selected region ticks pay approximately the same cost while other operation
hooks take a fast null branch; maintaining the full eligible denominator still costs one lightweight
chunk mark for each actually ticking chunk. Sampling therefore reduces the dominant timer and result
aggregation cost toward `1 / interval`, but does not reduce all profiler bookkeeping to that ratio.
Both modes are intended for diagnostics, not permanent operation. Deadline/owner checks and report
merging use separate Horizon daemon executors, so sorting a large completed report cannot delay the
next window's duration timer or block a region worker.

Completion callbacks have no Bukkit/Folia thread ownership. Reading, serializing, and storing the
immutable report is safe there; any access to live server state must first be scheduled to the
appropriate global, region, or entity owner.

`TickProfilerPerformanceTest` exercises the inactive fast path, exact collection, sampled collection
(`every(10)`), and the optional percentile histogram. It prints ns/op and the observed sampling
ratio, but intentionally has no absolute pass/fail timing threshold: CI scheduling and CPU frequency
make such thresholds unreliable. Use it to compare builds on the same machine; use an A/B run on a
representative server workload for TPS, CPU, and allocation conclusions.
