# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · **🇹🇷 Türkçe** · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Minecraft icin istemci tarafi onbellek temizleyici: kurdugun modlar ve onlarin
kullandigi kaynak paketleri icin. Eski onbellekler oyunun yanlis cizmesinin
sebebidir: kayip modeller siyah ve mor gorunur, dokular hic yenilenmez, kaynak
paketleri bozulur. CleanMCMod bunlari tek ekrandan siler, silmeden once neyin
gidecegini gosterir ve istersen modlari Modrinth uzerinden gunceller.

## 🧹 Neleri siler

- `logs/`, `crash-reports/` ve oyunun hata ayiklama dokumleri
- sunucunun gonderdigi kaynak paketlerinin onbellegi
- Fabric'in yeniden yazdigi jar dosyalari ve Mixin hata ayiklama ciktisi
- derin kaynak onbellegi, varsayilan olarak kapali
- `config/<mod>/` altindaki onbellek klasorleri, VoxelMap ve Xaero haritalari
  dahil, klasor bazinda kapali

Her hedef silinmeden once olculur. Klasorler asla kendileri silinmez, sadece
dosyalari; sembolik baglantilar takip edilmez; `saves/`, `mods/*.jar`,
`resourcepacks/`, `shaderpacks/`, `screenshots/` ve ust duzeydeki
`config/*.json` dosyalarina dokunulmaz.

## 🎮 Oyunda

- **F7** ekrani acar, baslik ekraninda da bir dugme var
- `/cleanmcmod scan`, `/cleanmcmod clean` ve `/cleanmcmod screen` sohbetten
- **deneme** satiri acikken hicbir sey silinmez

## ⚙️ Ayarlar

Sekiz anahtar, degistirdigin an `config/cleanmcmod.json` dosyasina yazilir.

## ⬆️ Modlari guncelleme

Modrinth'ten jar dosyalarinin daha yeni derlemesini ister. Once her sey dogrulanir,
eski jar yeni tamamlanana kadar yerinde kalir. Kendi jar dosyasini asla degistirmez.

## 🧩 Desteklenen surumler

1.21.8 ile 1.21.11 arasi tek kodla `-Pmc=<version>`, 26.1, 26.2 ve 26.3 ise Java
25 ile kendi dallarinda.

## 🤖 Ceviri hakkinda

Bu sayfa ve modun metinleri yapay zeka yardimiyla cevrildi. Hata olabilir; suphede
Ingilizce sayfa gecerlidir.

## 📄 Lisans

MIT, **PiBOH**.
