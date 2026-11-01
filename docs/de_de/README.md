# ♻️ CleanMCMod

🌍 [🇬🇧 English](../../README.md) · **🇩🇪 Deutsch** · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Clientseitiger Cache-Reiniger fuer Minecraft, fuer die Mods, die du installiert
hast, und fuer die Ressourcenpakete, die sie benutzen. Veraltete Caches sind der
Grund, warum das Spiel falsch zeichnet: fehlende Modelle in Schwarz und Violett,
Texturen, die sich nie erneuern, kaputte Ressourcenpakete. CleanMCMod raeumt sie
von einem Bildschirm aus weg, zeigt vorher, was verschwinden wuerde, und
aktualisiert auf Wunsch Mods von Modrinth.

## 🧹 Was geloescht wird

- `logs/`, `crash-reports/` und die Debug-Dumps des Spiels
- der Cache der Ressourcenpakete, die ein Server schickt
- die umgeschriebenen Fabric-Jars und die Mixin-Debug-Ausgabe
- der tiefe Asset-Cache, standardmaessig aus
- die Cache-Ordner unter `config/<mod>/`, auch die Karten von VoxelMap und Xaero,
  pro Ordner aus

Jedes Ziel wird vorher gemessen. Ordner werden nie selbst geloescht, nur ihre
Dateien; symbolischen Links wird nicht gefolgt; `saves/`, `mods/*.jar`,
`resourcepacks/`, `shaderpacks/`, `screenshots/` und die obersten
`config/*.json` bleiben unberuehrt.

## 🎮 Im Spiel

- **F7** oeffnet den Bildschirm, der Titelbildschirm hat einen Knopf dafuer
- `/cleanmcmod scan`, `/cleanmcmod clean` und `/cleanmcmod screen` aus dem Chat
- der **Probelauf** sagt, dass nichts geloescht wird, bis du ihn ausschaltest

## ⚙️ Einstellungen

Acht Schalter, sofort in `config/cleanmcmod.json` gespeichert.

## ⬆️ Mods aktualisieren

Fragt Modrinth nach einem neueren Build deiner Jars. Alles wird vorher geprueft,
das alte Jar bleibt liegen, bis das neue vollstaendig da ist. Das eigene Jar
wird nie ersetzt.

## 🧩 Versionen

1.21.8 bis 1.21.11 aus einem Quellbaum mit `-Pmc=<version>`, dazu 26.1, 26.2 und
26.3 aus eigenen Branches mit Java 25.

## 🤖 Hinweis zur Uebersetzung

Diese Seite und die Texte der Mod wurden mit Hilfe von KI uebersetzt. Fehler
sind moeglich; im Zweifel gilt die englische Seite.

## 📄 Lizenz

MIT, von **PiBOH**.
