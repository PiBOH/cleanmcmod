"""Adds the 0.3.1 language keys, drops the ones the new screens replaced, and scrubs long dashes.

Kept next to update_lang.py so the key set can be re-applied; running it twice is a no-op.
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
    "cleanmcmod.button.change": row(
        en_us="Change...", de_de="Ändern...", es_es="Cambiar...", fr_fr="Changer...",
        it_it="Cambia...", ja_jp="変更...", ko_kr="변경...", nl_nl="Wijzigen...",
        pl_pl="Zmień...", pt_br="Alterar...", ru_ru="Изменить...", tr_tr="Değiştir...",
        uk_ua="Змінити...", zh_cn="更改...", zh_tw="變更..."),
    "cleanmcmod.button.download_all": row(
        en_us="Download all", de_de="Alle herunterladen", es_es="Descargar todo",
        fr_fr="Tout télécharger", it_it="Scarica tutto", ja_jp="すべてダウンロード",
        ko_kr="모두 다운로드", nl_nl="Alles downloaden", pl_pl="Pobierz wszystko",
        pt_br="Baixar tudo", ru_ru="Скачать всё", tr_tr="Tümünü indir",
        uk_ua="Завантажити все", zh_cn="全部下载", zh_tw="全部下載"),
    "cleanmcmod.button.download_selected": row(
        en_us="Download selected (%s)", de_de="Auswahl herunterladen (%s)",
        es_es="Descargar seleccionados (%s)", fr_fr="Télécharger la sélection (%s)",
        it_it="Scarica selezionati (%s)", ja_jp="選択をダウンロード (%s)",
        ko_kr="선택 다운로드 (%s)", nl_nl="Selectie downloaden (%s)",
        pl_pl="Pobierz wybrane (%s)", pt_br="Baixar selecionados (%s)",
        ru_ru="Скачать выбранные (%s)", tr_tr="Seçilenleri indir (%s)",
        uk_ua="Завантажити вибрані (%s)", zh_cn="下载所选 (%s)", zh_tw="下載所選 (%s)"),
    "cleanmcmod.screen.updates.installed": row(
        en_us="Installed", de_de="Installiert", es_es="Instalada", fr_fr="Installée",
        it_it="Installata", ja_jp="導入済み", ko_kr="설치됨", nl_nl="Geïnstalleerd",
        pl_pl="Zainstalowana", pt_br="Instalada", ru_ru="Установлено", tr_tr="Kurulu",
        uk_ua="Встановлено", zh_cn="已安装", zh_tw="已安裝"),
    "cleanmcmod.screen.updates.new": row(
        en_us="New", de_de="Neu", es_es="Nueva", fr_fr="Nouvelle", it_it="Nuova",
        ja_jp="新規", ko_kr="새 버전", nl_nl="Nieuw", pl_pl="Nowa", pt_br="Nova",
        ru_ru="Новая", tr_tr="Yeni", uk_ua="Нова", zh_cn="新版本", zh_tw="新版本"),
    "cleanmcmod.screen.updates.result.title": row(
        en_us="Downloads finished", de_de="Downloads abgeschlossen",
        es_es="Descargas finalizadas", fr_fr="Téléchargements terminés",
        it_it="Download completati", ja_jp="ダウンロード完了", ko_kr="다운로드 완료",
        nl_nl="Downloads voltooid", pl_pl="Pobieranie zakończone",
        pt_br="Downloads concluídos", ru_ru="Загрузки завершены",
        tr_tr="İndirmeler bitti", uk_ua="Завантаження завершено",
        zh_cn="下载完成", zh_tw="下載完成"),
    "cleanmcmod.screen.updates.result.summary": row(
        en_us="%s of %s updated", de_de="%s von %s aktualisiert",
        es_es="%s de %s actualizadas", fr_fr="%s sur %s mises à jour",
        it_it="%s di %s aggiornate", ja_jp="%s / %s 件を更新",
        ko_kr="%s/%s개 업데이트됨", nl_nl="%s van %s bijgewerkt",
        pl_pl="Zaktualizowano %s z %s", pt_br="%s de %s atualizados",
        ru_ru="Обновлено %s из %s", tr_tr="%s / %s güncellendi",
        uk_ua="Оновлено %s з %s", zh_cn="已更新 %s / %s", zh_tw="已更新 %s / %s"),
    "cleanmcmod.settings.section.interface": row(
        en_us="Interface", de_de="Oberfläche", es_es="Interfaz", fr_fr="Interface",
        it_it="Interfaccia", ja_jp="インターフェース", ko_kr="인터페이스", nl_nl="Interface",
        pl_pl="Interfejs", pt_br="Interface", ru_ru="Интерфейс", tr_tr="Arayüz",
        uk_ua="Інтерфейс", zh_cn="界面", zh_tw="介面"),
    "cleanmcmod.settings.section.about": row(
        en_us="About", de_de="Über", es_es="Acerca de", fr_fr="À propos",
        it_it="Informazioni", ja_jp="このMODについて", ko_kr="정보", nl_nl="Over",
        pl_pl="O modzie", pt_br="Sobre", ru_ru="О моде", tr_tr="Hakkında",
        uk_ua="Про мод", zh_cn="关于", zh_tw="關於"),
    "cleanmcmod.settings.language": row(
        en_us="Language", de_de="Sprache", es_es="Idioma", fr_fr="Langue", it_it="Lingua",
        ja_jp="言語", ko_kr="언어", nl_nl="Taal", pl_pl="Język", pt_br="Idioma",
        ru_ru="Язык", tr_tr="Dil", uk_ua="Мова", zh_cn="语言", zh_tw="語言"),
    "cleanmcmod.settings.language.hint": row(
        en_us="Opens the game's language selector.",
        de_de="Öffnet die Sprachauswahl des Spiels.",
        es_es="Abre el selector de idioma del juego.",
        fr_fr="Ouvre le sélecteur de langue du jeu.",
        it_it="Apre il selettore della lingua del gioco.",
        ja_jp="ゲームの言語選択を開きます。",
        ko_kr="게임의 언어 선택을 엽니다.",
        nl_nl="Opent de taalkeuze van het spel.",
        pl_pl="Otwiera wybór języka gry.",
        pt_br="Abre o seletor de idioma do jogo.",
        ru_ru="Открывает выбор языка игры.",
        tr_tr="Oyunun dil seçimini açar.",
        uk_ua="Відкриває вибір мови гри.",
        zh_cn="打开游戏的语言选择。", zh_tw="開啟遊戲的語言選擇。"),
    "cleanmcmod.about.version": row(en_us="CleanMCMod %s"),
    "cleanmcmod.about.creator": row(
        en_us="Creator: PiBOH", de_de="Ersteller: PiBOH", es_es="Creador: PiBOH",
        fr_fr="Créateur : PiBOH", it_it="Creatore: PiBOH", ja_jp="制作者: PiBOH",
        ko_kr="제작자: PiBOH", nl_nl="Maker: PiBOH", pl_pl="Autor: PiBOH",
        pt_br="Criador: PiBOH", ru_ru="Автор: PiBOH", tr_tr="Geliştirici: PiBOH",
        uk_ua="Автор: PiBOH", zh_cn="作者: PiBOH", zh_tw="作者: PiBOH"),
    "cleanmcmod.about.inspired": row(
        en_us="Inspired by ModMenuUpdater, Sodium and Reese's Sodium Options",
        de_de="Inspiriert von ModMenuUpdater, Sodium und Reese's Sodium Options",
        es_es="Inspirado en ModMenuUpdater, Sodium y Reese's Sodium Options",
        fr_fr="Inspiré de ModMenuUpdater, Sodium et Reese's Sodium Options",
        it_it="Ispirato da ModMenuUpdater, Sodium e Reese's Sodium Options",
        ja_jp="ModMenuUpdater、Sodium、Reese's Sodium Options に着想",
        ko_kr="ModMenuUpdater, Sodium, Reese's Sodium Options에서 착안",
        nl_nl="Geïnspireerd door ModMenuUpdater, Sodium en Reese's Sodium Options",
        pl_pl="Zainspirowane przez ModMenuUpdater, Sodium i Reese's Sodium Options",
        pt_br="Inspirado em ModMenuUpdater, Sodium e Reese's Sodium Options",
        ru_ru="Вдохновлено ModMenuUpdater, Sodium и Reese's Sodium Options",
        tr_tr="ModMenuUpdater, Sodium ve Reese's Sodium Options esinlenmiştir",
        uk_ua="Натхненно ModMenuUpdater, Sodium і Reese's Sodium Options",
        zh_cn="灵感来自 ModMenuUpdater、Sodium 和 Reese's Sodium Options",
        zh_tw="靈感來自 ModMenuUpdater、Sodium 與 Reese's Sodium Options"),
    "cleanmcmod.about.links": row(
        en_us="modrinth.com/mod/cleanmcmod · github.com/PiBOH/cleanmcmod"),
}

OLD = ["cleanmcmod.button.install_all", "cleanmcmod.button.install_selected",
       "cleanmcmod.settings.tab.all"]

LONG_DASHES = [" \u2014 ", "\u2014 "]


def main():
    root = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        "src", "main", "resources", "assets", "cleanmcmod", "lang")
    for path in sorted(glob.glob(os.path.join(root, "*.json"))):
        lang = os.path.basename(path)[:-5]
        with io.open(path, encoding="utf-8") as handle:
            data = json.load(handle)
        for key, values in NEW.items():
            data[key] = values.get(lang, values["en_us"])
        for key in OLD:
            data.pop(key, None)
        for key, value in list(data.items()):
            for dash in LONG_DASHES:
                value = value.replace(dash, ": " if dash.startswith(" ") else ": ")
            data[key] = value.replace("\u2014", "-")
        with io.open(path, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(data, handle, ensure_ascii=False, indent=2, sort_keys=True)
            handle.write("\n")
    print("lang updated for 0.3.1")


if __name__ == "__main__":
    main()
