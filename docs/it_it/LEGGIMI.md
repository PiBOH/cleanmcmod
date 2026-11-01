# ♻️ CleanMCMod

🌍 [🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · **🇮🇹 Italiano** · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Pulitore di cache lato client per Minecraft, per le mod che hai installato e per i
pacchetti di risorse che usano. Le cache vecchie sono il motivo per cui il gioco
disegna male: modelli mancanti neri e viola, texture che non si aggiornano,
pacchetti rotti. CleanMCMod le cancella da una sola schermata, mostra che cosa
cancellerebbe prima di farlo e, se glielo chiedi, aggiorna le mod da Modrinth.

## 🧹 Che cosa cancella

- `logs/`, `crash-reports/` e i dump di debug del gioco
- la cache dei pacchetti di risorse che manda un server
- i jar rimappati di Fabric e l'output di debug di Mixin
- la cache profonda delle risorse, spenta per impostazione predefinita
- le cartelle di cache sotto `config/<mod>/`, comprese le mappe di VoxelMap e
  Xaero, spente cartella per cartella

Ogni bersaglio viene misurato prima di essere cancellato. Le cartelle non vengono
mai rimosse, solo i loro file; i collegamenti simbolici non vengono seguiti;
`saves/`, `mods/*.jar`, `resourcepacks/`, `shaderpacks/`, `screenshots/` e i
`config/*.json` in primo livello non vengono mai toccati.

## 🎮 In gioco

- **F7** apre la schermata, e la schermata del titolo ha un pulsante per lei
- `/cleanmcmod scan`, `/cleanmcmod clean` e `/cleanmcmod screen` dalla chat
- la riga **prova a vuoto** avvisa che non viene cancellato niente finche' e'
  accesa

## ⚙️ Impostazioni

Otto interruttori, salvati subito in `config/cleanmcmod.json`.

## ⬆️ Aggiornare le mod

Chiede a Modrinth una versione piu' recente dei tuoi jar. Tutto viene verificato
prima, il vecchio jar resta al suo posto finche' il nuovo non e' completo. Il
proprio jar non viene mai sostituito.

## 🧩 Versioni

Da 1.21.8 a 1.21.11 da un solo codice con `-Pmc=<versione>`, poi 26.1, 26.2 e
26.3 dai rispettivi rami su Java 25.

## 🤖 Nota sulla traduzione

Questa pagina e i testi della mod sono stati tradotti con l'aiuto di un'AI.
Possono esserci errori; nel dubbio vale la pagina in inglese.

## 📄 Licenza

MIT, di **PiBOH**.
