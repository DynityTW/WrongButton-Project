#!/usr/bin/env python3
"""
第 22 章〈我不是機器人〉的驗證題產生器與檢查器。

每一題是一組圖片格子（顏色、形狀、數量），加上幾句關於「正確答案」的話。產生器用暴力法檢查所有可能的選法：
- 答案唯一。
- 每一句話都用得到：拿掉任何一句，答案就不唯一（所以要把每一句都想過）。
- 有說謊的話時，把謊話當真會得到錯的答案，或根本沒有答案。

輸出：
  app/src/main/kotlin/.../levels/s3/Ch22Puzzles.kt   格子、答案、每句話的說話方式
  app/src/main/res/values/strings_ch22_puzzles.xml   每句話的文字

用法：python3 tools/s3/captcha.py
"""
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch22Puzzles.kt")
OUT_XML = os.path.join(ROOT, "app/src/main/res/values/strings_ch22_puzzles.xml")

COLORS = ["紅色", "藍色", "綠色", "黃色"]
SHAPES = ["圓形", "方形", "三角形", "星星"]


def popcount(m):
    return bin(m).count("1")


# ---------------------------------------------------------------- 條件（對一格）

def preds(tiles):
    """所有可能的格子條件：(名詞, 判斷函式)。名詞接在「都是」「所有……都要選」「沒有」後面。"""
    out = []
    used_c = sorted({t[0] for t in tiles})
    used_s = sorted({t[1] for t in tiles})
    for c in used_c:
        out.append((f"{COLORS[c]}的", lambda t, c=c: t[0] == c))
    for s in used_s:
        out.append((SHAPES[s], lambda t, s=s: t[1] == s))
    for c in used_c:
        for s in used_s:
            if any(t[0] == c and t[1] == s for t in tiles):
                out.append((f"{COLORS[c]}的{SHAPES[s]}", lambda t, c=c, s=s: t[0] == c and t[1] == s))
    out.append(("只有一個圖形的格子", lambda t: t[2] == 1))
    if any(t[2] >= 2 for t in tiles):
        out.append(("圖形不只一個的格子", lambda t: t[2] >= 2))
    if any(t[2] >= 3 for t in tiles):
        out.append(("有三個圖形的格子", lambda t: t[2] >= 3))
    return out


def neg_noun(base):
    """「不是紅色的格子」「不是只有一個圖形的格子」。"""
    if base.endswith("格子"):
        return "不是" + base
    return "不是" + base.rstrip("的") + "的格子"


def pic(base):
    return f"選出所有{base}圖片。" if base.endswith("的") else f"選出所有{base}。"


# ---------------------------------------------------------------- 關於答案的句子

class Stmt:
    def __init__(self, text, fn, kind):
        self.text, self.fn, self.kind = text, fn, kind


