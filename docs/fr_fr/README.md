# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · **🇫🇷 Français** · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Nettoyeur de cache cote client pour Minecraft, pour les mods que tu as installes
et pour les packs de ressources qu'ils utilisent. Les caches perimes sont la
raison pour laquelle le jeu s'affiche mal : modeles manquants en noir et violet,
textures qui ne se rafraichissent jamais, packs casses. CleanMCMod les supprime
depuis un seul ecran, montre ce qu'il supprimerait avant de le faire et, sur
demande, met a jour les mods depuis Modrinth.

## 🧹 Ce qu'il nettoie

- `logs/`, `crash-reports/` et les vidages de debogage du jeu
- le cache des packs de ressources envoyes par un serveur
- les jars remappes de Fabric et la sortie de debogage de Mixin
- le cache profond des ressources, desactive par defaut
- les dossiers de cache sous `config/<mod>/`, y compris les cartes de VoxelMap et
  Xaero, desactives dossier par dossier

Chaque cible est mesuree avant d'etre supprimee. Les dossiers ne sont jamais
supprimes, seulement leurs fichiers ; les liens symboliques ne sont pas suivis ;
`saves/`, `mods/*.jar`, `resourcepacks/`, `shaderpacks/`, `screenshots/` et les
`config/*.json` de premier niveau ne sont jamais touches.

## 🎮 En jeu

- **F7** ouvre l'ecran, et l'ecran de titre a un bouton pour lui
- `/cleanmcmod scan`, `/cleanmcmod clean` et `/cleanmcmod screen` depuis le chat
- la ligne de **simulation** previent que rien n'est supprime tant qu'elle est
  active

## ⚙️ Reglages

Huit interrupteurs, enregistres aussitot dans `config/cleanmcmod.json`.

## ⬆️ Mettre a jour les mods

Demande a Modrinth une version plus recente de tes jars. Tout est verifie avant,
l'ancien jar reste en place jusqu'a ce que le nouveau soit complet. Son propre jar
n'est jamais remplace.

## 🧩 Versions

1.21.8 a 1.21.11 depuis un seul code avec `-Pmc=<version>`, puis 26.1, 26.2 et
26.3 depuis leurs propres branches sous Java 25.

## 🤖 Note sur la traduction

Cette page et les textes du mod ont ete traduits avec l'aide d'une IA. Des
erreurs sont possibles ; en cas de doute, la page anglaise fait foi.

## 📄 Licence

MIT, par **PiBOH**.
