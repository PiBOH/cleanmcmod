# AGENTS.md

Instructions for AI coding agents working in this repository.

## 1. Project overview

**CleanMCMod** is a **client-only** Minecraft mod that clears the caches which
make the game render wrong: stale `logs/`, crash reports, debug dumps, the
server resource-pack cache, Fabric's remapped-jar cache, Mixin debug output and
the asset cache, plus the per-mod cache folders that mods create under
`config/<mod>/`. It also lists the mods installed in `mods/` and, on request,
installs newer versions from the Modrinth API.

- **Language/build**: Java 21 source/target, Gradle (Fabric Loom 1.19.3).
- **Loader**: Fabric Loader. The jar is built against `piboh.cleanmc.mod` and
  declares `environment: client` — it never runs on a dedicated server.
- **Targets**: Minecraft 1.21.8, 1.21.9, 1.21.10 and 1.21.11 (Java 21) — all
  four compile, each with its own CI workflow. 26.1 / 26.2 / 26.3 ship
  **unobfuscated**: no Yarn mappings exist for them, so they need Loom's
  non-remapping plugin, the official class names and Java 25 and live on the
  `26.1` / `26.2` / `26.3` branches instead of this tree. Here the version is a
  build dimension (`-Pmc=`), not a branch of code.
- **Version**: `version.txt` at the repo root is the single source of truth.
  `build.gradle` reads it; `fabric.mod.json` is expanded from it. Never
  hard-code a version in Java or in the JSON.

## 2. Architecture and package structure

Root package `piboh.cleanmc.mod`, four concerns, one owner of state each:

| Package | Files | Role |
|---|---|---|
| `clean` | `CacheTargets`, `CacheTarget`, `CacheCategory`, `CacheSafety`, `CacheScanner`, `CacheScan`, `TargetState`, `CacheAutoDetector`, `CacheCleaner`, `CleanReport`, `TargetResult`, `TargetStatus` | Pure filesystem work: what counts as a cache, how it is measured, how it is deleted. No Minecraft classes. |
| `config` | `CleanMcModConfig` | The eight settings + `extraPaths`, stored as `config/cleanmcmod.json`. Gson, saved on every toggle. |
| `update` | `ModScanner`, `LocalMod`, `ModVersion`, `Dependency`, `ModrinthClient`, `Updates`, `ModUpdate`, `Compatibility`, `VersionRange`, `ModUpdaterService`, `ModInstaller`, `VersionComparator` | Installed-mod scan, Modrinth queries, the update decision, the dependency pre-flight (`Compatibility`/`VersionRange`: install, warn or block with a reason, plus the plan that fetches the missing required dependencies one level deep), the download/verify/replace step. |
| `ui` | `CleanMenuScreen`, `CacheSettingsScreen`, `ModUpdatesScreen`, `AbstractUpdatesScreen`, `ConfirmInstallScreen`, `Texts`, `MenuKey` | Rendering and input only. Screens read state, call a service, draw the result. `ModUpdatesScreen` and `MenuKey` are two-file shims under `src/compat/<api>/` because the click and key-binding signatures changed in 1.21.9; everything else lives in `src/main/java`. |
| `command` | `CleanMcModCommand` | `/cleanmcmod scan|clean|screen` through Fabric's client command API. |
| `compat` | `CleanMcModMenu` | Optional Mod Menu integration, named only by the `modmenu` entrypoint. Compiled against `modmenu` with `modCompileOnly`, so a game without Mod Menu never loads it; `modmenu` is `suggests`, never `depends`. |
| root | `CleanMcMod`, `CleanMcModClient` | Mod initializer + static service accessors. |

Rules of ownership:

- `CleanMcModService` owns scanning/cleaning state (`lastScan`, `lastReport`,
  `running`) and is the only thing screens and the command talk to.
- `ModUpdaterService` owns update state through an immutable `Snapshot`
  (`IDLE`/`CHECKING`/`READY`/`INSTALLING`/`FAILED`, updates, outcomes, error).
  Screens must never mutate it; they call `checkAsync()` / `installAsync()` and
  read `snapshot()`.
- Config is one object, loaded once and saved through `CleanMcModService`.
  Do not add a second settings store, a second JSON file, or per-screen fields
  that shadow a setting.
- `Texts` is the only place that maps a `TargetResult` to a colour or a status
  label. Do not inline new colours in a screen.

