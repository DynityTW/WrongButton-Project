#!/usr/bin/env python3
"""
第 29 章〈鏡子〉的題目產生器與檢查器。

雷射從左邊某一列射進格子。格子裡有固定的鏡子（／或＼）和可以轉的鏡子（灰底）。
把可以轉的鏡子轉好，讓雷射射出格子時剛好打到 The Button（在格子外面某一個位置）。按「發射」之前看不到光線。

產生器列舉所有可以轉的鏡子的組合（最多 2^9），檢查：
- 只有一種組合打得到 The Button（所以每一面可轉的鏡子都在光線的路上）。
- 有些固定的鏡子要聽 The Button 說方向；說謊時方向相反。把謊話當真的話，那一種組合打不到（或答案不同）。

輸出 app/src/main/kotlin/.../levels/s3/Ch29Puzzles.kt 與 tools/s3/ch29_puzzles.json。
"""
import itertools
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch29Puzzles.kt")

# 方向：0 右、1 下、2 左、3 上
D = [(0, 1), (1, 0), (0, -1), (-1, 0)]
SLASH = {0: 3, 3: 0, 1: 2, 2: 1}      # ／：右→上、上→右、下→左、左→下
BACK = {0: 1, 1: 0, 2: 3, 3: 2}       # ＼：右→下、下→右、左→上、上→左


def trace(grid, R, C, row):
    """回傳 (出口, 經過的格子)。出口：(邊, 位置)，邊 0 右、1 下、2 左、3 上。迴圈時回傳 None。"""
    r, c, d = row, 0, 0
    seen = set()
    path = []
    while 0 <= r < R and 0 <= c < C:
        if (r, c, d) in seen:
            return None, path
        seen.add((r, c, d))
        path.append(r * C + c)
        m = grid[r * C + c]
        if m == "/":
            d = SLASH[d]
        elif m == "\\":
            d = BACK[d]
        r, c = r + D[d][0], c + D[d][1]
    if c >= C:
        return (0, r), path
    if r >= R:
        return (1, c), path
    if c < 0:
        return (2, r), path
    return (3, c), path


def flip(m):
    return "\\" if m == "/" else "/"


def make(spec, seed):
    rnd = random.Random(seed)
    R = C = spec["size"]
    n = R * C
    k = spec["rot"]
    for attempt in range(spec.get("attempts", 20000)):
        grid = ["." for _ in range(n)]
        cells = list(range(n))
        rnd.shuffle(cells)
        fixed = cells[: spec["fixed"]]
        for i in fixed:
            grid[i] = rnd.choice("/\\")
        row = rnd.randrange(R)
        # 一面一面把可轉的鏡子放在目前光線經過的空格上
        g = list(grid)
        rot = []
        ok = True
        for _ in range(k):
            ex, path = trace(g, R, C, row)
            free = [i for i in path if g[i] == "." and i not in rot]
            if ex is None or not free:
                ok = False
                break
            i = rnd.choice(free)
            rot.append(i)
            g[i] = rnd.choice("/\\")
        if not ok:
            continue
        rot.sort()
        answer = tuple(g[i] for i in rot)
        exit_, path = trace(g, R, C, row)
        if exit_ is None or exit_ == (2, row):
            continue
        if not all(i in path for i in rot):
            continue
        hits = []
        for combo in itertools.product("/\\", repeat=k):
            g2 = list(grid)
            for i, m in zip(rot, combo):
                g2[i] = m
            if trace(g2, R, C, row)[0] == exit_:
                hits.append(combo)
                if len(hits) > 1:
                    break
        if hits != [answer]:
            continue
        # 光線要轉幾次彎：太少就太簡單
        turns = sum(1 for i in path if g[i] != ".")
        if turns < spec.get("min_turns", 0):
            continue
        lies, spoken = set(), []
        if spec.get("spoken"):
            on_path = [i for i in fixed if i in path]
            if len(on_path) < spec.get("lies", 0):
                continue
            lies = set(rnd.sample(on_path, spec.get("lies", 0)))
            others = [i for i in fixed if i not in lies]
            spoken = sorted(lies | set(rnd.sample(others, min(len(others), spec["spoken"] - len(lies)))))
            shown = list(grid)
            for i in lies:
                shown[i] = flip(grid[i])
            # 把謊話當真：同一組打不到
            g3 = list(shown)
            for i, m in zip(rot, answer):
                g3[i] = m
            if lies and trace(g3, R, C, row)[0] == exit_:
                continue
        else:
            shown = list(grid)
        return dict(grid=grid, shown=shown, size=R, row=row, rot=rot, answer=answer, exit=exit_, lies=sorted(lies), spoken=spoken, turns=turns, path=path)
    raise RuntimeError(f"找不到題目：{spec}")


