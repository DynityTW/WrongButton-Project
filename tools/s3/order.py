#!/usr/bin/env python3
"""
第 26 章〈排順序〉的題目產生器與檢查器。

幾顆按鈕排成一排（由左到右第 1、2、3……個）。根據線索排出唯一的順序。
後半章每一顆還買了一種飲料（第二個屬性），兩樣都要排對。

暴力列舉所有排法，檢查：
- 答案唯一，每一條線索都用得到。
- 只看一半的線索，可能的排法還很多（不會一兩條就看穿）。
- 有些線索要聽 The Button 說（說謊時線索是假的），把謊話當真會排錯；有些關卡看不到臉，只知道剛好幾條是假的。

輸出 app/src/main/kotlin/.../levels/s3/Ch26Puzzles.kt、app/src/main/res/values/strings_ch26_puzzles.xml、tools/s3/ch26_puzzles.json。
"""
import itertools
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch26Puzzles.kt")
OUT_XML = os.path.join(ROOT, "app/src/main/res/values/strings_ch26_puzzles.xml")

COLORS = ["紅鈕", "藍鈕", "綠鈕", "黃鈕", "紫鈕", "橘鈕"]
DRINKS = ["咖啡", "紅茶", "果汁", "牛奶", "汽水"]


def worlds(n, two):
    perms = list(itertools.permutations(range(n)))
    if not two:
        for p in perms:
            yield (p, None)
    else:
        for p in perms:
            for q in perms:
                yield (p, q)


def pos_of(w, attr, v):
    """attr 0：顏色（按鈕）；attr 1：飲料。回傳位置（0 開始）。"""
    return w[attr].index(v)


def clue(rnd, n, two, kinds):
    kind = rnd.choice(kinds)
    attr = rnd.choice([0, 1]) if two else 0
    names = COLORS if attr == 0 else DRINKS

    def nm(a, v):
        return (COLORS if a == 0 else DRINKS)[v] if a == 0 else f"買{DRINKS[v]}的"
    x, y, z = rnd.sample(range(n), 3) if n >= 3 else (0, 1, 0)
    X, Y, Z = nm(attr, x), nm(attr, y), nm(attr, z)

    def of(s):
        """「紅鈕的左邊」「買果汁的左邊」（不要變成「的的」）。"""
        return s if s.endswith("的") else s + "的"
    k = rnd.randrange(n)
    if kind == "at":
        return f"{X}排第 {k + 1} 個。", lambda w: pos_of(w, attr, x) == k
    if kind == "notat":
        return f"{X}不是第 {k + 1} 個。", lambda w: pos_of(w, attr, x) != k
    if kind == "left":
        return f"{X}在{of(Y)}左邊（不一定相鄰）。", lambda w: pos_of(w, attr, x) < pos_of(w, attr, y)
    if kind == "right_next":
        return f"{X}就在{of(Y)}右邊，緊貼著。", lambda w: pos_of(w, attr, x) == pos_of(w, attr, y) + 1
    if kind == "adj":
        return f"{X}和{Y}相鄰。", lambda w: abs(pos_of(w, attr, x) - pos_of(w, attr, y)) == 1
    if kind == "notadj":
        return f"{X}和{Y}不相鄰。", lambda w: abs(pos_of(w, attr, x) - pos_of(w, attr, y)) != 1
    if kind == "end":
        return f"{X}在最左邊或最右邊。", lambda w: pos_of(w, attr, x) in (0, n - 1)
    if kind == "notend":
        return f"{X}不在兩端。", lambda w: pos_of(w, attr, x) not in (0, n - 1)
    if kind == "gap":
        return f"{X}和{Y}中間剛好隔一個。", lambda w: abs(pos_of(w, attr, x) - pos_of(w, attr, y)) == 2
    if kind == "between":
        return f"{Y}在{X}和{Z}中間（不一定相鄰）。", lambda w: min(pos_of(w, attr, x), pos_of(w, attr, z)) < pos_of(w, attr, y) < max(pos_of(w, attr, x), pos_of(w, attr, z))
    if kind == "same" and two:
        c, d = rnd.randrange(n), rnd.randrange(n)
        return f"{COLORS[c]}買的是{DRINKS[d]}。", lambda w: w[1][pos_of(w, 0, c)] == d
    if kind == "notsame" and two:
        c, d = rnd.randrange(n), rnd.randrange(n)
        return f"{COLORS[c]}買的不是{DRINKS[d]}。", lambda w: w[1][pos_of(w, 0, c)] != d
    if kind == "crossleft" and two:
        c, d = rnd.randrange(n), rnd.randrange(n)
        return f"{COLORS[c]}在買{DRINKS[d]}的左邊。", lambda w: pos_of(w, 0, c) < pos_of(w, 1, d)
    if kind == "crossadj" and two:
        c, d = rnd.randrange(n), rnd.randrange(n)
        return f"{COLORS[c]}和買{DRINKS[d]}的相鄰。", lambda w: abs(pos_of(w, 0, c) - pos_of(w, 1, d)) == 1
    return None


