# Changelog

All notable changes to CleanMCMod are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). `version.txt` is the single source of
truth for the version; a new `## [x.y.z]` section is written in the same commit that bumps it.

## [0.3.4] - 2026-10-31

### Fixed

- **Updates now install while the game is running.** Minecraft keeps every jar in `mods/` open, so
  Windows refuses to replace one: the new build is written under its own file name and the jar it
  replaces is removed on the next launch, or deleted at once when Windows lets go of it. Nothing is
  ever written over a jar the game has open, which is the method Mod Menu Updater uses.
- **The list no longer offers a build that is already installed.** Two sources spell the same
  version their own way (`3.0.10+mc26.3` and `26.3-3.0.10`), and those rows tried to replace the open
  jar and failed. In the reported pack five of fourteen rows were of that kind.
- **A mod icon is also read from a root `icon.png`**, which CurseForge mods often ship, and a jar
  whose icon cannot be decoded now says why in the log instead of dropping to the initial tile
  silently.

## [0.3.3] - 2026-10-31

### Fixed

- **A mod that cannot be downloaded no longer cancels the whole install.** The other mods in the
  request are installed and the alert names the jar that failed, instead of losing every download
  because of one bad file. A missing required dependency still cancels the batch, which is what keeps
  a mod from being installed without the library it needs.
- **A failed install says why, on screen and in the log.** The reason names the file plus the HTTP
  status or the checksum mismatch, a download is retried once before giving up, and the task ends as a
  visible failure instead of leaving the screen on Installing forever.

## [0.3.2] - 2026-10-31

### Fixed

- **Rows in the update list are clickable again.** Every row is now a real button under the drawn
  table, so a click anywhere on a row ticks its box and "Download selected" enables immediately.
- **The update list shows each mod's own icon.** It is read from `assets/<modid>/icon.png` in the jar
  that is already in `mods/`, so nothing is downloaded; a jar without a usable icon keeps the tile
  with the mod's initial.

## [0.3.1] - 2026-10-31

### Added

- **About group in the settings.** It names the creator, PiBOH, and the mods this one took its layout
  and its update list from: ModMenuUpdater, Sodium and Reese's Sodium Options.
- **Language row in the settings.** It shows the current language and opens the game's own language
  selector, so the language can be changed without leaving the mod.
- **Update list as a table.** Icon, mod name, installed version, new version and a checkbox on every
  row; a click on a row ticks its box, and a single row can be downloaded on its own.
- **Progress bar with a percentage** while updates are checked and downloaded, driven by the steps the
  updater has really finished, never by a timer.
- **Result alert when a download finishes.** One line per mod saying whether it was updated and to
  which version, instead of leaving the answer in the log.
- Mod Menu's button now opens the main screen, so `Update mods...` is one click from Mod Menu.

### Changed

- **Settings laid out like Sodium.** One scrolling page with a search field, a header per group and
  the control on the right. The side column of categories is gone, so nothing next to the list can be
  left unclickable.
- The bulk action is called **Download selected** and **Download all** instead of `Install all`.
- Sources are preferred in the order Modrinth, GitHub, CurseForge, and the source tag is coloured:
  Modrinth green, GitHub dark violet, CurseForge orange.
- No long dash in any shipped string.

### Fixed

- Two builds of the same mod no longer produce two rows: the newest one is shown, and updating the
  older copy is no longer offered.
- A row in the update list responds to a click again, including when two rows pointed at the same
  project.

## [0.3.0] - 2026-10-31

### Added

- **Three update sources.** Mod updates are checked on Modrinth, CurseForge and GitHub and merged
  into one list. Modrinth is matched by file hash; CurseForge by project slug through its public
  read-only mirror; GitHub through the repository a mod names in its own `contact` block. When two
  sources publish the same version, the one that publishes a checksum wins.
- **Mod Menu Updater hand-over.** When `Mod Menu Updater` is installed, CleanMCMod lists what is
  outdated and installs nothing: the update screen says so and the install buttons disappear, so two
  updaters never write the same jars.
- **Keyboard shortcuts.** `F7` opens the menu and `F8` opens Mod Updates; the menu footer shows
  both keys.
- **VoxelMap and Xaero caches.** New `includeMapCaches` setting (on by default) that finds the
  `cache` folders of VoxelMap and Xaero's Minimap / World Map and deletes their contents. Map data,
  waypoints and settings are never touched, so the stale sprites that break mob rendering are rebuilt
  without losing the player's maps.
- **Reese's Sodium Options–style settings screen.** Category tabs on the left, a search field above
  the list, one row per option with its toggle on the right, a live size per row, a marker on rows
  that differ from the default, a detail line at the bottom and a `Reset` button.
- `CHANGELOG.md`, a rewrite of `AGENTS.md` covering versioning, changelog and workflow, and
  `scripts/update_lang.py` to keep the language key set in one place.

### Changed

- The shared jar installer verifies a download only when the source publishes a hash, and says in the
  log when it cannot. CurseForge and GitHub publish none, so those jars install unverified by design.
- The update row shows its source (`[MR]`, `[CF]`, `[GH]`), and hovering it names the source in full.

### Notes

- All seven release branches ship in this release: 1.21.8, 1.21.9, 1.21.10 and 1.21.11 on Java 21,
  26.1, 26.2 and 26.3 on Java 25. The 26.x line draws the settings rows itself, so it has no search
  box there; every other feature is identical.