## 3. Code conventions

- **Never block the render thread on I/O or HTTP.** Cleaning runs on the
  `cleanmcmod-cleaner` daemon thread, updates on `cleanmcmod-updater`; results
  come back through a future/`Snapshot` and screens re-read them in `render`.
- **Screen code draws, it does not compute.** If a screen starts growing logic
  (a decision, a filesystem walk, a version comparison), it belongs in a
  service or in the corresponding pure class.
- **Colours are ARGB.** `DrawContext.drawText`/`drawCenteredTextWithShadow`
  take `0xAARRGGBB`; `0xFFFFFF` renders nothing (alpha 0). Use `0xFFFFFFFF` for
  white and the `Texts.COLOR_*` constants for everything else.
- **Never let a screen print a raw translation key.** Every user-visible string
  goes through `Text.translatable(...)` and exists in **every** file under
  `src/main/resources/assets/cleanmcmod/lang/`. See §6.
- **Cache cleaning must stay conservative.** Only delete inside the paths
  produced by `CacheTargets`/`CacheAutoDetector`; never delete a directory, only
  its files; never follow symlinks; never touch `saves/`, `mods/*.jar`,
  `resourcepacks/`, `shaderpacks/`, `screenshots/` or `config/*.json` at the
  top level. `CacheSafety` documents which targets the game regenerates and
  which are re-downloaded.
- **Dry run means dry.** `CleanReport.filesDeleted()`/`bytesFreed()` must be 0
  in a dry run; `filesPlanned()`/`bytesPlanned()` carry the would-be numbers.
  Do not "fix" a total by relabelling it.
- **The updater must never destroy a working mod.** Download to a temp file,
  verify the SHA-1 Modrinth publishes, move the old jar aside, move the new one
  in, and restore the old one on any failure. Never delete the only working
  copy before the replacement is fully written.
- **A missing required dependency is fetched, not just reported.** A dependency that
  is absent, or installed at a different build than the candidate pins, counts as
  missing: `ModUpdaterService` resolves it from Modrinth for the running game version
  and loader (and its own required dependencies, one level only) and installs the
  whole set in one batch, then deletes the dependency jar it replaced — never a jar
  the batch itself just wrote:
  every jar is downloaded and hash-checked before any of them is moved into
  `mods/`. If one cannot be resolved, the update stays blocked with the original
  reason and nothing is downloaded or modified.
- **No self-update.** CleanMCMod never replaces its own jar while it is loaded.
- Keep changes small and in the existing structure; no new dependency for a
  few lines; no abstraction, option or "for later" code nobody asked for.

## 4. Build, run and test

The wrapper is checked in (`gradle/wrapper`, Gradle 9.8.2, distribution
checksum pinned). Two JDKs are needed: Loom 1.19.3 refuses Java 21 as its own
JVM, so Gradle and Loom run on **JDK 25**, while `build.gradle` pins a Java 21
toolchain that must also be installed (no toolchain download repositories are
configured). CI installs both and passes the 21 home through
`-Porg.gradle.java.installations.paths`; locally a JDK 21 in a standard
location is auto-detected, otherwise pass the same property:

```bash
export JAVA_HOME=/path/to/jdk-25
./gradlew clean build      # tests + jar
./gradlew test             # JUnit only
./gradlew runClient        # dev client, game dir ./run

# only if the Java 21 toolchain is not auto-detected:
./gradlew clean build -Porg.gradle.java.installations.paths=/path/to/jdk-21
```

- The test suite must stay green (`50` tests at the time of writing). Tests are
  plain JUnit 5 and cover the pure logic: cache cleaning, auto-detection
  depth, config round-trip, version comparison, update selection, Modrinth
  payload parsing, installer safety, updater state transitions.
- A green `build` is not proof of UI correctness: screens are only proven by
  running the client (see `run/screenshots/`, F2) or by the layout being read
  against `TextRenderer` widths.
- Dev game dir is `run/`. Cleaning it is safe; the settings file lives in
  `run/config/cleanmcmod.json`.

## 5. Golden rule: "If it works, don't touch it"

The cache cleaner and the updater are the product. Do not rewrite a working
code path to make it prettier, do not swap the Modrinth client, do not rework
the screens' layout without a screenshot proving the current one is broken.
Refactors happen only when a behaviour must change anyway.

