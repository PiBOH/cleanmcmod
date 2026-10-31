# Changelog

All notable changes to CleanMCMod are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). `version.txt` is the single source of
truth for the version; a new `## [x.y.z]` section is written in the same commit that bumps it.

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

- Targets in this tree: 1.21.8, 1.21.9, 1.21.10 and 1.21.11, all four built and tested for this
  release. The 26.1, 26.2 and 26.3 branches use the unobfuscated 26.x API names and do not carry
  these features yet: they need the same change ported to their own sources.
