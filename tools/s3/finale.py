#!/usr/bin/env python3
"""
第 30 章〈終章〉（第 481–500 關）的題目產生器。

把第 21–29 章的九種謎題各出兩題最難的（第 500 關是「誰在說謊」的最終版）。
直接借用各章的產生器：暫時把它們的 SPECS 換成這一章的規格、輸出位置換成暫存資料夾，
再把產生的 Kotlin 與字串合併成：
  app/src/main/kotlin/.../levels/s3/Ch30Puzzles.kt
  app/src/main/res/values/strings_ch30_puzzles.xml
  tools/s3/ch30_puzzles.json（每關的種類與答案，給提示用）
各章的檢查（答案唯一、謊話有陷阱……）全部照舊。
"""
import importlib
import json
import os
import re
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch30Puzzles.kt")
OUT_XML = os.path.join(ROOT, "app/src/main/res/values/strings_ch30_puzzles.xml")

import mines  # noqa: E402
import captcha  # noqa: E402
import liars  # noqa: E402
import rules  # noqa: E402
import wires  # noqa: E402
import order  # noqa: E402
import maze  # noqa: E402
import pins  # noqa: E402
import mirrors  # noqa: E402

PLAN = {
    # 第一輪
    481: ("mines", dict(rows=8, cols=8, mines=13, max_open=18, reported=5, lies=2, counter=True, hard=6, min_steps=8, seed=481)),
    482: ("captcha", [captcha.R(4, 5, ["rows", "cols"], captcha.ARITH, spoken=True, lies=2, colors=4, shapes=4, subtle=True)]),
    483: ("liars", liars.S(6, "liars", liars.LOGIC + ["neighbors"], narrator=True, narrator_lies=True, min_half=6, max_direct=1)),
    484: ("rules", rules.Z(examples=(8, 13), spoken=5, lies=2, min_weight=3, max_weight=3)),
    485: ("rules", rules.M(colors=6, length=4, rows=(6, 9), dup=True, lies=2)),
    486: ("wires", wires.W(5, 5, (5, 8), xor=True, hidden=1, min_on=2)),
    487: ("order", order.O(5, (6, 11), order.T, two=True, hidden=1, min_half=100, max_direct=1, attempts=80)),
    488: ("maze", maze.M(7, "mix", (15, 18), spoken=6, lies=2)),
    489: ("pins", pins.P(5, (7, 12), pins.C, hidden=1, min_half=600, attempts=300)),
    490: ("mirrors", mirrors.M(7, 14, 9, spoken=6, lies=2, min_turns=14)),
    # 第二輪
    491: ("mines", dict(rows=8, cols=8, mines=14, mode="flag", reported=5, lies=2, hard=3, min_steps=4, seed=491)),
    492: ("captcha", [captcha.R(4, 5, (), captcha.GRID, exactly_true=2, colors=4, shapes=4),
                     captcha.R(4, 5, ["adjacent"], captcha.GRID, spoken=True, lies=2, colors=4, shapes=4, subtle=True)]),
    493: ("liars", liars.S(5, "spy", liars.LOGIC + liars.SPY, spies=1, per=2, min_half=4, max_direct=2)),
    494: ("rules", rules.Z(examples=(7, 14), spoken=5, lies=2, min_weight=2, max_weight=3)),
    495: ("rules", rules.M(colors=6, length=4, rows=(6, 9), hidden_lies=1)),
    496: ("wires", wires.W(5, 5, (5, 8), hidden=1, min_on=2)),
    497: ("order", order.O(6, (7, 11), order.S2, lies=2, spoken=8, min_half=60, max_direct=0)),
    498: ("maze", maze.M(7, "arrow", (11, 14), spoken=6, lies=2)),
    499: ("pins", pins.P(5, (8, 12), pins.C, lies=2, spoken=8, min_half=600, attempts=300)),
    # 最後一關
    500: ("liars", liars.S(6, "button", liars.LOGIC + liars.BTN + liars.SPY, button=True, spies=1, narrator=True, narrator_lies=True, min_half=6, max_direct=1)),
}

MODULES = {"mines": mines, "captcha": captcha, "liars": liars, "rules": rules, "wires": wires, "order": order, "maze": maze, "pins": pins, "mirrors": mirrors}
JSON_NAME = {"mines": "ch21_boards.json", "captcha": "ch22_puzzles.json", "liars": "ch23_puzzles.json", "rules": "ch24_puzzles.json",
             "wires": "ch25_puzzles.json", "order": "ch26_puzzles.json", "maze": "ch27_puzzles.json", "pins": "ch28_puzzles.json", "mirrors": "ch29_puzzles.json"}


def run(kind, lvl, spec, tmp):
    mod = MODULES[kind]
    saved = {k: getattr(mod, k) for k in ("SPECS", "OUT_KT", "OUT_XML", "OUT", "__file__") if hasattr(mod, k)}
    kt_path = os.path.join(tmp, f"{lvl}.kt")
    xml_path = os.path.join(tmp, f"{lvl}.xml")
    try:
        mod.SPECS = {lvl: spec}
        if hasattr(mod, "OUT_KT"):
            mod.OUT_KT = kt_path
        if hasattr(mod, "OUT"):
            mod.OUT = kt_path
        if hasattr(mod, "OUT_XML"):
            mod.OUT_XML = xml_path
        mod.__file__ = os.path.join(tmp, f"{kind}.py")  # 讓 json 寫到暫存資料夾
        mod.main(None) if mod.main.__code__.co_argcount else mod.main()
    finally:
        for k, v in saved.items():
            setattr(mod, k, v)
    kt = open(kt_path).read()
    body = kt[kt.index("{", kt.index("internal object")) + 2: kt.rstrip().rindex("}")]
    xml = ""
    if os.path.exists(xml_path):
        x = open(xml_path).read()
        xml = "".join(line + "\n" for line in x.splitlines() if line.strip().startswith("<string"))
    info = json.load(open(os.path.join(tmp, JSON_NAME[kind])))[str(lvl)]
    return body, xml, info


def main():
    tmp = tempfile.mkdtemp()
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "import tw.dynity.wrongbutton.R\n\n",
        "// 由 tools/s3/finale.py 產生（借用第 21–29 章的產生器，檢查都照舊）。請改產生器再重新產生。\n",
        "internal object Ch30Puzzles {\n",
    ]
    xml = ['<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/s3/finale.py 產生。台灣繁體中文。 -->\n<resources>\n']
    info = {}
    for lvl, (kind, spec) in PLAN.items():
        body, x, inf = run(kind, lvl, spec, tmp)
        kt.append(f"    // ---- 第 {lvl} 關：{kind} ----\n")
        kt.append(body)
        xml.append(x)
        info[lvl] = dict(inf, engine=kind) if isinstance(inf, dict) else dict(engine=kind, rounds=inf)
        print(lvl, kind, file=sys.stderr)
    kt.append("}\n")
    xml.append("</resources>\n")
    with open(OUT_KT, "w") as f:
        f.write("".join(kt))
    with open(OUT_XML, "w") as f:
        f.write("".join(xml))
    with open(os.path.join(HERE, "ch30_puzzles.json"), "w") as f:
        json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()
