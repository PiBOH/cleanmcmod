# ♻️ CleanMCMod

[🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · [🇰🇷 한국어](../ko_kr/README.md) · [🇳🇱 Nederlands](../nl_nl/README.md) · **🇵🇱 Polski** · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Narzedzie do czyszczenia cache po stronie klienta dla Minecrafta, dla modow, ktore
masz zainstalowane, i dla paczek zasobow, ktorych uzywaja. Stare cache to powod,
dlaczego gra rysuje zle: brakujace modele w czerni i fiolecie, tekstury, ktore sie
nie odswiezaja, zepsute paczki. CleanMCMod usuwa je z jednego ekranu, najpierw
pokazuje, co zniknie, i na zadanie aktualizuje mody z Modrinth.

## 🧹 Co czysci

- `logs/`, `crash-reports/` i zrzuty debugowania gry
- cache paczek zasobow wysylanych przez serwer
- przemapowane jary Fabric i wyjscie debugowania Mixin
- gleboki cache zasobow, domyslnie wylaczony
- foldery cache w `config/<mod>/`, takze mapy VoxelMap i Xaero, wylaczone per folder

Kazdy cel jest mierzony przed usunieciem. Foldery nigdy nie sa usuwane, tylko ich
pliki; dowiazania symboliczne nie sa sledzone; `saves/`, `mods/*.jar`,
`resourcepacks/`, `shaderpacks/`, `screenshots/` i najwyzsze `config/*.json`
zostaja nietkniete.

## 🎮 W grze

- **F7** otwiera ekran, a ekran tytulowy ma do niego przycisk
- `/cleanmcmod scan`, `/cleanmcmod clean` i `/cleanmcmod screen` z czatu
- wiersz **Proby** mowi, ze nic nie zostanie usuniete, dopoki go nie wylaczysz

## ⚙️ Ustawienia

Osiem przelacznikow, zapisywanych od razu w `config/cleanmcmod.json`.

## ⬆️ Aktualizacja modow

Pyta Modrinth o nowsza wersje twoich jarow. Wszystko jest najpierw sprawdzane,
stary jar zostaje, dopoki nowy nie jest kompletny. Wlasny jar nigdy nie jest
podmieniany.

## 🧩 Wersje

1.21.8 do 1.21.11 z jednego kodu z `-Pmc=<version>`, oraz 26.1, 26.2 i 26.3 z
wlasnych galezi na Java 25.

## 🤖 O tlumaczeniu

Ta strona i teksty moda zostaly przetlumaczone z pomoca AI. Moga zawierac bledy;
w razie watpliwosci obowiazuje strona angielska.

## 📄 Licencja

MIT, autor **PiBOH**.