def solve(n, two, clues, lies, exactly_true):
    out = []
    for w in worlds(n, two):
        if exactly_true is not None:
            if sum(1 for _, f in clues if f(w)) != exactly_true:
                continue
        elif any(f(w) == (j in lies) for j, (_, f) in enumerate(clues)):
            continue
        out.append(w)
        if len(out) > 1:
            break
    return out


def count(n, two, clues, cap=50):
    c = 0
    for w in worlds(n, two):
        if all(f(w) for _, f in clues):
            c += 1
            if c >= cap:
                break
    return c


def make(spec, seed):
    rnd = random.Random(seed)
    n, two = spec["n"], spec.get("two", False)
    lo, hi = spec["clues"]
    for attempt in range(spec.get("attempts", 400)):
        target = (tuple(rnd.sample(range(n), n)), tuple(rnd.sample(range(n), n)) if two else None)
        clues = []
        tries = 0
        while len(solve(n, two, clues, set(), None)) != 1 and tries < 400:
            tries += 1
            c = clue(rnd, n, two, spec["kinds"])
            if c is None or not c[1](target) or any(c[0] == x[0] for x in clues):
                continue
            clues.append(c)
        if len(solve(n, two, clues, set(), None)) != 1:
            continue
        order = list(range(len(clues)))
        rnd.shuffle(order)
        keep = list(clues)
        for c in [clues[i] for i in order]:
            trial = [x for x in keep if x is not c]
            if len(solve(n, two, trial, set(), None)) == 1:
                keep = trial
        clues = keep
        # 直接說「X 排第 k 個」的線索不能太多
        if sum(1 for t, _ in clues if "排第" in t or "買的是" in t) > spec.get("max_direct", 99):
            continue
        lies, spoken = set(), []
        k_lies = spec.get("lies", 0)
        hidden = spec.get("hidden", 0)
        if k_lies:
            # 加上謊話線索（對目標是假的），插在隨機的位置
            for _ in range(k_lies):
                for _t in range(200):
                    c = clue(rnd, n, two, spec["kinds"])
                    if c is not None and not c[1](target) and all(c[0] != x[0] for x in clues):
                        break
                pos = rnd.randrange(len(clues) + 1)
                clues.insert(pos, c)
                lies = {x + (x >= pos) for x in lies} | {pos}
            if solve(n, two, clues, set(), None) == [target]:
                continue  # 謊話沒有陷阱（當真也排得對）
            spoken = sorted(set(rnd.sample(range(len(clues)), min(len(clues), spec.get("spoken", len(clues))))) | lies)
        if hidden:
            for _ in range(hidden):
                for _t in range(200):
                    c = clue(rnd, n, two, spec["kinds"])
                    if c is not None and not c[1](target) and all(c[0] != x[0] for x in clues):
                        break
                pos = rnd.randrange(len(clues) + 1)
                clues.insert(pos, c)
                lies = {x + (x >= pos) for x in lies} | {pos}
            if solve(n, two, clues, set(), len(clues) - hidden) != [target]:
                continue
        elif solve(n, two, clues, lies, None) != [target]:
            continue
        if not (lo <= len(clues) <= hi):
            continue
        truthful = [c for j, c in enumerate(clues) if j not in lies]
        half = rnd.sample(truthful, max(1, len(truthful) // 2))
        if count(n, two, half, cap=spec.get("min_half", 4)) < spec.get("min_half", 4):
            continue
        return dict(target=target, clues=clues, lies=sorted(lies), spoken=spoken, hidden=hidden)
    raise RuntimeError(f"找不到題目：{spec}")


S1 = ["at", "notat", "left", "adj", "notadj", "end", "notend"]
S2 = S1 + ["right_next", "gap", "between"]
T = ["at", "notat", "left", "adj", "notadj", "gap", "same", "notsame", "crossleft", "crossadj"]


def O(n, clues, kinds, **kw):
    return dict(n=n, clues=clues, kinds=kinds, **kw)


SPECS = {
    401: O(4, (3, 5), S1, min_half=3),
    402: O(4, (3, 5), S1, min_half=4),
    403: O(5, (4, 6), S1, min_half=8),
    404: O(5, (4, 7), S2, min_half=10, max_direct=1),
    405: O(5, (4, 7), S2, lies=1, min_half=10, max_direct=1),
    406: O(6, (5, 8), S2, min_half=20, max_direct=1),
    407: O(5, (5, 8), S2, lies=2, spoken=5, min_half=10, max_direct=1),
    408: O(3, (3, 6), T, two=True, min_half=6),
    409: O(4, (4, 7), T, two=True, min_half=20, max_direct=2),
    410: O(4, (5, 8), T, two=True, min_half=30, max_direct=1),
    411: O(5, (4, 7), S2, hidden=1, min_half=10, max_direct=1),
    412: O(6, (5, 9), S2, min_half=40, max_direct=0),
    413: O(4, (5, 8), T, two=True, lies=1, spoken=4, min_half=30, max_direct=1),
    414: O(5, (5, 8), S2, lies=2, spoken=6, min_half=10, max_direct=0),
    415: O(4, (5, 9), T, two=True, hidden=1, min_half=30, max_direct=1),
    416: O(6, (6, 9), S2, hidden=1, min_half=40, max_direct=0),
    417: O(4, (6, 9), T, two=True, lies=2, spoken=6, min_half=40, max_direct=1),
    418: O(5, (6, 10), T, two=True, min_half=100, max_direct=1, attempts=60),
    419: O(6, (6, 10), S2, lies=2, spoken=7, min_half=40, max_direct=0),
    420: O(5, (6, 11), T, two=True, lies=2, spoken=7, min_half=100, max_direct=1, attempts=60),
}


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "import tw.dynity.wrongbutton.R\n\n",
        "// 由 tools/s3/order.py 產生（答案唯一、每一條線索都用得到、謊話有陷阱）。請改產生器再重新產生。\n",
        "internal object Ch26Puzzles {\n",
    ]
    xml = ['<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/s3/order.py 產生。台灣繁體中文。 -->\n<resources>\n']
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        p = make(spec, lvl)
        for j, (t, _) in enumerate(p["clues"]):
            xml.append(f'    <string name="l{lvl}_c{j}">{t}</string>\n')
        cl = ", ".join(f"R.string.l{lvl}_c{j}" for j in range(len(p["clues"])))
        colors, drinks = p["target"]
        kt.append(
            f"    val l{lvl} = OrderQuiz({spec['n']}, listOf({cl}), colors = listOf({', '.join(map(str, colors))}), "
            f"drinks = {('listOf(' + ', '.join(map(str, drinks)) + ')') if drinks else 'null'}, "
            f"spoken = setOf({', '.join(map(str, p['spoken']))}), lies = setOf({', '.join(map(str, p['lies']))}), hiddenLies = {p['hidden']})\n"
        )
        ans = "、".join(COLORS[c] + (f"（{DRINKS[drinks[i]]}）" if drinks else "") for i, c in enumerate(colors))
        info[lvl] = dict(answer=ans, lies=[j + 1 for j in p["lies"]], clues=[t for t, _ in p["clues"]])
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    xml.append("</resources>\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(OUT_XML, "w") as f:
            f.write("".join(xml))
        with open(os.path.join(os.path.dirname(__file__), "ch26_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
