# ♻️ CleanMCMod

🌍 [🇬🇧 English](../../README.md) · [🇩🇪 Deutsch](../de_de/README.md) · [🇪🇸 Español](../es_es/README.md) · [🇫🇷 Français](../fr_fr/README.md) · [🇮🇹 Italiano](../it_it/LEGGIMI.md) · [🇯🇵 日本語](../ja_jp/README.md) · **🇰🇷 한국어** · [🇳🇱 Nederlands](../nl_nl/README.md) · [🇵🇱 Polski](../pl_pl/README.md) · [🇧🇷 Português (Brasil)](../pt_br/README.md) · [🇷🇺 Русский](../ru_ru/README.md) · [🇹🇷 Türkçe](../tr_tr/README.md) · [🇺🇦 Українська](../uk_ua/README.md) · [🇨🇳 简体中文](../zh_cn/README.md) · [🇹🇼 繁體中文](../zh_tw/README.md)

<p align="center">
  <img src="../../logo.png" alt="CleanMCMod" width="180">
</p>

Minecraft 클라이언트용 캐시 정리 도구입니다. 설치한 모드와 그 모드가 쓰는 리소스
팩의 캐시를 지웁니다. 오래된 캐시는 게임이 잘못 그려지는 원인입니다. 모델이 검정과
보라색으로 보이거나, 텍스처가 새로 고쳐지지 않거나, 리소스 팩이 깨지는 증상입니다.
CleanMCMod는 한 화면에서 그것들을 지우고, 지우기 전에 무엇이 사라지는지 보여 주며,
원하면 Modrinth에서 모드를 업데이트합니다.

## 🧹 지우는 것

- `logs/`, `crash-reports/`, 게임의 디버그 덤프
- 서버가 보내는 리소스 팩 캐시
- Fabric이 다시 만드는 jar와 Mixin 디버그 출력
- 깊은 에셋 캐시, 기본값은 꺼짐
- `config/<mod>/` 아래의 캐시 폴더, VoxelMap과 Xaero 지도 포함, 폴더마다 꺼짐

모든 대상은 지우기 전에 크기를 잽니다. 폴더 자체는 지우지 않고 안의 파일만 지웁니다.
심볼릭 링크는 따라가지 않습니다. `saves/`, `mods/*.jar`, `resourcepacks/`,
`shaderpacks/`, `screenshots/`와 최상위 `config/*.json`은 건드리지 않습니다.

## 🎮 게임에서

- **F7** 로 화면을 엽니다. 타이틀 화면에도 버튼이 있습니다
- `/cleanmcmod scan`, `/cleanmcmod clean`, `/cleanmcmod screen`은 채팅에서
- **모의 실행**이 켜져 있으면 아무것도 지워지지 않습니다

## ⚙️ 설정

스위치는 여덟 개이며, 바꾸는 즉시 `config/cleanmcmod.json`에 저장됩니다.

## ⬆️ 모드 업데이트

Modrinth에 더 새로운 빌드를 묻습니다. 확인이 끝날 때까지 예전 jar는 그대로 있고,
새것이 완전히 준비된 뒤에 바꿉니다. 자기 자신의 jar는 절대 바꾸지 않습니다.

## 🧩 지원 버전

1.21.8부터 1.21.11까지는 한 코드에서 `-Pmc=<version>`으로, 26.1, 26.2, 26.3은
Java 25의 별도 브랜치에서.

## 🤖 번역 안내

이 페이지와 모드의 문장은 AI의 도움을 받아 번역했습니다. 오류가 있을 수 있으며,
확실하지 않으면 영어 페이지를 기준으로 하세요.

## 📄 라이선스

MIT, 만든 사람 **PiBOH**.
