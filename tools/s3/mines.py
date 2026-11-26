#!/usr/bin/env python3
"""
第 21 章〈踩地雷〉的盤面產生器與檢查器。

每一個盤面都經過模擬玩家的完整推理檢查：
- 不需要猜：每一步都至少有一格可以靠邏輯確定（考慮所有可能的地雷排法）。
- 難度：有幾步是「只看單一數字推不出來、要把好幾個數字放在一起想」（hard steps）。
- The Button 說謊的數字一定要「有陷阱」：把謊話當真的話，會推出錯的結論。

輸出 app/src/main/kotlin/.../levels/s3/Ch21Boards.kt（固定的盤面，所以提示可以寫「先翻 C3」）。

用法：python3 tools/s3/mines.py          產生全部盤面（會花幾分鐘）
"""
import itertools
import os
import random
import sys
from functools import lru_cache

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch21Boards.kt")
COLS = "ABCDEFGH"


def name(i, cols):
    return f"{COLS[i % cols]}{i // cols + 1}"


def neighbors(i, rows, cols, rule):
    r, c = divmod(i, cols)
    if rule == "eight":
        d = [(a, b) for a in (-1, 0, 1) for b in (-1, 0, 1) if (a, b) != (0, 0)]
    elif rule == "cross":
        d = [(-1, 0), (1, 0), (0, -1), (0, 1)]
    else:
        raise ValueError(rule)
    out = []
    for a, b in d:
        rr, cc = r + a, c + b
        if 0 <= rr < rows and 0 <= cc < cols:
            out.append(rr * cols + cc)
    return out


# ---------------------------------------------------------------- 推理

class Knowledge:
    """玩家知道的事。constraints: (cells, op, value)，op 是 'eq' 或 'ne'。lie_groups: (constraint 的 index 們, 恰好幾個是假的)。"""

    def __init__(self, n, total=None):
        self.n = n
        self.safe = set()
        self.mine = set()
        self.constraints = []
        self.groups = []  # (list of (cells, value)) , k  ── 這一組裡恰好 k 個是假的（看不到臉）
        self.total = total


def solve(k: Knowledge):
    """回傳 (forced_safe, forced_mine)；矛盾時回傳 None。完整枚舉所有一致的排法。"""
    unknown = [i for i in range(k.n) if i not in k.safe and i not in k.mine]
    uset = set(unknown)
    # 把 lie group 展開成各種「哪幾個是假的」的世界
    group_choices = []
    for items, lies in k.groups:
        opts = []
        for liars in itertools.combinations(range(len(items)), lies):
            opts.append([(cells, "ne" if j in liars else "eq", v) for j, (cells, v) in enumerate(items)])
        group_choices.append(opts)
    can_mine, can_safe = set(), set()
    any_world = False
    for combo in itertools.product(*group_choices) if group_choices else [()]:
        cons = list(k.constraints)
        for part in combo:
            cons.extend(part)
        res = solve_world(k, cons, unknown, uset)
        if res is None:
            continue
        any_world = True
        m, s = res
        can_mine |= m
        can_safe |= s
    if not any_world:
        return None
    forced_safe = {i for i in unknown if i not in can_mine}
    forced_mine = {i for i in unknown if i not in can_safe}
    return forced_safe, forced_mine