def M(size, fixed, rot, **kw):
    return dict(size=size, fixed=fixed, rot=rot, **kw)


SPECS = {
    461: M(4, 2, 2, min_turns=3),
    462: M(4, 3, 3, min_turns=4),
    463: M(5, 4, 3, min_turns=5),
    464: M(5, 5, 4, min_turns=6),
    465: M(5, 5, 4, spoken=2, lies=1, min_turns=6),
    466: M(5, 6, 5, min_turns=7),
    467: M(6, 7, 5, min_turns=8),
    468: M(6, 7, 5, spoken=3, lies=1, min_turns=8),
    469: M(6, 8, 6, min_turns=9),
    470: M(6, 8, 6, spoken=4, lies=1, min_turns=9),
    471: M(6, 9, 6, spoken=4, lies=2, min_turns=9),
    472: M(7, 9, 6, min_turns=10),
    473: M(7, 10, 7, min_turns=10),
    474: M(7, 10, 7, spoken=4, lies=1, min_turns=11),
    475: M(7, 11, 7, spoken=5, lies=2, min_turns=11),
    476: M(7, 11, 8, min_turns=12),
    477: M(7, 12, 8, spoken=5, lies=2, min_turns=12),
    478: M(7, 12, 8, spoken=6, lies=2, min_turns=13),
    479: M(7, 13, 9, spoken=5, lies=1, min_turns=13),
    480: M(7, 13, 9, spoken=6, lies=2, min_turns=14),
}


def name(i, C):
    return f"{'ABCDEFG'[i % C]}{i // C + 1}"


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "// 由 tools/s3/mirrors.py 產生（只有一種轉法打得到、謊話鏡子在光線上）。請改產生器再重新產生。\n",
        "// 格子：. 空、/ 或 \\ 固定鏡子（畫面上顯示、它說的方向）、R 可以轉的鏡子。\n",
        "internal object Ch29Puzzles {\n",
    ]
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        p = make(spec, lvl)
        R = p["size"]
        cells = list(p["shown"])
        for i in p["rot"]:
            cells[i] = "R"
        cs = ", ".join('"\\\\"' if v == "\\" else f'"{v}"' for v in cells)
        ans = ", ".join('"\\\\"' if v == "\\" else f'"{v}"' for v in p["answer"])
        side, pos = p["exit"]
        kt.append(f"    // 光線經過 {len(p['path'])} 格、轉 {p['turns']} 次彎。\n")
        kt.append(
            f"    val l{lvl} = MirrorQuiz({R}, listOf({cs}), row = {p['row']}, exitSide = {side}, exitPos = {pos}, "
            f"answer = listOf({ans}), spoken = setOf({', '.join(map(str, p['spoken']))}), lies = setOf({', '.join(map(str, p['lies']))}))\n"
        )
        info[lvl] = dict(answer={name(i, R): m for i, m in zip(p["rot"], p["answer"])}, lies=[name(i, R) for i in p["lies"]], turns=p["turns"])
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(os.path.join(os.path.dirname(__file__), "ch29_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
