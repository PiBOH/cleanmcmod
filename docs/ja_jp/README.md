# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · **🇯🇵 日本語** · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Minecraft のクライアント側キャッシュ掃除ツールです。導入している MOD と、それらが
使うリソースパックのキャッシュを消します。古いキャッシュは描画の不具合の原因になり
ます。モデルが黒と紫になる、テクスチャが更新されない、リソースパックが壊れる、と
いった症状です。CleanMCMod はそれらを 1 つの画面から消し、消す前に何が消えるかを
見せ、必要なら Modrinth から MOD を更新します。

## 🧹 何を消すか

- `logs/`、`crash-reports/`、ゲームのデバッグ出力
- サーバーが送ってくるリソースパックのキャッシュ
- Fabric が作り直す jar と Mixin のデバッグ出力
- 深いアセットキャッシュ (初期設定ではオフ)
- `config/<mod>/` の中のキャッシュ (VoxelMap と Xaero のマップも含む、フォルダごとにオフ)

どの対象も消す前にサイズを測ります。フォルダ自体は消さず、中のファイルだけを消し
ます。シンボリックリンクはたどりません。`saves/`、`mods/*.jar`、
`resourcepacks/`、`shaderpacks/`、`screenshots/`、直下の `config/*.json` には
決して触れません。

## 🎮 ゲーム内

- **F7** で画面が開きます。タイトル画面にもボタンがあります
- `/cleanmcmod scan`、`/cleanmcmod clean`、`/cleanmcmod screen` はチャットから
- **空振りモード** がオンなら何も消されません

## ⚙️ 設定

スイッチは 8 つで、切り替えた瞬間に `config/cleanmcmod.json` に保存されます。

## ⬆️ MOD の更新

Modrinth に新しいビルドを問い合わせます。確認が終わるまで古い jar は残り、新し
いものが揃ってから置き換えます。自分自身の jar は決して置き換えません。

## 🧩 対応バージョン

1.21.8 から 1.21.11 までが 1 つのコードで `-Pmc=<version>` により、26.1、26.2、
26.3 は Java 25 の専用ブランチで。

## 🤖 翻訳について

このページと MOD の文章は AI の助けを借りて翻訳しています。誤りがある可能性があ
ります。迷ったら英語のページを参照してください。

## 📄 ライセンス

MIT、作者 **PiBOH**。
