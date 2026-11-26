#!/usr/bin/env python3
"""
第二季（第 9–20 章）的資源產生器。

每一章的規格寫在 tools/s2/chNN.py（標題、任務、三階提示、台詞、關卡專用字串、設計說明）。
執行後產生：
  app/src/main/res/values/strings_chNN.xml   章名、關卡字串、提示
  app/src/main/res/values/dialogue_chNN.xml  每關的進場／第一次錯／通關台詞，加上整章共用的台詞池
  app/src/main/kotlin/.../levels/s2/ChNNMeta.kt  每關的 S2Meta（R 參照）
  docs/levels/chNN.md                        逐關設計

用法：python3 tools/s2/gen.py            （全部章節）
      python3 tools/s2/gen.py 9 10       （只產生指定章節）

台詞規則（和第一季相同）：前綴 [mood|tell] 決定表情；內容是假話的台詞一定要標 lie 或 subtle，
沒有標記的台詞必須是真話。
"""
import importlib.util
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app/src/main/res/values")
KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s2")
DOCS = os.path.join(ROOT, "docs/levels")

POOL_EVENTS = ["mistakes_3", "mistakes_6", "idle", "rapid_tap", "hint", "special"]
LEVEL_EVENTS = [("enter", "enter"), ("first", "first_mistake"), ("clear", "clear")]
SHARED_TASKS = {"press": "task_press", "correct": "task_correct"}


def esc(text: str) -> str:
    """Android 字串資源的跳脫。"""
    t = text.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    t = t.replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if t.startswith("@") or t.startswith("?"):
        t = "\\" + t
    return t


S3 = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "s3")


def season3(ch: int) -> bool:
    """第三季（第 21 章起）：每章 20 關，Kotlin 放在 levels/s3，規格放在 tools/s3。"""
    return ch >= 21


