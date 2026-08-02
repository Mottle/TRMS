# TRMS

TRMS is one Gradle multi-project build rooted here. It produces two separately
installed artifacts: the Horizon Extension in `extension/` and the required
NeoForge client Mod in `mod/`.

`common/` is a pure Java 25 library embedded into both artifacts. It owns only
side-neutral protocol and mold-pattern semantics; it contains no Minecraft,
NeoForge, Horizon, rendering, or world-mutation code.

```bash
./gradlew verifyTrms
./gradlew :extension:runHorizonServer
./gradlew :mod:runClient
```

The Extension resolves Horizon solely from locally published Maven artifacts.
It never depends on a Horizon source checkout or on its location.
