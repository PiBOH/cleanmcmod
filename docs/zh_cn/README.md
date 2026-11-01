# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · **🇨🇳 简体中文** · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Minecraft 客户端缓存清理工具，清理你安装的模组以及它们使用的资源包留下的缓存。
陈旧的缓存正是游戏显示错误的原因：模型丢失变成黑紫色、纹理一直不刷新、资源包损
坏。CleanMCMod 在一个界面里完成清理，删之前先告诉你范围，需要时还能从 Modrinth
更新模组。

## 🧹 清理内容

- `logs/`、`crash-reports/` 和游戏的调试转储
- 服务器下发的资源包缓存
- Fabric 重新映射的 jar 和 Mixin 调试输出
- 深层资源缓存，默认关闭
- `config/<mod>/` 下的缓存文件夹，包括 VoxelMap 和 Xaero 的地图，逐个文件夹关闭

每一项在删除前都会先测量。文件夹本身从不删除，只删里面的文件；不跟随符号链接；
`saves/`、`mods/*.jar`、`resourcepacks/`、`shaderpacks/`、`screenshots/` 以及
顶层的 `config/*.json` 绝不触碰。

## 🎮 游戏内

- **F7** 打开界面，标题界面也有按钮
- 聊天里可用 `/cleanmcmod scan`、`/cleanmcmod clean`、`/cleanmcmod screen`
- **试运行**那一行会提示：开启时不会删除任何东西

## ⚙️ 设置

八个开关，改动后立即写入 `config/cleanmcmod.json`。

## ⬆️ 更新模组

向 Modrinth 查询你 jar 的更新版本。全部先校验，新文件完整之前旧的 jar 一直留着。
它自己的 jar 从不替换。

## 🧩 支持的版本

1.21.8 到 1.21.11 用同一份代码加 `-Pmc=<version>`，26.1、26.2、26.3 在各自的
分支上用 Java 25。

## 🤖 关于翻译

这一页和模组的文字借助 AI 翻译，可能有错误；有疑问时以英文页面为准。

## 📄 许可

MIT，作者 **PiBOH**。
