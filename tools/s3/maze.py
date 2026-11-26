#!/usr/bin/env python3
"""
第 27 章〈迷宮〉的題目產生器與檢查器。

從左上角出發，走到 The Button 那一格。每一格有一個數字或箭頭：
- 數字 k：往上下左右其中一個方向，剛好跳 k 格。
- 箭頭：往箭頭的方向走，幾格都可以（不能出界）。
步數有上限（剛好是最短路徑的長度）。產生器用 BFS 檢查：
- 在步數上限內，到得了的路線只有一條（最短路徑唯一）。
- 迷宮夠大：從起點走得到的格子很多（死路很多），不能靠亂走。
- 有些箭頭要聽 The Button 說；說謊時，真正的方向是相反的。正確路線一定會經過說謊的箭頭（把謊話當真就走不到）。

輸出 app/src/main/kotlin/.../levels/s3/Ch27Puzzles.kt 與 tools/s3/ch27_puzzles.json。
"""
import json
import os
import random
import sys
from collections import deque

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch27Puzzles.kt")
DIRS = {"U": (-1, 0), "D": (1, 0), "L": (0, -1), "R": (0, 1)}
OPP = {"U": "D", "D": "U", "L": "R", "R": "L"}


def moves(grid, R, C, i, goal):
    if i == goal:
        return []
    r, c = divmod(i, C)
    v = grid[i]
    out = []
    if isinstance(v, int):
        for dr, dc in DIRS.values():
            rr, cc = r + dr * v, c + dc * v
            if 0 <= rr < R and 0 <= cc < C:
                out.append(rr * C + cc)
    else:
        dr, dc = DIRS[v]
        rr, cc = r + dr, c + dc
        while 0 <= rr < R and 0 <= cc < C:
            out.append(rr * C + cc)
            rr, cc = rr + dr, cc + dc
    return out


def bfs(grid, R, C, start, goal):
    """回傳 (到終點的最短距離, 最短路徑數量, 一條最短路徑, 走得到的格子數)。"""
    dist = {start: 0}
    ways = {start: 1}
    prev = {}
    q = deque([start])
    while q:
        x = q.popleft()
        for y in moves(grid, R, C, x, goal):
            if y not in dist:
                dist[y] = dist[x] + 1
                ways[y] = ways[x]
                prev[y] = x
                q.append(y)
            elif dist[y] == dist[x] + 1:
                ways[y] = min(ways[y] + ways[x], 99)
    if goal not in dist:
        return None, 0, None, len(dist)
    path = [goal]
    while path[-1] != start:
        path.append(prev[path[-1]])
    return dist[goal], ways[goal], path[::-1], len(dist)


def make(spec, seed):
    rnd = random.Random(seed)
    R = C = spec["size"]
    n = R * C
    lo, hi = spec["steps"]
    for attempt in range(spec.get("attempts", 20000)):
        grid = []
        for i in range(n):
            if spec["kind"] == "num" or (spec["kind"] == "mix" and rnd.random() < 0.5):
                grid.append(rnd.randint(1, max(1, R - 2)))
            else:
                grid.append(rnd.choice("UDLR"))
        start = 0
        goal = rnd.randrange(n // 2, n)
        grid[goal] = "G"
        d, w, path, reach = bfs(grid, R, C, start, goal)
        if d is None or not (lo <= d <= hi) or w != 1:
            continue
        if reach < spec.get("min_reach", int(n * 0.6)):
            continue
        lies = set()
        spoken = []
        if spec.get("spoken"):
            arrows = [i for i in range(n) if isinstance(grid[i], str) and grid[i] in DIRS and i != goal]
            on_path = [i for i in path[:-1] if i in arrows]
            k = spec.get("lies", 0)
            if len(on_path) < k:
                continue
            lies = set(rnd.sample(on_path, k))
            others = [i for i in arrows if i not in lies]
            if len(others) < spec["spoken"] - k:
                continue
            spoken = sorted(lies | set(rnd.sample(others, spec["spoken"] - k)))
            shown = list(grid)
            for i in lies:
                shown[i] = OPP[grid[i]]
            # 把謊話當真：走不到，或要更多步
            d2, w2, _, _ = bfs(shown, R, C, start, goal)
            if d2 is not None and d2 <= d:
                continue
        else:
            shown = list(grid)
        return dict(grid=grid, shown=shown, size=R, start=start, goal=goal, steps=d, path=path, lies=sorted(lies), spoken=spoken, reach=reach)
    raise RuntimeError(f"找不到題目：{spec}")


def M(size, kind, steps, **kw):
    return dict(size=size, kind=kind, steps=steps, **kw)


SPECS = {
    421: M(4, "num", (4, 5)),
    422: M(5, "num", (5, 6)),
    423: M(5, "num", (7, 8)),
    424: M(5, "arrow", (5, 6)),
    425: M(5, "arrow", (7, 8)),
    426: M(5, "mix", (7, 8)),
    427: M(6, "num", (9, 10)),
    428: M(6, "arrow", (9, 10)),
    429: M(5, "arrow", (6, 8), spoken=3, lies=1),
    430: M(6, "mix", (10, 11)),
    431: M(6, "num", (11, 13)),
    432: M(6, "arrow", (8, 10), spoken=4, lies=1),
    433: M(7, "mix", (10, 12)),
    434: M(7, "arrow", (11, 13)),
    435: M(7, "num", (12, 14)),
    436: M(6, "mix", (9, 11), spoken=5, lies=2),
    437: M(7, "arrow", (10, 12), spoken=5, lies=2),
    438: M(7, "num", (14, 16)),
    439: M(7, "mix", (12, 14), spoken=4, lies=1),
    440: M(7, "mix", (14, 17), spoken=6, lies=2),
}


def name(i, C):
    return f"{'ABCDEFG'[i % C]}{i // C + 1}"


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "// 由 tools/s3/maze.py 產生（步數上限內只有一條路、死路很多、謊話箭頭在正確路線上）。請改產生器再重新產生。\n",
        "// 格子：數字＝剛好跳幾格；U/D/L/R＝往那個方向走幾格都可以；G＝The Button。這裡是畫面上顯示（它說）的箭頭。\n",
        "internal object Ch27Puzzles {\n",
    ]
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        p = make(spec, lvl)
        R = p["size"]
        cells = ", ".join(f'"{v}"' for v in p["shown"])
        kt.append(f"    // 路線：{' → '.join(name(i, R) for i in p['path'])}（走得到 {p['reach']} 格）\n")
        kt.append(
            f"    val l{lvl} = MazeQuiz({R}, listOf({cells}), goal = {p['goal']}, steps = {p['steps']}, "
            f"spoken = setOf({', '.join(map(str, p['spoken']))}), lies = setOf({', '.join(map(str, p['lies']))}), "
            f"path = listOf({', '.join(map(str, p['path']))}))\n"
        )
        info[lvl] = dict(path=[name(i, R) for i in p["path"]], steps=p["steps"], lies=[name(i, R) for i in p["lies"]], reach=p["reach"])
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(os.path.join(os.path.dirname(__file__), "ch27_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