## 6. Localization (multilingual support)

- Canonical file: `assets/cleanmcmod/lang/en_us.json`. **It is the source of
  truth for keys**; every other file must have exactly the same key set and the
  same number of `%s` placeholders in the same string.
- The reference project this mod follows, `PiBOH/vivi-music-de`, ships 47
  translated language variants (`app/src/main/res/values-*` with a
  `strings.xml`):

  ```
  ar as b+sr+Latn be bg bn bn-rIN bs ca cs de el en-rCA es es-rUS et fa fi fil
  fr hi hr hu in it iw ja ko ml nb-rNO nl pa pl pt pt-rBR ro ru sk sl sv ta te
  tr uk vi zh-rCN zh-rTW
  ```

- Shipped so far (Minecraft locale codes under `assets/cleanmcmod/lang/`):
  `de_de es_es fr_fr it_it ja_jp ko_kr nl_nl pl_pl pt_br ru_ru tr_tr uk_ua
  zh_cn zh_tw` (+ `en_us`). The rest of the reference list is still to come;
  a language only counts as supported when its file passes the parity check.
- Mapping notes when adding one: `pt-rBR`→`pt_br`, `zh-rCN`→`zh_cn`,
  `zh-rTW`→`zh_tw`, `nb-rNO`→`nb_no`, `iw`→`he_il`, `in`→`id_id`,
  `es-rUS`→`es_mx`, `en-rCA`→`en_ca`, `fil`→`fil_ph`. Android qualifiers with
  no Minecraft equivalent (`as`, `bn`, `bn-rIN`, `km`, `ml`, `mte`, `pa`,
  `te`, `wae`) cannot be shipped: the game would never load them.
- Parity check (run before every commit that touches a lang file):

  ```bash
  python - <<'PY'
  import json, glob, os, re
  base = json.load(open("src/main/resources/assets/cleanmcmod/lang/en_us.json", encoding="utf-8"))
  for p in sorted(glob.glob("src/main/resources/assets/cleanmcmod/lang/*.json")):
      d = json.load(open(p, encoding="utf-8"))
      assert set(d) == set(base), f"{p}: key drift"
      for k in base:
          assert base[k].count("%s") == d[k].count("%s"), f"{p}: placeholder mismatch in {k}"
  print("parity ok")
  PY
  ```

- Keep `%s` placeholders and the `✔`/`✘` markers; translate the words around
  them. Do not translate mod names, `/cleanmcmod` or the key `F7`.

## 7. Versioning

- `version.txt` holds `MAJOR.MINOR.PATCH`. `build.gradle` builds
  `<version.txt>+<minecraft_version>`; the jar name and `fabric.mod.json`
  version come from it.
- Bump `version.txt` only when the released artifact changes meaning. Never
  write a version literal into Java code, `fabric.mod.json` or the README.

## 8. Modrinth / CurseForge policy — mandatory

- The mod is **client-side only** and must stay that way (`environment:
  client`, no server entrypoint, no packet handling).
- **Do not implement, stub, hide behind a setting or ship**: hiding installed
  mods from a server, defeating anti-cheat or mod-detection probes, or
  pretending a server resource pack was accepted. Those behaviours violate the
  Modrinth content rules and server terms of service. The user's original
  request mentioned them; they are deliberately absent and will stay absent.
- Do not bundle Mojang assets, mods, or resource packs. Downloads happen at
  runtime, from Modrinth, on the user's request.
- API use is polite: no polling loops, no background scheduler, no telemetry.

## 9. What is not implemented (do not claim otherwise)

- No mod hiding, no anti-cheat evasion, no server resource-pack bypass.
- No Quilt-specific testing (the code is Fabric only; Quilt compatibility is
  unproven).
- No 26.x builds on this branch: those targets are unobfuscated, need Java 25
  and live on the `26.1` / `26.2` / `26.3` branches.
- No `/cleanmcmod` verification inside a loaded world (the command is exercised
  through the same service as the GUI button).
- No published release; CI only compiles and tests (`./gradlew build`), it
  does not publish. The language set above is partial.

## 10. Ask before assuming

If a request implies deleting user data (a new cache target outside the known
set, an `extraPaths` entry, a "clean everything" button), ask first: which
path, why, and whether the game regenerates it. Wrong deletions are not
recoverable.