def solve_world(k, cons, unknown, uset):
    # 化簡：去掉已知格
    red = []
    for cells, op, v in cons:
        known_m = sum(1 for c in cells if c in k.mine)
        cs = tuple(c for c in cells if c in uset)
        red.append((cs, op, v - known_m))
    for cs, op, v in red:
        if op == "eq" and not (0 <= v <= len(cs)):
            return None
        if op == "ne" and len(cs) == 0 and v == 0:
            return None
    frontier = sorted({c for cs, _, _ in red for c in cs})
    fset = set(frontier)
    interior = [c for c in unknown if c not in fset]
    # 連通分量
    adj = {c: set() for c in frontier}
    cons_of = {c: [] for c in frontier}
    for idx, (cs, op, v) in enumerate(red):
        for c in cs:
            cons_of[c].append(idx)
            adj[c] |= set(cs)
    comps, seen = [], set()
    for c in frontier:
        if c in seen:
            continue
        stack, comp = [c], []
        seen.add(c)
        while stack:
            x = stack.pop()
            comp.append(x)
            for y in adj[x]:
                if y not in seen:
                    seen.add(y)
                    stack.append(y)
        comps.append(comp)
    # 每個分量：依地雷數分組，記錄每格可能是雷／可能安全
    comp_results = []
    for comp in comps:
        cidx = sorted({i for c in comp for i in cons_of[c]})
        ccons = [red[i] for i in cidx]
        res = enumerate_comp(comp, ccons)
        if not res:
            return None
        comp_results.append((comp, res))
    # 組合地雷總數
    if k.total is None:
        m, s = set(), set()
        for comp, res in comp_results:
            for cnt, (cm, cs_) in res.items():
                m |= cm
                s |= cs_
        # 內部格沒有限制：可雷可安全
        m |= set(interior)
        s |= set(interior)
        return m, s
    remaining = k.total - len(k.mine)
    # 可行的總數組合：DP over components
    counts = [sorted(res.keys()) for _, res in comp_results]
    nI = len(interior)
    feasible = []
    for combo in itertools.product(*counts) if counts else [()]:
        rest = remaining - sum(combo)
        if 0 <= rest <= nI:
            feasible.append((combo, rest))
    if not feasible:
        return None
    m, s = set(), set()
    for combo, rest in feasible:
        for (comp, res), cnt in zip(comp_results, combo):
            cm, cs_ = res[cnt]
            m |= cm
            s |= cs_
        if rest > 0:
            m |= set(interior)
        if rest < nI:
            s |= set(interior)
    return m, s


def enumerate_comp(comp, ccons):
    order = bfs_order(comp, ccons)
    pos = {c: i for i, c in enumerate(order)}
    # 每個限制：在第幾格之後全部指定完
    last = [max(pos[c] for c in cs) if cs else -1 for cs, _, _ in ccons]
    by_last = {}
    for idx, l in enumerate(last):
        by_last.setdefault(l, []).append(idx)
    touching = {c: [i for i, (cs, _, _) in enumerate(ccons) if c in cs] for c in comp}
    assign = {}
    result = {}

    def ok_partial(c):
        for i in touching[c]:
            cs, op, v = ccons[i]
            if op != "eq":
                continue
            m = sum(1 for x in cs if assign.get(x) == 1)
            u = sum(1 for x in cs if x not in assign)
            if m > v or m + u < v:
                return False
        return True

    def ok_final(i):
        for idx in by_last.get(i, []):
            cs, op, v = ccons[idx]
            m = sum(assign[x] for x in cs)
            if op == "eq" and m != v:
                return False
            if op == "ne" and m == v:
                return False
        return True

    def rec(i):
        if i == len(order):
            cnt = sum(assign.values())
            cm, cs_ = result.setdefault(cnt, (set(), set()))
            for x, val in assign.items():
                (cm if val else cs_).add(x)
            return
        c = order[i]
        for val in (0, 1):
            assign[c] = val
            if ok_partial(c) and ok_final(i):
                rec(i + 1)
            del assign[c]

    # 沒有任何格的限制（cs 空）
    for cs, op, v in ccons:
        if not cs and ((op == "eq" and v != 0) or (op == "ne" and v == 0)):
            return {}
    rec(0)
    return result


def bfs_order(comp, ccons):
    adj = {c: set() for c in comp}
    for cs, _, _ in ccons:
        for c in cs:
            adj[c] |= set(cs)
    start = min(comp)
    order, seen, q = [], {start}, [start]
    while q:
        x = q.pop(0)
        order.append(x)
        for y in sorted(adj[x]):
            if y not in seen:
                seen.add(y)
                q.append(y)
    return order


def simple_forced(k: Knowledge):
    """「好想」的推理：單一數字（剩下的格都是雷／都不是雷），加上兩個數字的子集合規則。只用 eq 限制。"""
    safe, mine = set(), set()
    red = []
    for cells, op, v in k.constraints:
        if op != "eq":
            continue
        cs = frozenset(c for c in cells if c not in k.safe and c not in k.mine)
        v2 = v - sum(1 for c in cells if c in k.mine)
        if cs:
            red.append((cs, v2))
    for cs, v in red:
        if v == 0:
            safe |= cs
        elif v == len(cs):
            mine |= cs
    for (a, va), (b, vb) in itertools.permutations(red, 2):
        if a < b:
            d = b - a
            if vb - va == 0:
                safe |= d
            elif vb - va == len(d):
                mine |= d
    return safe, mine


