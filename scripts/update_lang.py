"""One-shot helper that adds the 0.3.0 language keys to every shipped locale.

Kept in the repository so the key set can be verified and re-applied; running it twice is a no-op.
"""

import glob
import io
import json
import os

LANGS = ["en_us", "de_de", "es_es", "fr_fr", "it_it", "ja_jp", "ko_kr", "nl_nl", "pl_pl", "pt_br",
         "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"]


def row(**values):
    return {lang: values.get(lang, values["en_us"]) for lang in LANGS}


NEW = {
    "cleanmcmod.settings.tab.all": row(
        en_us="All", de_de="Alle", es_es="Todo", fr_fr="Tout", it_it="Tutto", ja_jp="すべて",
        ko_kr="전체", nl_nl="Alles", pl_pl="Wszystko", pt_br="Tudo", ru_ru="Все", tr_tr="Tümü",
        uk_ua="Усе", zh_cn="全部", zh_tw="全部"),
    "cleanmcmod.settings.tab.advanced": row(
        en_us="Advanced", de_de="Erweitert", es_es="Avanzado", fr_fr="Avancé", it_it="Avanzate",
        ja_jp="詳細設定", ko_kr="고급", nl_nl="Geavanceerd", pl_pl="Zaawansowane", pt_br="Avançado",
        ru_ru="Дополнительно", tr_tr="Gelişmiş", uk_ua="Додатково", zh_cn="高级", zh_tw="進階"),
    "cleanmcmod.settings.search": row(
        en_us="Search options", de_de="Einstellungen suchen", es_es="Buscar opciones",
        fr_fr="Rechercher des options", it_it="Cerca opzioni", ja_jp="設定を検索",
        ko_kr="설정 검색", nl_nl="Opties zoeken", pl_pl="Szukaj opcji", pt_br="Buscar opções",
        ru_ru="Поиск настроек", tr_tr="Ayar ara", uk_ua="Пошук налаштувань",
        zh_cn="搜索选项", zh_tw="搜尋選項"),
    "cleanmcmod.settings.reset": row(
        en_us="Reset", de_de="Zurücksetzen", es_es="Restablecer", fr_fr="Réinitialiser",
        it_it="Reimposta", ja_jp="リセット", ko_kr="초기화", nl_nl="Herstellen", pl_pl="Resetuj",
        pt_br="Redefinir", ru_ru="Сбросить", tr_tr="Sıfırla", uk_ua="Скинути",
        zh_cn="重置", zh_tw="重設"),
    "cleanmcmod.settings.on": row(
        en_us="✔ ON", de_de="✔ AN", es_es="✔ SÍ", fr_fr="✔ OUI", it_it="✔ SÌ", ja_jp="✔ オン",
        ko_kr="✔ 켜짐", nl_nl="✔ AAN", pl_pl="✔ WŁ.", pt_br="✔ LIG.", ru_ru="✔ ВКЛ",
        tr_tr="✔ AÇIK", uk_ua="✔ УВІМК", zh_cn="✔ 开", zh_tw="✔ 開"),
    "cleanmcmod.settings.off": row(
        en_us="✘ OFF", de_de="✘ AUS", es_es="✘ NO", fr_fr="✘ NON", it_it="✘ NO", ja_jp="✘ オフ",
        ko_kr="✘ 꺼짐", nl_nl="✘ UIT", pl_pl="✘ WYŁ.", pt_br="✘ DESL.", ru_ru="✘ ВЫКЛ",
        tr_tr="✘ KAPALI", uk_ua="✘ ВИМК", zh_cn="✘ 关", zh_tw="✘ 關"),
    "cleanmcmod.settings.scroll": row(
        en_us="Scroll for more", de_de="Scrollen für mehr", es_es="Desplázate para ver más",
        fr_fr="Faites défiler pour en voir plus", it_it="Scorri per altro",
        ja_jp="スクロールで続きを表示", ko_kr="스크롤하여 더 보기", nl_nl="Scroll voor meer",
        pl_pl="Przewiń, aby zobaczyć więcej", pt_br="Role para ver mais",
        ru_ru="Прокрутите, чтобы увидеть больше", tr_tr="Daha fazlası için kaydır",
        uk_ua="Прокрутіть, щоб побачити більше", zh_cn="滚动查看更多", zh_tw="捲動以查看更多"),
    "cleanmcmod.settings.explain": row(en_us="%s — %s"),
    "cleanmcmod.category.map": row(
        en_us="Minimap mods", de_de="Minimap-Mods", es_es="Mods de minimapa",
        fr_fr="Mods de minimap", it_it="Mod minimappa", ja_jp="ミニマップMOD",
        ko_kr="미니맵 모드", nl_nl="Minimap-mods", pl_pl="Mody minimapy",
        pt_br="Mods de minimapa", ru_ru="Моды миникарты", tr_tr="Minimap modları",
        uk_ua="Моди мінікарти", zh_cn="小地图模组", zh_tw="小地圖模組"),
    "cleanmcmod.setting.map_caches": row(
        en_us="VoxelMap / Xaero caches", de_de="VoxelMap-/Xaero-Caches",
        es_es="Cachés de VoxelMap / Xaero", fr_fr="Caches VoxelMap / Xaero",
        it_it="Cache di VoxelMap / Xaero", ja_jp="VoxelMap / Xaero キャッシュ",
        ko_kr="VoxelMap / Xaero 캐시", nl_nl="VoxelMap-/Xaero-caches",
        pl_pl="Cache VoxelMap / Xaero", pt_br="Caches do VoxelMap / Xaero",
        ru_ru="Кэши VoxelMap / Xaero", tr_tr="VoxelMap / Xaero önbellekleri",
        uk_ua="Кеші VoxelMap / Xaero", zh_cn="VoxelMap / Xaero 缓存",
        zh_tw="VoxelMap / Xaero 快取"),
    "cleanmcmod.source.modrinth": row(en_us="Modrinth"),
    "cleanmcmod.source.curseforge": row(en_us="CurseForge"),
    "cleanmcmod.source.github": row(en_us="GitHub"),
    "cleanmcmod.screen.updates.delegated": row(
        en_us="Mod Menu Updater is installed — updates are installed from Mod Menu",
        de_de="Mod Menu Updater ist installiert – Updates werden über Mod Menu installiert",
        es_es="Mod Menu Updater está instalado: las actualizaciones se instalan desde Mod Menu",
        fr_fr="Mod Menu Updater est installé : les mises à jour s'installent depuis Mod Menu",
        it_it="Mod Menu Updater è installato: gli aggiornamenti si installano da Mod Menu",
        ja_jp="Mod Menu Updater が導入済みのため、更新は Mod Menu から行います",
        ko_kr="Mod Menu Updater가 설치되어 있어 업데이트는 Mod Menu에서 설치합니다",
        nl_nl="Mod Menu Updater is geïnstalleerd — updates worden via Mod Menu geïnstalleerd",
        pl_pl="Mod Menu Updater jest zainstalowany — aktualizacje instaluje się z Mod Menu",
        pt_br="O Mod Menu Updater está instalado: as atualizações são instaladas pelo Mod Menu",
        ru_ru="Mod Menu Updater установлен — обновления ставятся из Mod Menu",
        tr_tr="Mod Menu Updater kurulu — güncellemeler Mod Menu üzerinden kurulur",
        uk_ua="Mod Menu Updater встановлено — оновлення ставляться з Mod Menu",
        zh_cn="已安装 Mod Menu Updater：更新请在 Mod Menu 中安装",
        zh_tw="已安裝 Mod Menu Updater：更新請在 Mod Menu 中安裝"),
    "key.cleanmcmod.updates": row(
        en_us="Mod updates", de_de="Mod-Updates", es_es="Actualizaciones de mods",
        fr_fr="Mises à jour des mods", it_it="Aggiornamenti mod", ja_jp="MOD更新",
        ko_kr="모드 업데이트", nl_nl="Mod-updates", pl_pl="Aktualizacje modów",
        pt_br="Atualizações de mods", ru_ru="Обновления модов", tr_tr="Mod güncellemeleri",
        uk_ua="Оновлення модів", zh_cn="模组更新", zh_tw="模組更新"),
    "cleanmcmod.screen.main.hint": row(
        en_us="F7 opens this menu · F8 mod updates · /cleanmcmod clean",
        de_de="F7 öffnet dieses Menü · F8 Mod-Updates · /cleanmcmod clean",
        es_es="F7 abre este menú · F8 actualizaciones · /cleanmcmod clean",
        fr_fr="F7 ouvre ce menu · F8 mises à jour · /cleanmcmod clean",
        it_it="F7 apre questo menu · F8 aggiornamenti · /cleanmcmod clean",
        ja_jp="F7 でこのメニュー · F8 でMOD更新 · /cleanmcmod clean",
        ko_kr="F7 메뉴 열기 · F8 모드 업데이트 · /cleanmcmod clean",
        nl_nl="F7 opent dit menu · F8 mod-updates · /cleanmcmod clean",
        pl_pl="F7 otwiera to menu · F8 aktualizacje modów · /cleanmcmod clean",
        pt_br="F7 abre este menu · F8 atualizações · /cleanmcmod clean",
        ru_ru="F7 открывает это меню · F8 обновления модов · /cleanmcmod clean",
        tr_tr="F7 bu menüyü açar · F8 mod güncellemeleri · /cleanmcmod clean",
        uk_ua="F7 відкриває це меню · F8 оновлення модів · /cleanmcmod clean",
        zh_cn="F7 打开此菜单 · F8 模组更新 · /cleanmcmod clean",
        zh_tw="F7 開啟此選單 · F8 模組更新 · /cleanmcmod clean"),
    "cleanmcmod.screen.updates.checking": row(
        en_us="Checking Modrinth, CurseForge and GitHub...",
        de_de="Modrinth, CurseForge und GitHub werden geprüft...",
        es_es="Consultando Modrinth, CurseForge y GitHub...",
        fr_fr="Vérification de Modrinth, CurseForge et GitHub...",
        it_it="Controllo Modrinth, CurseForge e GitHub...",
        ja_jp="Modrinth / CurseForge / GitHub を確認中...",
        ko_kr="Modrinth, CurseForge, GitHub 확인 중...",
        nl_nl="Modrinth, CurseForge en GitHub worden gecontroleerd...",
        pl_pl="Sprawdzanie Modrinth, CurseForge i GitHub...",
        pt_br="Consultando Modrinth, CurseForge e GitHub...",
        ru_ru="Проверка Modrinth, CurseForge и GitHub...",
        tr_tr="Modrinth, CurseForge ve GitHub denetleniyor...",
        uk_ua="Перевірка Modrinth, CurseForge і GitHub...",
        zh_cn="正在检查 Modrinth、CurseForge 和 GitHub...",
        zh_tw="正在檢查 Modrinth、CurseForge 與 GitHub..."),
    "cleanmcmod.screen.updates.failed": row(
        en_us="Update check failed: %s",
        de_de="Update-Prüfung fehlgeschlagen: %s",
        es_es="La comprobación de actualizaciones falló: %s",
        fr_fr="La recherche de mises à jour a échoué : %s",
        it_it="Controllo aggiornamenti non riuscito: %s",
        ja_jp="更新の確認に失敗しました: %s",
        ko_kr="업데이트 확인 실패: %s",
        nl_nl="Update-controle mislukt: %s",
        pl_pl="Sprawdzanie aktualizacji nie powiodło się: %s",
        pt_br="A verificação de atualizações falhou: %s",
        ru_ru="Не удалось проверить обновления: %s",
        tr_tr="Güncelleme denetimi başarısız: %s",
        uk_ua="Не вдалося перевірити оновлення: %s",
        zh_cn="检查更新失败：%s",
        zh_tw="檢查更新失敗：%s"),
    "cleanmcmod.screen.updates.hint": row(
        en_us="Click a row to select · %s selected",
        de_de="Zeile anklicken zum Auswählen · %s gewählt",
        es_es="Clic en una fila para elegir · %s elegidos",
        fr_fr="Cliquez une ligne pour choisir · %s sélectionnés",
        it_it="Clicca una riga per selezionare · %s selezionati",
        ja_jp="行をクリックで選択 · %s 件選択",
        ko_kr="행을 클릭해 선택 · %s개 선택",
        nl_nl="Klik een rij om te kiezen · %s geselecteerd",
        pl_pl="Kliknij wiersz, aby wybrać · %s wybranych",
        pt_br="Clique numa linha para escolher · %s selecionados",
        ru_ru="Клик по строке, чтобы выбрать · выбрано %s",
        tr_tr="Seçmek için satıra tıkla · %s seçili",
        uk_ua="Клік по рядку, щоб вибрати · вибрано %s",
        zh_cn="点击一行选择 · 已选 %s",
        zh_tw="點擊一列選擇 · 已選 %s"),
}


def main():
    root = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        "src", "main", "resources", "assets", "cleanmcmod", "lang")
    for path in sorted(glob.glob(os.path.join(root, "*.json"))):
        lang = os.path.basename(path)[:-5]
        with io.open(path, encoding="utf-8") as handle:
            data = json.load(handle)
        for key, values in NEW.items():
            data[key] = values.get(lang, values["en_us"])
        with io.open(path, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(data, handle, ensure_ascii=False, indent=2, sort_keys=True)
            handle.write("\n")
    print("lang updated")


if __name__ == "__main__":
    main()