def statements(tiles, size, rnd, kinds):
    n = len(tiles)
    ps = preds(tiles)
    masks = [sum(1 << i for i, t in enumerate(tiles) if f(t)) for _, f in ps]
    out = []
    for kind in kinds:
        j = rnd.randrange(len(ps))
        base, m = ps[j][0], masks[j]
        neg = rnd.random() < 0.35 and not base.endswith("格子")
        full = (1 << n) - 1
        if neg and kind in ("exact", "all", "covers", "countp"):
            m = full & ~m
        if kind == "exact":
            text = pic(neg_noun(base) if neg else base)
            out.append(Stmt(text, lambda a, m=m: a == m, kind))
        elif kind == "all":
            out.append(Stmt(f"選的每一格都{'不' if neg else ''}是{base}。", lambda a, m=m: a & ~m == 0, kind))
        elif kind == "covers":
            out.append(Stmt(f"所有{neg_noun(base) if neg else base}都要選。", lambda a, m=m: a & m == m, kind))
        elif kind == "none":
            out.append(Stmt(f"選的格子裡沒有{base}。", lambda a, m=m: a & m == 0, kind))
        elif kind == "countp":
            k = rnd.randint(1, 3)
            out.append(Stmt(f"選的格子裡，剛好有 {k} 格{'不' if neg else ''}是{base}。", lambda a, m=m, k=k: popcount(a & m) == k, kind))
        elif kind == "count":
            k = rnd.randint(2, max(2, n // 2))
            out.append(Stmt(f"剛好選 {k} 格。", lambda a, k=k: popcount(a) == k, kind))
        elif kind == "sum":
            k = rnd.randint(4, 9)
            out.append(Stmt(f"選的格子裡，圖形加起來剛好 {k} 個。",
                            lambda a, k=k: sum(tiles[i][2] for i in range(n) if a >> i & 1) == k, kind))
        elif kind == "possum":
            k = rnd.randint(8, 20)
            out.append(Stmt(f"選的格子編號加起來剛好是 {k}。", lambda a, k=k: sum(i + 1 for i in range(n) if a >> i & 1) == k, kind))
        elif kind == "rows":
            out.append(Stmt("每一列都剛好選一格。", lambda a: all(popcount((a >> (r * size)) & ((1 << size) - 1)) == 1 for r in range(size)), kind))
        elif kind == "cols":
            colmask = [sum(1 << (r * size + c) for r in range(size)) for c in range(size)]
            out.append(Stmt("每一行都剛好選一格。", lambda a, cm=colmask: all(popcount(a & m) == 1 for m in cm), kind))
        elif kind == "pick":
            i = rnd.randrange(n)
            out.append(Stmt(f"第 {i + 1} 格要選。", lambda a, i=i: a >> i & 1 == 1, kind))
        elif kind == "skip":
            i = rnd.randrange(n)
            out.append(Stmt(f"第 {i + 1} 格不要選。", lambda a, i=i: a >> i & 1 == 0, kind))
        elif kind == "adjacent":
            def ok(a):
                for i in range(n):
                    if a >> i & 1:
                        r, c = divmod(i, size)
                        if c + 1 < size and a >> (i + 1) & 1:
                            return False
                        if r + 1 < size and a >> (i + size) & 1:
                            return False
                return True
            out.append(Stmt("選的格子不能上下或左右相鄰。", ok, kind))
        else:
            raise ValueError(kind)
    return out


def truth_ok(a, stmts, lies, exactly_true):
    """lies：已知是謊話的句子（看得到臉）。exactly_true：看不到臉，只知道剛好幾句是真的。"""
    if exactly_true is not None:
        return sum(1 for s in stmts if s.fn(a)) == exactly_true
    for j, s in enumerate(stmts):
        if s.kind == "exact" and j in lies:
            # 「選出所有 X」說謊 ⇒ 要選的剛好是相反的那些（本章規則）。
            if not s.fn(~a & ((1 << 30) - 1) & full_mask_holder[0]):
                return False
        elif s.fn(a) == (j in lies):
            return False
    return True


full_mask_holder = [0]


def solutions(n, stmts, lies, exactly_true, limit=3):
    out = []
    for a in range(1, 1 << n):
        if truth_ok(a, stmts, lies, exactly_true):
            out.append(a)
            if len(out) >= limit:
                break
    return out


def make(spec, seed):
    rnd = random.Random(seed)
    size = spec["size"]
    n = size * size
    full_mask_holder[0] = (1 << n) - 1
    for attempt in range(spec.get("attempts", 40000)):
        nc, ns = spec.get("colors", 3), spec.get("shapes", 3)
        tiles = [(rnd.randrange(nc), rnd.randrange(ns), rnd.randint(1, spec.get("maxcount", 3))) for _ in range(n)]
        must = spec.get("must", [])
        pool = [k for k in spec.get("pool", []) if k not in must]
        kinds = must + rnd.sample(pool, spec.get("num", len(must)) - len(must))
        rnd.shuffle(kinds)
        stmts = statements(tiles, size, rnd, kinds)
        if len({s.text for s in stmts}) < len(stmts):
            continue
        k_lies = spec.get("lies", 0)
        exactly = spec.get("exactly_true")
        lies = set(rnd.sample(range(len(stmts)), k_lies)) if k_lies and exactly is None else set()
        if spec.get("lie_kind"):
            # 指定哪些句子可以說謊（例如「選出所有……」那句）
            cand = [j for j, s in enumerate(stmts) if s.kind == spec["lie_kind"]]
            if len(cand) < k_lies:
                continue
            lies = set(rnd.sample(cand, k_lies))
        sol = solutions(n, stmts, lies, exactly, 2)
        if len(sol) != 1:
            continue
        a = sol[0]
        if not (spec.get("min_pick", 2) <= popcount(a) <= n - 1):
            continue
        # 每一句都要用得到
        needed = True
        if exactly is None and len(stmts) > 1:
            for j in range(len(stmts)):
                rest = stmts[:j] + stmts[j + 1:]
                rl = {x - (x > j) for x in lies if x != j}
                if len(solutions(n, rest, rl, None, 2)) < 2:
                    needed = False
                    break
        if not needed:
            continue
        # 謊話要有陷阱：全部當真的話，不是沒有答案就是答案不同
        if lies:
            trusting = solutions(n, stmts, set(), None, 2)
            if trusting == [a]:
                continue
        if exactly is not None:
            # 只看真話數量時，也要「每一句都可能是假的那一句」才有推理空間：至少要試過兩種組合
            truths = [s.fn(a) for s in stmts]
            if sum(truths) != exactly:
                continue
        return tiles, stmts, lies, a
    raise RuntimeError(f"找不到題目：{spec}")


# ---------------------------------------------------------------- 關卡規格

BASIC = ["all", "covers", "none", "count", "countp", "pick", "skip"]
ARITH = BASIC + ["sum", "possum"]
GRID = ARITH + ["adjacent"]


def R(size, num, must=(), pool=BASIC, **kw):
    return dict(size=size, num=num, must=list(must), pool=list(pool), **kw)


def X(size, **kw):
    """一句「選出所有……」。"""
    return dict(size=size, num=1, must=["exact"], pool=[], **kw)


SPECS = {
    321: [X(3, colors=3, shapes=2, maxcount=1)],
    322: [X(3, maxcount=2), X(3)],
    323: [R(3, 2, ["all", "covers"]), R(3, 2, ["covers"])],
    324: [X(3), R(3, 2), R(3, 3)],
    325: [X(3, spoken=True, lies=1, lie_kind="exact"), X(3, spoken=True), X(3, spoken=True, lies=1, lie_kind="exact")],
    326: [R(3, 3), R(3, 3, ["count"])],
    327: [R(3, 3, ["sum"], ARITH), R(3, 3, ["sum"], ARITH)],
    328: [R(3, 3, ["rows", "cols"]), R(3, 3, ["rows", "cols"], ARITH)],
    329: [R(3, 3, (), ARITH, spoken=True, lies=1, cover="Eyes"), R(3, 3, (), ARITH, spoken=True, lies=1, cover="Eyes")],
    330: [R(3, 3, (), BASIC, exactly_true=1)],
    331: [R(3, 3, ["adjacent"], ARITH), R(3, 3, ["adjacent"], ARITH)],
    332: [R(4, 4, (), ARITH, colors=4, shapes=3), R(4, 4, ["sum"], ARITH, colors=4, shapes=3)],
    333: [R(3, 4, (), ARITH, spoken=True, lies=2)],
    334: [R(3, 4, (), ARITH, exactly_true=2)],
    335: [R(3, 3, ["possum"], ARITH), R(3, 3, ["possum", "adjacent"], ARITH)],
    336: [R(4, 4, ["rows", "cols"], ARITH, colors=4, shapes=4), R(4, 4, ["rows", "cols", "sum"], ARITH, colors=4, shapes=4)],
    337: [R(3, 4, (), ARITH, spoken=True, lies=2, cover="Mouth")],
    338: [R(3, 4, (), ARITH, exactly_true=1), R(3, 4, ["adjacent"], GRID, exactly_true=2)],
    339: [R(4, 4, ["adjacent"], GRID, colors=4, shapes=4), R(4, 4, ["rows", "cols"], ARITH, colors=4, shapes=4),
          R(4, 4, ["possum"], ARITH, colors=4, shapes=4)],
    340: [R(4, 5, ["rows", "cols"], ARITH, spoken=True, lies=2, colors=4, shapes=4, subtle=True),
          R(4, 5, (), GRID, exactly_true=3, colors=4, shapes=4)],
}


def main():
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "import tw.dynity.wrongbutton.R\n\n",
        "// 由 tools/s3/captcha.py 產生（答案唯一、每一句都用得到、謊話有陷阱）。請改產生器再重新產生。\n",
        "internal object Ch22Puzzles {\n",
    ]
    xml = ['<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/s3/captcha.py 產生。台灣繁體中文。 -->\n<resources>\n']
    info = {}
    for lvl, rounds in SPECS.items():
        kt.append(f"    val l{lvl} = listOf(\n")
        info[lvl] = []
        for ri, spec in enumerate(rounds):
            tiles, stmts, lies, a = make(spec, lvl * 100 + ri)
            n = len(tiles)
            for j, s in enumerate(stmts):
                xml.append(f'    <string name="l{lvl}_r{ri}_s{j}">{s.text}</string>\n')
            tl = ", ".join(f"Tile({c}, {s}, {k})" for c, s, k in tiles)
            st = ", ".join(f"R.string.l{lvl}_r{ri}_s{j}" for j in range(len(stmts)))
            answer = sorted(i for i in range(n) if a >> i & 1)
            opts = []
            if spec.get("spoken"):
                opts.append("spoken = true")
            if lies:
                opts.append(f"lies = setOf({', '.join(map(str, sorted(lies)))})")
            if spec.get("exactly_true") is not None:
                opts.append(f"exactlyTrue = {spec['exactly_true']}")
            if spec.get("cover"):
                opts.append(f"cover = tw.dynity.wrongbutton.core.character.FaceCover.{spec['cover']}")
            if spec.get("subtle"):
                opts.append("subtle = true")
            if spec.get("lie_kind") == "exact":
                opts.append("complementRule = true")
            extra = "".join(", " + o for o in opts)
            kt.append(f"        Captcha({spec['size']}, listOf({tl}), listOf({st}), answer = setOf({', '.join(map(str, answer))}){extra}),\n")
            info[lvl].append(dict(answer=[i + 1 for i in answer], lies=[j + 1 for j in sorted(lies)], stmts=[s.text for s in stmts]))
            print(lvl, ri, [s.text for s in stmts], "lies", sorted(lies), "→", [i + 1 for i in answer], file=sys.stderr)
        kt.append("    )\n")
    kt.append("}\n")
    xml.append("</resources>\n")
    with open(OUT_KT, "w") as f:
        f.write("".join(kt))
    with open(OUT_XML, "w") as f:
        f.write("".join(xml))
    with open(os.path.join(os.path.dirname(__file__), "ch22_puzzles.json"), "w") as f:
        json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()