def load(ch: int):
    folder = S3 if season3(ch) else os.path.dirname(__file__)
    path = os.path.join(folder, f"ch{ch:02d}.py")
    spec = importlib.util.spec_from_file_location(f"ch{ch:02d}", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def check(mod):
    ids = [lv["id"] for lv in mod.LEVELS]
    n = 20 if season3(mod.CH) else 15
    assert len(ids) == n, f"第 {mod.CH} 章應有 {n} 關，現在有 {len(ids)}"
    assert len(set(ids)) == n, "關卡 id 重複"
    for ev in POOL_EVENTS:
        lines = mod.POOL[ev]
        assert len(lines) >= 2 and len(set(lines)) == len(lines), f"第 {mod.CH} 章台詞池 {ev} 至少 2 句且不重複"
    for lv in mod.LEVELS:
        assert len(lv["hints"]) == 3, f"{lv['id']} 需要三階提示"
        for key, _ in LEVEL_EVENTS:
            lines = lv[key]
            assert len(lines) >= 2 and len(set(lines)) == len(lines), f"{lv['id']} / {key} 至少 2 句且不重複"
        for k in lv.get("strings", {}):
            assert re.fullmatch(r"[a-z0-9_]+", k), f"{lv['id']} 字串鍵 {k} 只能用小寫英數與底線"
            assert k not in ("title", "task") and not k.startswith("hint_"), f"{lv['id']} 字串鍵 {k} 和自動產生的鍵衝突"


def gen(ch: int):
    mod = load(ch)
    check(mod)
    nn = f"{ch:02d}"
    header = '<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/s2/gen.py 依 tools/s2/ch%s.py 產生，請改規格檔再重新產生。台灣繁體中文。 -->\n<resources>\n' % nn

    # ---- 字串 ----
    out = [header, f'    <string name="chapter_{ch}">{esc(mod.TITLE)}</string>\n']
    for lv in mod.LEVELS:
        i = lv["id"]
        out.append(f'    <string name="l{i}_title">{esc(lv["title"])}</string>\n')
        if lv["task"] not in SHARED_TASKS:
            out.append(f'    <string name="l{i}_task">{esc(lv["task"])}</string>\n')
        for n, h in enumerate(lv["hints"], 1):
            out.append(f'    <string name="l{i}_hint_{n}">{esc(h)}</string>\n')
        for k, v in lv.get("strings", {}).items():
            if isinstance(v, list):
                out.append(f'    <string-array name="l{i}_{k}">\n')
                out.extend(f"        <item>{esc(x)}</item>\n" for x in v)
                out.append("    </string-array>\n")
            else:
                fmt = "" if re.search(r"%\d\$", v) else ' formatted="false"' if "%" in v else ""
                out.append(f'    <string name="l{i}_{k}"{fmt}>{esc(v)}</string>\n')
    for k, v in getattr(mod, "STRINGS", {}).items():
        if isinstance(v, list):
            out.append(f'    <string-array name="ch{ch}_{k}">\n')
            out.extend(f"        <item>{esc(x)}</item>\n" for x in v)
            out.append("    </string-array>\n")
        else:
            out.append(f'    <string name="ch{ch}_{k}">{esc(v)}</string>\n')
    out.append("</resources>\n")
    with open(os.path.join(RES, f"strings_ch{nn}.xml"), "w") as f:
        f.write("".join(out))

    # ---- 台詞 ----
    out = [header]

    def arr(name, lines):
        out.append(f'    <string-array name="{name}">\n')
        out.extend(f"        <item>{esc(x)}</item>\n" for x in lines)
        out.append("    </string-array>\n")

    for ev in POOL_EVENTS:
        arr(f"ch{ch}_{ev}", mod.POOL[ev])
    for lv in mod.LEVELS:
        for key, ev in LEVEL_EVENTS:
            arr(f"l{lv['id']}_{ev}", lv[key])
    out.append("</resources>\n")
    with open(os.path.join(RES, f"dialogue_ch{nn}.xml"), "w") as f:
        f.write("".join(out))

    # ---- Kotlin：S2Meta ----
    pkg = "s3" if season3(ch) else "s2"
    kt = [
        f"package tw.dynity.wrongbutton.levels.{pkg}\n\n",
        "import tw.dynity.wrongbutton.R\n",
        *(["import tw.dynity.wrongbutton.levels.s2.S2Meta\n"] if season3(ch) else []),
        "import tw.dynity.wrongbutton.levels.dialogue\n\n",
        f"// 由 tools/s2/gen.py 依 tools/s2/ch{nn}.py 產生，請改規格檔再重新產生。\n",
        f"internal object Ch{nn}Meta {{\n",
    ]
    for lv in mod.LEVELS:
        i = lv["id"]
        task = f"R.string.{SHARED_TASKS[lv['task']]}" if lv["task"] in SHARED_TASKS else f"R.string.l{i}_task"
        d = ", ".join(
            [f"R.array.l{i}_enter", f"R.array.l{i}_first_mistake"]
            + [f"R.array.ch{ch}_mistakes_3", f"R.array.ch{ch}_mistakes_6", f"R.array.ch{ch}_idle", f"R.array.ch{ch}_rapid_tap", f"R.array.ch{ch}_hint"]
            + [f"R.array.l{i}_clear", f"R.array.ch{ch}_special"]
        )
        kt.append(
            f"    val l{i} = S2Meta({i}, R.string.l{i}_title, {task}, listOf(R.string.l{i}_hint_1, R.string.l{i}_hint_2, R.string.l{i}_hint_3), dialogue({d}))\n"
        )
    kt.append("}\n")
    with open(os.path.join(KT.replace("/s2", "/s3") if season3(ch) else KT, f"Ch{nn}Meta.kt"), "w") as f:
        f.write("".join(kt))

    # ---- 設計文件 ----
    md = [f"# {mod.TITLE.replace('　', '　')}（{mod.LEVELS[0]['id']}–{mod.LEVELS[-1]['id']}）\n\n"]
    md.append(f"> 由 `tools/s2/gen.py` 依 `tools/{'s3' if season3(ch) else 's2'}/ch{nn}.py` 產生。\n\n")
    md.append(mod.INTRO.strip() + "\n\n---\n\n")
    for lv in mod.LEVELS:
        doc = lv["doc"]
        task = {"press": "按下按鈕。", "correct": "按下正確的按鈕。"}.get(lv["task"], lv["task"])
        md.append(f"### {lv['id']}〈{lv['title']}〉\n")
        md.append(f"- **任務**：{task}\n")
        md.append(f"- **畫面**：{doc['screen']}\n")
        md.append(f"- **解法**：{doc['solution']}\n")
        md.append(f"- **線索**：{doc['clue']}\n")
        md.append("- **提示**：" + "".join(f"{'①②③'[n]}「{h}」" for n, h in enumerate(lv["hints"])) + "\n")
        if doc.get("note"):
            md.append(f"- **備註**：{doc['note']}\n")
        md.append("\n")
    with open(os.path.join(DOCS, f"ch{nn}.md"), "w") as f:
        f.write("".join(md))
    print(f"第 {ch} 章：{len(mod.LEVELS)} 關")


if __name__ == "__main__":
    chapters = [int(a) for a in sys.argv[1:]] or sorted(
        int(m.group(1))
        for d in (os.path.dirname(os.path.abspath(__file__)), S3)
        for m in (re.match(r"ch(\d+)\.py$", f) for f in os.listdir(d))
        if m
    )
    for c in chapters:
        gen(c)
