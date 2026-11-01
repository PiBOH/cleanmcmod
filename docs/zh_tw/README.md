# ♻️ CleanMCMod

🌍 [🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · **🇹🇼 繁體中文**

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Minecraft 用戶端快取清理工具，清理你安裝的模組以及它們使用的資源包留下的快取。
老舊的快取正是遊戲顯示錯誤的原因：模型遺失變成黑紫色、貼圖一直不更新、資源包損
壞。CleanMCMod 在一個畫面裡完成清理，刪除前先告訴你範圍，需要時還能從 Modrinth
更新模組。

## 🧹 清理內容

- `logs/`、`crash-reports/` 和遊戲的除錯傾印
- 伺服器下發的資源包快取
- Fabric 重新對應的 jar 和 Mixin 除錯輸出
- 深層資源快取，預設關閉
- `config/<mod>/` 下的快取資料夾，包含 VoxelMap 和 Xaero 的地圖，逐個資料夾關閉

每一項在刪除前都會先測量。資料夾本身從不刪除，只刪裡面的檔案；不跟隨符號連結；
`saves/`、`mods/*.jar`、`resourcepacks/`、`shaderpacks/`、`screenshots/` 以及
最上層的 `config/*.json` 絕不觸碰。

## 🎮 遊戲內

- **F7** 開啟畫面，標題畫面也有按鈕
- 聊天中可用 `/cleanmcmod scan`、`/cleanmcmod clean`、`/cleanmcmod screen`
- **試跑**那一行會提示：開啟時不會刪除任何東西

## ⚙️ 設定

八個開關，變更後立即寫入 `config/cleanmcmod.json`。

## ⬆️ 更新模組

向 Modrinth 查詢你 jar 的更新版本。全部先驗證，新檔案完整之前舊的 jar 一直留著。
它自己的 jar 從不替換。

## 🧩 支援的版本

1.21.8 到 1.21.11 用同一份程式碼加 `-Pmc=<version>`，26.1、26.2、26.3 在各自的
分支上用 Java 25。

## 🤖 關於翻譯

這一頁和模组的文字藉助 AI 翻譯，可能有錯誤；有疑問時以英文頁面為準。

## 📄 授權

MIT，作者 **PiBOH**。
