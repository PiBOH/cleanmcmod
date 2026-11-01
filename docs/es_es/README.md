# ♻️ CleanMCMod

🌍 [🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · **🇪🇸 Español** · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Limpiador de cache del lado del cliente para Minecraft, para los mods que tienes
instalados y para los paquetes de recursos que usan. Las caches viejas son la
razon por la que el juego se dibuja mal: modelos ausentes en negro y violeta,
texturas que nunca se refrescan, paquetes rotos. CleanMCMod las borra desde una
sola pantalla, muestra que borraria antes de hacerlo y, si se lo pides, actualiza
los mods desde Modrinth.

## 🧹 Que limpia

- `logs/`, `crash-reports/` y los volcados de depuracion del juego
- la cache de los paquetes de recursos que envia un servidor
- los jars remapeados de Fabric y la salida de depuracion de Mixin
- la cache profunda de recursos, desactivada por defecto
- las carpetas de cache bajo `config/<mod>/`, tambien los mapas de VoxelMap y
  Xaero, desactivadas por carpeta

Cada objetivo se mide antes de borrarlo. Las carpetas no se eliminan nunca, solo
sus archivos; no se siguen enlaces simbolicos; `saves/`, `mods/*.jar`,
`resourcepacks/`, `shaderpacks/`, `screenshots/` y los `config/*.json` de primer
nivel no se tocan.

## 🎮 En el juego

- **F7** abre la pantalla, y la pantalla de titulo tiene un boton para ella
- `/cleanmcmod scan`, `/cleanmcmod clean` y `/cleanmcmod screen` desde el chat
- la linea de **simulacion** avisa de que no se borra nada hasta que la apagues

## ⚙️ Ajustes

Ocho interruptores, guardados al momento en `config/cleanmcmod.json`.

## ⬆️ Actualizar mods

Pide a Modrinth una version mas nueva de tus jars. Todo se comprueba antes, el
jar viejo se queda hasta que el nuevo esta completo. Nunca se sustituye su propio
jar.

## 🧩 Versiones

1.21.8 a 1.21.11 desde un solo codigo con `-Pmc=<version>`, y 26.1, 26.2 y 26.3
desde ramas propias con Java 25.

## 🤖 Nota sobre la traduccion

Esta pagina y los textos del mod se tradujeron con ayuda de IA. Puede haber
errores; si hay duda, vale la pagina en ingles.

## 📄 Licencia

MIT, de **PiBOH**.
