# Canvas Upstream Sync

Horizon treats Canvas as its practical upstream, but Canvas is itself a
paperweight/weaver patch project built on Folia. Because of that, Horizon does
not point paperweight directly at the Canvas repository as a source tree.

Instead, Horizon synchronizes Canvas by applying a path-mapped diff from the
last synchronized Canvas commit to a newer Canvas commit.

## Why Not Use Canvas Directly As The Paperweight Upstream?

The Canvas repository contains patch stacks such as:

- `canvas-api/paper-patches`
- `canvas-server/minecraft-patches`
- `canvas-server/paper-patches`
- `canvas-server/build.gradle.kts.patch`

It does not expose a ready-made final source tree for Horizon to patch on top
of. Pointing Horizon's paperweight upstream at Canvas would give Horizon the
Canvas patch project, not the expanded Canvas server source.

Keeping Horizon's existing `horizon-api` and `horizon-server` layout is simpler
than adding a second visible `canvas-server` layer to the working tree.

## Tracked Upstream Commit

`gradle.properties` records the last Canvas commit that has been synchronized:

```properties
canvasCommit = <commit>
```

The sync script diffs that commit against a target Canvas ref and maps Canvas
paths into Horizon paths.

## Path Mapping

The sync script maps these paths:

```text
canvas-api/             -> horizon-api/
canvas-server/          -> horizon-server/
build-data/canvas.at    -> build-data/horizon.at
```

Paths outside those synchronized patch stacks remain Horizon-owned. If Canvas
changes root build logic, helper scripts, CI files, documentation, or other
project infrastructure, review those changes manually and port only the pieces
that make sense for Horizon.

The sync script reports Canvas changes outside synchronized paths, such as:

```text
build.gradle.kts
settings.gradle.kts
gradle.properties
gradle/
gradlew
.github/
scripts/
pre_update.sh
prepare_for_patch_roulette.sh
rbp.sh
```

In `--check` mode those warnings are informational. For a real apply, they are
a safety gate: the script will not advance `canvasCommit` across ignored
changes unless `--ack-ignored` is passed after the manual review.

Canvas helper scripts are Horizon-owned by default because they frequently
contain project layout assumptions. To include helper script changes in the
mapped patch anyway, pass `--include-helper-scripts` and review the result
carefully.

## Workflow

Start from a clean tracked working tree. Untracked local folders are ignored,
but tracked modifications must be committed or stashed first.

Check whether the next Canvas diff applies:

```bash
scripts/sync-canvas-upstream.sh --check
```

Apply the sync:

```bash
scripts/sync-canvas-upstream.sh
```

Then verify the patch stack and generated sources:

```bash
./gradlew applyAllPatches
./gradlew :horizon-api:compileJava :horizon-server:compileJava :horizon-server:test
```

If a conflict appears in generated Minecraft, Paper API, or Paper server
sources, fix the generated source and use the appropriate paperweight/weaver
fixup and rebuild tasks. Do not hand-edit patch files as the source of truth.

Examples:

```bash
./gradlew :horizon-server:fixupMinecraftSourcePatches
./gradlew :horizon-server:rebuildMinecraftSourcePatches
./gradlew :horizon-server:fixupPaperServerFilePatches
./gradlew :horizon-server:rebuildPaperServerFilePatches
./gradlew rebuildFoliaSingleFilePatches
```

## Choosing A Target

By default the script syncs to:

```text
canvas-upstream/ver/26.1.2
```

To test or apply a specific range:

```bash
scripts/sync-canvas-upstream.sh --from <oldCanvasCommit> --to <newCanvasCommit> --check
scripts/sync-canvas-upstream.sh --from <oldCanvasCommit> --to <newCanvasCommit>
```

After a successful apply, the script updates `canvasCommit` to the target
commit and stages the synchronized paths together with `gradle.properties`.

By default the target commit must be a descendant of the recorded
`canvasCommit`, preventing accidental reverse or cross-branch syncs. Use
`--allow-non-ff` only when intentionally replaying a non-linear Canvas range.
