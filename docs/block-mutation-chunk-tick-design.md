# Async chunk-tick planning with BlockMutationQueue

Status: design draft. No vanilla chunk-tick producer is enabled by this document.

## Goal

Move selected, read-heavy chunk-tick decisions to workers while retaining all real
world mutation, Bukkit/Extension event dispatch, neighbor updates, and side effects on
the owning Folia region thread.

The optimization is deliberately whitelist-based. It must never invoke an arbitrary
`BlockState.randomTick` or `FluidState.randomTick` implementation on a worker.

## Current chunk-tick pipeline

The relevant owner-thread order is:

1. scheduled block and fluid ticks in `ServerLevel.tick`;
2. natural spawning and thunder in `ServerChunkCache.tickChunks`;
3. precipitation, ice, and snow selection in `iterateTickingChunksFaster`;
4. block and fluid random ticks in `ServerLevel.optimiseRandomTick`;
5. custom spawners.

The existing BlockMutationQueue drain runs before step 1. Random-tick mutations must
not use that phase by default. They should drain immediately before step 3 so a result
planned by the previous chunk tick preserves the closest practical ordering relative
to spawning, precipitation, and the next random-tick pass.

## Hard boundaries

Workers may:

- read immutable block-section snapshots and captured scalar environment values;
- use a task-local deterministic random source;
- calculate whether one of the supported transition recipes should be attempted;
- create immutable mutation plans;
- publish those plans to the world-owned queue.

Workers may not:

- retain or call `ServerLevel`, `LevelChunk`, a live `LevelChunkSection`, an entity,
  block entity, light engine, POI manager, event bus, or plugin API;
- call `setBlock`, `destroyBlock`, `randomTick`, `scheduleTick`, feature placement,
  entity spawning, loot generation, sound, particles, or game events;
- synchronously load a chunk;
- read a chunk or section owned by another Folia region.

## Commit phases

Introduce an internal `BlockMutationPhase` and associate each producer source with one
phase:

| Phase | Drain point | Initial sources |
| --- | --- | --- |
| `PRE_SCHEDULED_TICKS` | existing `ServerLevel.tick` hook | synthetic and future general planners |
| `PRE_RANDOM_TICKS` | after natural spawning and before `iterateTickingChunksFaster` | random tick and precipitation |

Mailboxes should be partitioned by phase rather than staging entries from all phases
and requeueing the entries that are not currently eligible. World, source, and section
backpressure remains global unless profiling shows a phase can monopolize capacity.

## Planner architecture

Use a Horizon-owned planner in front of BlockMutationQueue:

```text
owner random-tick selection
    -> classify selected BlockState by an explicit supported-family table
    -> reserve bounded worker admission
    -> capture one immutable section batch and candidate scalars
    -> run unsupported/rejected candidates through vanilla on the owner
    -> worker evaluates only supported adapters
    -> worker publishes zero or more immutable BlockMutation plans
    -> next owner chunk-tick phase revalidates and commits
```

Suggested components:

- `HorizonAsyncBlockTickPlanner`: configuration, admission, circuit breaker, metrics;
- `BlockTickBatchSnapshot`: worker-owned snapshot lease and ordered candidates;
- `BlockTickCandidate`: position, expected state, adapter, source tick, sequence,
  random seed, captured brightness, and immutable configuration scalars;
- `BlockTickSnapshotView`: read-only section-coordinate lookup over frozen block-state
  generations;
- `BlockTickPlannerAdapter`: closed internal adapter set, not a public registration API;
- `BlockMutationPublisher`: queue plus immutable settings/routing context, with no world
  reference.

The adapter set should be a switch or sealed internal hierarchy. A reflective or
extension-registerable planner would turn an auditable worker boundary back into an
open asynchronous event bus.

## Snapshot strategy

### First implementation

Batch selected candidates by `LevelChunkSection`. Once a batch contains at least one
supported candidate, retain that section's current `PalettedContainer` generation by
the existing Horizon COW mechanism. Add worker-side indexed reads to the frozen data;
do not copy 4096 block-state references for three default random-tick samples.

The first crop adapter is restricted to candidates whose complete read stencil is in
the captured section:

- local X and Z in `1..14`;
- local Y in `1..15`;
- target chunk loaded and owned by the current region.

Candidates at a section/chunk/region boundary use vanilla. This restriction keeps the
first implementation to one frozen generation per batch and gives benchmarks a clean
snapshot-cost baseline.

### Later multi-section view

Support a union of required section coordinates for all candidates in one batch.
Every section must be loaded and owned by the current region. Missing, unloaded, or
foreign-region sections make only the affected candidate fall back to vanilla.

Snapshot leases must close in all normal, rejected, cancelled, exceptional, and JVM
fatal cleanup paths. A worker must release the COW generation before attempting any
non-essential logging.

### Consistency model

The initial model remains intentionally optimistic:

- target chunk identity must match;
- target `BlockState` must still be the exact expected canonical state;
- owner thread and loaded-chunk status are rechecked at commit;
- a stale neighborhood snapshot may still produce a transition when the target state
  itself did not change.

This is a controlled vanilla-semantic difference, not a Java data race. Adding a
whole-chunk version check to each independent mutation is not sufficient: the first
successful mutation in a batch increments the version and would reject every later
mutation in the same batch. Strict neighborhood consistency would require a staged
batch preflight or explicit dependency states, and should be designed separately if
gameplay testing shows the optimistic model is too permissive.

## Randomness

Do not share the region's mutable `RandomSource` with workers.

After worker admission succeeds, consume one `nextLong()` seed for each offloaded
candidate at its original position in the selected-candidate order. Unsupported
candidates continue to execute vanilla in that same order. The adapter uses only a
task-local random source created from the captured seed.

This does not preserve vanilla's exact number of random draws. It does provide:

- deterministic replay for a captured batch;
- no cross-thread random-source access;
- stable ordering among candidates;
- an immediate vanilla path when worker admission or snapshot capture is rejected.

If admission fails, no async seed is consumed and every candidate follows the existing
vanilla path.

## Commit recipes

Mutation kinds must encode owner-side semantic recipes instead of carrying callbacks.
Callbacks could retain live world state and make cleanup, metrics, and event ordering
unverifiable.

Initial recipes:

| Recipe | Owner-side operation |
| --- | --- |
| `CROP_GROW_RANDOM` | `HorizonCropGrowEventBridge.grow` with random-tick cause |
| `BLOCK_FORM` | existing `CraftEventFactory.handleBlockFormEvent` path |
| `BLOCK_SPREAD_RANDOM` | Horizon crop pre/post plus Bukkit block-spread path |
| `BLOCK_FADE` | Bukkit fade event followed by the exact original block update |
| `MOISTURE_CHANGE` | Bukkit moisture-change path |

Recipes that need a game event, sound, particle, explicit neighbor call, loot, or
entity push must own that post-commit behavior and execute it only when the block
transition succeeds. Do not introduce a generic `Runnable afterCommit` field.

## Candidate families

### Phase 1: implement and benchmark

| Family | Expected value | Snapshot | Commit recipe | Notes |
| --- | --- | --- | --- | --- |
| Standard `CropBlock` growth | high on large farms | current state, 3x3 farmland below, horizontal/diagonal crop states, captured brightness | `CROP_GROW_RANDOM` | include wheat, carrot, potato, beetroot, and torchflower; exclude pitcher crop |
| Safe weathering copper shapes | high computation per successful prefilter | Manhattan radius 4 | `BLOCK_FORM` | start only after multi-section snapshots; exclude block entities, redstone-sensitive shapes, and multi-block doors |
| Moist farmland transitions | potentially high on large farms | 9x9x2 fluid stencil, rain scalar, state above | `MOISTURE_CHANGE` | initially handle moisture decrement/refill only; moisture-zero conversion to dirt remains vanilla |

Standard crops are the first implementation target because they combine a common
server workload with a single target mutation and an existing Horizon/Bukkit event
recipe. Weathering copper is a useful second benchmark because its radius-4 scan is
expensive enough to demonstrate worker benefit, but it needs the multi-section view.

### Phase 2: eligible after the first planner is proven

| Family | Reason it can be planned | Additional requirement |
| --- | --- | --- |
| grass/mycelium spread | bounded reads and up to four independent spread attempts | multi-section view, per-target expected states, spread recipe |
| nylium fade | one target and a small light/above-state predicate | fade recipe; likely too cheap to improve MSPT by itself |
| budding amethyst | one chosen neighbor and one grow/spread event | distinguish spread from grow recipe |
| cocoa and nether wart age | one target and existing crop event bridge | benchmark first; decision is so cheap that async overhead may be larger |
| precipitation ice formation | one target block-form event | captured biome, block light, water-neighbor states |
| fresh single-layer snow | one block-form event | only air-to-snow initially; stacking requires entity-push semantics |
| redstone ore unlight | one fade event and one state transition | likely not worth async overhead; keep as control case |

### Keep synchronous

The following paths should not enter the generic queue planner:

- arbitrary block or fluid `randomTick` virtual calls;
- lava ignition and fluid flow;
- scheduled block/fluid ticks;
- sapling, mushroom, or configured-feature placement;
- bamboo, cactus, sugar cane, stems, pitcher crops, and other multi-block growth until
  each has a dedicated atomic owner-side recipe;
- chorus, vines, pointed dripstone, and spreading structures with recursive or
  multi-target topology decisions;
- leaves and snow melt paths that generate loot or drops;
- turtle eggs and dried ghasts, which spawn entities and emit several side effects;
- eyeblossoms, which schedule nearby ticks and emit sound/particles/game events;
- copper chests and golem statues with block entities;
- copper bulbs, lightning rods, doors, and trapdoors until redstone and multi-block
  semantics have dedicated validation;