# ---------------------------------------------------------------- 盤面

class Board:
    def __init__(self, rows, cols, mines, rule="eight"):
        self.rows, self.cols, self.rule = rows, cols, rule
        self.n = rows * cols
        self.mines = set(mines)
        self.nb = [neighbors(i, rows, cols, rule) for i in range(self.n)]

    def num(self, i):
        return sum(1 for j in self.nb[i] if j in self.mines)

    def flood(self, start):
        out, stack = set(), [start]
        while stack:
            x = stack.pop()
            if x in out or x in self.mines:
                continue
            out.add(x)
            if self.num(x) == 0:
                stack.extend(self.nb[x])
        return out


def simulate(b: Board, opened, reported, lies, mode, counter, faces, lie_count=None, trust_lies=False, cap=200):
    """
    模擬玩家。回傳 dict(ok, hard, steps, first)；
    - reported：The Button 說的格子 → 顯示的數字；lies：其中哪些是謊話。
    - faces=False 時玩家看不到臉，只知道「恰好 lie_count 個是假的」。
    - trust_lies=True：把所有說的話都當真（用來確認謊話有陷阱）。
    """
    k = Knowledge(b.n, total=len(b.mines) if counter else None)
    k.safe |= set(opened) | set(reported)
    for i in opened:
        k.constraints.append((tuple(b.nb[i]), "eq", b.num(i)))
    if trust_lies:
        for i, v in reported.items():
            k.constraints.append((tuple(b.nb[i]), "eq", v))
    elif faces:
        for i, v in reported.items():
            k.constraints.append((tuple(b.nb[i]), "ne" if i in lies else "eq", v))
    elif reported:
        k.groups.append(([(tuple(b.nb[i]), v) for i, v in reported.items()], lie_count))
    hard, steps, first = 0, 0, None
    while True:
        if mode == "reveal":
            remaining = [i for i in range(b.n) if i not in b.mines and i not in k.safe]
            if not remaining:
                return dict(ok=True, hard=hard, steps=steps, first=first)
        else:
            if k.mine == b.mines:
                return dict(ok=True, hard=hard, steps=steps, first=first)
        ss, sm = simple_forced(k)
        if ss or sm:
            fs, fm = ss, sm
        else:
            res = solve(k)
            if res is None:
                return dict(ok=False, wrong=True, hard=hard, steps=steps)
            fs, fm = res
            if not fs and not fm:
                return dict(ok=False, hard=hard, steps=steps)
            hard += 1
            # 要用到完整推理的那一步，只算推出一格（玩家想通一格後，通常又能用簡單的規則往下推）。
            one = sorted(fs)[:1] or []
            fs, fm = set(one), (set() if one else set(sorted(fm)[:1]))
        if fs & b.mines or fm - b.mines:
            return dict(ok=False, wrong=True, hard=hard, steps=steps)
        if first is None:
            pick = sorted(fs)[0] if fs else sorted(fm)[0]
            first = (pick, pick in fs, False)
        steps += 1
        k.mine |= fm
        for i in fs:
            if mode == "reveal":
                for j in b.flood(i) if b.num(i) == 0 else {i}:
                    if j not in k.safe:
                        k.safe.add(j)
                        k.constraints.append((tuple(b.nb[j]), "eq", b.num(j)))
            else:
                k.safe.add(i)
        if steps > cap:
            return dict(ok=False, hard=hard, steps=steps)


