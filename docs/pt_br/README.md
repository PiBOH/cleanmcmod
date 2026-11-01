# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · **🇧🇷 Português (Brasil)** · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Limpador de cache do lado do cliente para Minecraft, para os mods que voce tem
instalados e para os pacotes de recursos que eles usam. Caches velhos sao o
motivo de o jogo desenhar errado: modelos ausentes em preto e roxo, texturas que
nunca atualizam, pacotes quebrados. O CleanMCMod limpa tudo em uma tela, mostra o
que seria apagado antes de apagar e, se voce pedir, atualiza os mods pelo
Modrinth.

## 🧹 O que ele limpa

- `logs/`, `crash-reports/` e os despejos de depuracao do jogo
- o cache dos pacotes de recursos enviados por um servidor
- os jars remapeados do Fabric e a saida de depuracao do Mixin
- o cache profundo de recursos, desligado por padrao
- as pastas de cache em `config/<mod>/`, inclusive os mapas do VoxelMap e do
  Xaero, desligadas pasta por pasta

Cada alvo e medido antes de ser apagado. As pastas nunca sao removidas, apenas os
arquivos; links simbolicos nao sao seguidos; `saves/`, `mods/*.jar`,
`resourcepacks/`, `shaderpacks/`, `screenshots/` e os `config/*.json` de primeiro
nivel nunca sao tocados.

## 🎮 No jogo

- **F7** abre a tela, e a tela de titulo tem um botao para ela
- `/cleanmcmod scan`, `/cleanmcmod clean` e `/cleanmcmod screen` pelo chat
- a linha de **simulacao** avisa que nada e apagado enquanto ela estiver ligada

## ⚙️ Configuracoes

Oito opcoes, salvas na hora em `config/cleanmcmod.json`.

## ⬆️ Atualizar mods

Pede ao Modrinth uma versao mais nova dos seus jars. Tudo e verificado antes, o
jar antigo fica no lugar ate o novo estar completo. O proprio jar nunca e
substituido.

## 🧩 Versoes

1.21.8 a 1.21.11 com um unico codigo e `-Pmc=<version>`, e 26.1, 26.2 e 26.3 em
branches proprias com Java 25.

## 🤖 Sobre a traducao

Esta pagina e os textos do mod foram traduzidos com ajuda de IA. Podem existir
erros; na duvida, vale a pagina em ingles.

## 📄 Licenca

MIT, por **PiBOH**.