- thunder and natural spawning; natural spawning already has its own async planner and
  neither operation is fundamentally a block mutation.

## Admission and failure behavior

Worker admission and snapshot capture happen before replacing the vanilla call:

- executor saturation, circuit-open state, unsupported candidate, foreign ownership,
  or snapshot budget exhaustion always runs vanilla immediately;
- once a worker task is accepted, the task never falls back by touching the world;
- a later queue rejection drops only that speculative transition, records a metric,
  and contributes to the planner failure threshold;
- repeated worker failures or output rejection opens a circuit breaker so subsequent
  candidates return to vanilla until a bounded retry time;
- world close cancels admission, rejects late publication, and releases all snapshot
  leases without requiring a world callback.

The worker task should retain a queue publisher, not `ServerLevel`. This preserves the
current rule that asynchronous work cannot keep an unloading world or chunk alive.

## Configuration

Keep the entire feature disabled until gameplay and JFR validation are complete.
Suggested settings under `[block-mutations]` or a nested random-tick section:

- `async-random-ticks-enabled = false`;
- `async-random-tick-threads`;
- `async-random-tick-max-queued-batches`;
- `async-random-tick-max-batches-per-region-tick`;
- `async-random-tick-max-candidates-per-region-tick`;
- `async-random-tick-max-frozen-sections-per-region-tick`;
- `async-random-tick-max-result-age-ticks`;
- individual family switches for crops, weathering, farmland, and later adapters.

Fallback on admission rejection is a correctness invariant and should not be exposed
as an option.

## Required metrics

Record global and per-family values for:

- selected, supported, admitted, and synchronous-fallback candidates;
- snapshot capture time, frozen handles, retained bytes, and capture rejection reason;
- worker queue delay, planning time, no-op plans, and produced mutations;
- queue admission result;
- applied, state conflict, chunk unloaded/reloaded, ownership retry, and age expiry;
- worker failure and circuit-breaker state;
- COW detach count/bytes while block-tick snapshots are retained;
- owner capture and commit time separately.

The optimization should not be enabled by default unless owner capture plus commit time
is lower than the original random-tick time under realistic farms. Worker throughput by
itself is not evidence of MSPT improvement.

## Test plan

### Pure and concurrency tests

- supported-family classification never accepts an unlisted subclass;
- snapshot batches retain no live world, chunk, section, entity, or block entity;
- frozen section reads are immutable while the live palette detaches on write;
- snapshot handles close exactly once on success, rejection, cancellation, exception,
  fatal error, and world close;
- deterministic candidate seeds produce deterministic plans;
- admission rejection executes every selected candidate exactly once through vanilla;
- worker success never also executes the vanilla path;
- late publication after queue close is rejected without leaking a handle;
- commit phase partitioning cannot drain a random-tick mutation in the scheduled-tick
  phase;
- region split/merge or ownership movement requeues to the current owner;
- target state, chunk identity, deadline, and unloaded chunk rejection remain intact.

### Event and gameplay tests

- crop pre/post, generic grow, and Bukkit grow events run only on the owner thread;
- cancellation prevents mutation and post event;
- event-modified or concurrently changed target state produces a conflict rather than
  overwriting the newer state;
- beetroot and torchflower age/state transitions match their supported planner recipe;
- supported candidates near section, chunk, or region boundaries use vanilla;
- random-tick ordering relative to natural spawn, precipitation, and scheduled ticks is
  asserted from the generated source hook.

### Performance gates

Use at least these JFR scenarios:

1. a normal survival world with few farms;
2. a dense crop farm across many entity-ticking chunks;
3. a large copper weathering build;
4. rapidly mutating sections to expose COW detach cost;
5. worker saturation with mandatory vanilla fallback.

Compare random-tick owner time, complete region MSPT, allocation rate, COW detach bytes,
GC pause, stale/conflict rate, and worker utilization. Remove an adapter if its capture
and detach costs are not lower than the owner work it replaces.

## Implementation order

1. Add per-family timing/count metrics without changing behavior.
2. Add commit-phase partitioning and tests.
3. Add indexed frozen block-state reads and one-section `BlockTickBatchSnapshot`.
4. Implement standard CropBlock planning behind a default-off switch.
5. Run correctness, saturation, COW-detach, and JFR comparisons.
6. Add multi-section snapshot unions only if the crop pilot shows net owner-thread gain.
7. Implement safe weathering copper and moist-farmland adapters independently.
8. Re-evaluate every later family from profiler data rather than enabling all
   syntactically simple `setBlock` paths.

Before any vanilla producer is enabled, also solve or explicitly sweep ownerless
mailboxes: a mutation whose region section disappears currently remains bounded but can
wait until that section returns or the world closes.