def make(spec, seed):
    rnd = random.Random(seed)
    rows, cols, nm = spec["rows"], spec["cols"], spec["mines"]
    rule, mode = spec.get("rule", "eight"), spec.get("mode", "reveal")
    n = rows * cols
    for attempt in range(20000):
        mines = rnd.sample(range(n), nm)
        b = Board(rows, cols, mines, rule)
        safe = [i for i in range(n) if i not in b.mines]
        if mode == "reveal":
            zeros = [i for i in safe if b.num(i) == 0]
            if spec.get("start_zero", True):
                if not zeros:
                    continue
                opened = b.flood(rnd.choice(zeros))
                if len(opened) > spec.get("max_open", n // 3) or len(opened) < spec.get("min_open", 1):
                    continue
            else:
                opened = set(rnd.sample(safe, spec.get("open_count", 3)))
            extra = spec.get("extra_open", 0)
            if extra:
                opened |= set(rnd.sample([i for i in safe if i not in opened], extra))
        else:
            opened = set(safe)
        rep_n, lie_n = spec.get("reported", 0), spec.get("lies", 0)
        reported, lies = {}, set()
        if rep_n:
            pool = [i for i in safe if i not in opened]
            if spec.get("report_from_open") or mode == "flag":
                pool = sorted(opened)
            if len(pool) < rep_n:
                continue
            chosen = rnd.sample(pool, rep_n)
            opened -= set(chosen)
            lies = set(rnd.sample(chosen, lie_n))
            for i in chosen:
                t = b.num(i)
                if i in lies:
                    opts = [v for v in (t - 1, t + 1, t + 2, t - 2) if 0 <= v <= len(b.nb[i])]
                    reported[i] = rnd.choice(opts)
                else:
                    reported[i] = t
        faces = spec.get("faces", True)
        counter = spec.get("counter", False) or mode == "flag"
        if mode == "flag":
            # 像數獨挖空：一格一格拿掉數字，拿掉後還能完全推理出來才真的拿掉。
            order = sorted(opened)
            rnd.shuffle(order)
            for i in order:
                trial = opened - {i}
                if simulate(b, trial, reported, lies, mode, counter, faces, lie_n)["ok"]:
                    opened = trial
        r = simulate(b, opened, reported, lies, mode, counter, faces, lie_n)
        if not r["ok"] or r["hard"] < spec.get("hard", 0) or r["hard"] > spec.get("max_hard", 99) or r["steps"] < spec.get("min_steps", 0):
            continue
        if spec.get("need_counter"):
            r2 = simulate(b, opened, reported, lies, mode, False, faces, lie_n)
            if r2["ok"]:
                continue
        if lies and spec.get("tempting", True):
            r3 = simulate(b, opened, reported, lies, mode, counter, True, trust_lies=True)
            if r3["ok"]:
                continue  # 謊話沒有陷阱
        if mode == "reveal" and len(safe) - len(opened) - len(reported) < spec.get("min_hidden_safe", 4):
            continue
        return b, opened, reported, lies, r
    raise RuntimeError(f"找不到盤面：{spec}")


def kt_board(lvl, spec, b, opened, reported, lies, r):
    cols = b.cols
    rows_s = []
    for rr in range(b.rows):
        row = ""
        for cc in range(cols):
            i = rr * cols + cc
            if i in b.mines:
                row += "*"
            elif i in reported:
                row += "L" if i in lies else "R"
            elif i in opened:
                row += "o"
            else:
                row += "."
        rows_s.append(row)
    rep = ", ".join(f"{i} to {v}" for i, v in sorted(reported.items()))
    first_cell, first_safe, _ = r["first"]
    opts = []
    if spec.get("rule", "eight") != "eight":
        opts.append(f"rule = MineRule.{spec['rule'].capitalize()}")
    if spec.get("mode", "reveal") != "reveal":
        opts.append("mode = MineMode.Flag")
    if spec.get("counter") or spec.get("mode") == "flag":
        opts.append("counter = true")
    if not spec.get("faces", True):
        opts.append(f"hiddenLies = {len(lies)}")
    cover = spec.get("cover")
    if cover:
        opts.append(f"cover = tw.dynity.wrongbutton.core.character.FaceCover.{cover}")
    extra = (", " + ", ".join(opts)) if opts else ""
    return (
        f"    // 第 {lvl} 關：{spec['rows']}×{spec['cols']}、{len(b.mines)} 顆雷；推理 {r['steps']} 步，其中 {r['hard']} 步要把幾個數字放在一起想。"
        f"第一步：{name(first_cell, cols)} {'安全' if first_safe else '是雷'}。\n"
        f"    val l{lvl} = MineBoard.parse(\n"
        + "".join(f'        "{row}",\n' for row in rows_s)
        + f"        reported = mapOf({rep}){extra},\n    )\n"
    )


SPECS = {
    301: dict(rows=4, cols=4, mines=2, max_open=8, min_open=4, min_hidden_safe=3, max_hard=0, seed=1),
    302: dict(rows=5, cols=5, mines=4, max_open=10, hard=0, min_steps=3, seed=2),
    303: dict(rows=5, cols=5, mines=4, mode="flag", min_steps=2, max_hard=2, seed=3),
    304: dict(rows=6, cols=6, mines=7, max_open=12, hard=2, min_steps=4, seed=4),
    305: dict(rows=6, cols=6, mines=7, max_open=14, counter=True, need_counter=True, min_steps=3, hard=2, seed=5),
    306: dict(rows=6, cols=6, mines=6, max_open=14, reported=2, lies=1, min_steps=3, seed=6),
    307: dict(rows=6, cols=6, mines=7, max_open=14, reported=3, lies=1, hard=2, min_steps=4, seed=7),
    308: dict(rows=6, cols=6, mines=9, rule="cross", start_zero=False, open_count=5, hard=2, min_steps=4, seed=8),
    309: dict(rows=6, cols=6, mines=8, mode="flag", open_count=16, hard=2, min_steps=3, seed=9),
    310: dict(rows=7, cols=7, mines=9, max_open=16, reported=3, lies=2, cover="Eyes", hard=2, min_steps=4, seed=10),
    311: dict(rows=6, cols=6, mines=8, rule="cross", mode="flag", open_count=15, hard=2, min_steps=3, seed=11),
    312: dict(rows=7, cols=7, mines=10, max_open=16, reported=5, lies=2, report_from_open=True, hard=2, min_steps=4, seed=12),
    313: dict(rows=7, cols=7, mines=9, max_open=16, reported=3, lies=1, cover="Mouth", hard=2, min_steps=4, seed=13),
    314: dict(rows=7, cols=7, mines=9, max_open=16, reported=4, lies=1, faces=False, hard=2, min_steps=4, seed=14),
    315: dict(rows=7, cols=7, mines=10, max_open=18, counter=True, need_counter=True, hard=2, min_steps=4, seed=15),
    316: dict(rows=7, cols=7, mines=13, rule="cross", start_zero=False, open_count=7, reported=3, lies=1, hard=2, min_steps=4, seed=16),
    317: dict(rows=8, cols=8, mines=12, max_open=18, hard=5, min_steps=6, seed=17),
    318: dict(rows=7, cols=7, mines=11, mode="flag", open_count=20, reported=4, lies=2, hard=2, min_steps=3, seed=18),
    319: dict(rows=8, cols=8, mines=12, max_open=20, reported=5, lies=2, faces=False, counter=True, hard=4, min_steps=5, seed=19),
    320: dict(rows=8, cols=8, mines=13, max_open=18, reported=4, lies=2, counter=True, hard=6, min_steps=6, seed=20),
}


def main(only=None):
    out = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "// 由 tools/s3/mines.py 產生（每個盤面都檢查過：不用猜、謊話有陷阱）。請改產生器再重新產生。\n",
        "// 圖例：* 地雷、. 沒翻開的安全格、o 一開始就翻開、R The Button 說的真話、L The Button 說的謊話。\n",
        "internal object Ch21Boards {\n",
    ]
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        b, opened, reported, lies, r = make(spec, spec["seed"])
        out.append(kt_board(lvl, spec, b, opened, reported, lies, r))
        fc, fsafe, _ = r["first"]
        info[lvl] = dict(first=name(fc, b.cols), first_safe=fsafe, steps=r["steps"], hard=r["hard"],
                         lies=[name(i, b.cols) for i in sorted(lies)], reported={name(i, b.cols): v for i, v in reported.items()})
        print(lvl, info[lvl], file=sys.stderr)
    out.append("}\n")
    if not only:
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        with open(OUT, "w") as f:
            f.write("".join(out))
        with open(os.path.join(os.path.dirname(__file__), "ch21_boards.json"), "w") as f:
            import json
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
