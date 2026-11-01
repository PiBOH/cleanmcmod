# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · **🇳🇱 Nederlands** · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Cache-opruimer aan de clientkant voor Minecraft, voor de mods die je hebt
geinstalleerd en voor de resourcepacks die ze gebruiken. Oude caches zijn de
reden dat het spel verkeerd tekent: ontbrekende modellen in zwart en paars,
textures die nooit verversen, kapotte resourcepacks. CleanMCMod ruimt ze op
vanuit een scherm, laat eerst zien wat er zou verdwijnen en werkt op verzoek
mods bij via Modrinth.

## 🧹 Wat het opruimt

- `logs/`, `crash-reports/` en de debug-dumps van het spel
- de cache van resourcepacks die een server stuurt
- de omgeschreven Fabric-jars en de debug-uitvoer van Mixin
- de diepe assetcache, standaard uit
- de cachemappen onder `config/<mod>/`, ook de kaarten van VoxelMap en Xaero,
  per map uit

Elk doel wordt gemeten voordat het verdwijnt. Mappen worden nooit zelf verwijderd,
alleen hun bestanden; symbolische links worden niet gevolgd; `saves/`,
`mods/*.jar`, `resourcepacks/`, `shaderpacks/`, `screenshots/` en de bovenste
`config/*.json` blijven ongemoeid.

## 🎮 In het spel

- **F7** opent het scherm, en het titelscherm heeft er een knop voor
- `/cleanmcmod scan`, `/cleanmcmod clean` en `/cleanmcmod screen` vanuit de chat
- de regel **Proefrun** zegt dat er niets verdwijnt tot je hem uitzet

## ⚙️ Instellingen

Acht schakelaars, meteen opgeslagen in `config/cleanmcmod.json`.

## ⬆️ Mods bijwerken

Vraagt Modrinth om een nieuwere build van je jars. Alles wordt eerst gecontroleerd,
de oude jar blijft staan tot de nieuwe compleet is. De eigen jar wordt nooit
vervangen.

## 🧩 Versies

1.21.8 tot 1.21.11 uit een code met `-Pmc=<version>`, en 26.1, 26.2 en 26.3 uit
eigen branches op Java 25.

## 🤖 Over de vertaling

Deze pagina en de teksten van de mod zijn met hulp van AI vertaald. Fouten zijn
mogelijk; bij twijfel geldt de Engelse pagina.

## 📄 Licentie

MIT, door **PiBOH**.
