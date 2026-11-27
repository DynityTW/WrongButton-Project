#!/usr/bin/env python3
"""
第 23 章〈誰在說謊〉的題目產生器與檢查器。

幾顆沒有臉的按鈕各說一兩句話。每一顆不是「永遠說真話」（T）就是「永遠說謊」（L）；
有些關卡還有一個「間諜」（S，想說什麼就說什麼），或是 The Button 本人混在裡面（目標是找到它）。

暴力列舉所有身分組合，檢查：
- 答案唯一（說謊的是哪幾個；找分身、找間諜的關卡，連說謊的是哪幾個也一起唯一，因為也要標出來）。
- 每一句話都用得到：拿掉任何一句，答案就不唯一。
- 難度：只用一半的句子時，可能的答案至少還有好幾種（不會一兩句就看穿）。

輸出 app/src/main/kotlin/.../levels/s3/Ch23Puzzles.kt 與 app/src/main/res/values/strings_ch23_puzzles.xml。
"""
import itertools
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch23Puzzles.kt")
OUT_XML = os.path.join(ROOT, "app/src/main/res/values/strings_ch23_puzzles.xml")

NAMES = ["紅鈕", "藍鈕", "綠鈕", "黃鈕", "紫鈕", "灰鈕"]


class World:
    def __init__(self, roles, button):
        self.roles, self.button = roles, button


def say_type(r):
    return {"T": "說真話的", "L": "說謊的", "S": "間諜"}[r]


def make_statement(rnd, me, n, kinds, has_button, has_spy):
    """回傳 (文字, 判斷函式(world) -> bool)。文字裡的「我」是說話的那一顆。"""
    others = [i for i in range(n) if i != me]
    kind = rnd.choice(kinds)
    x = rnd.choice(others)
    rest = [i for i in others if i != x]
    if kind in ("same", "diff", "oneof", "if") and not rest:
        return None
    y = rnd.choice(rest or others)
    nm = NAMES
    if kind == "is":
        r = rnd.choice("TL" + ("S" if has_spy else ""))
        return f"{nm[x]}是{say_type(r)}。", lambda w, x=x, r=r: w.roles[x] == r
    if kind == "isnot":
        r = rnd.choice("TL")
        return f"{nm[x]}不是{say_type(r)}。", lambda w, x=x, r=r: w.roles[x] != r
    if kind == "same":
        return f"{nm[x]}和{nm[y]}是同一種。", lambda w, x=x, y=y: w.roles[x] == w.roles[y]
    if kind == "diff":
        return f"{nm[x]}和{nm[y]}不是同一種。", lambda w, x=x, y=y: w.roles[x] != w.roles[y]
    if kind == "me_same":
        return f"我和{nm[x]}是同一種。", lambda w, x=x, me=me: w.roles[x] == w.roles[me]
    if kind == "count":
        k = rnd.randint(1, n - 1)
        return f"我們之中剛好有 {k} 個說謊的。", lambda w, k=k: sum(1 for r in w.roles if r == "L") == k
    if kind == "atleast":
        k = rnd.randint(2, n - 1)
        return f"我們之中至少有 {k} 個說真話的。", lambda w, k=k: sum(1 for r in w.roles if r == "T") >= k
    if kind == "oneof":
        return f"{nm[x]}和{nm[y]}之中，至少一個說謊。", lambda w, x=x, y=y: w.roles[x] == "L" or w.roles[y] == "L"
    if kind == "if":
        return f"如果{nm[x]}說真話，那{nm[y]}也說真話。", lambda w, x=x, y=y: w.roles[x] != "T" or w.roles[y] == "T"
    if kind == "would":
        # 「X 會說我是說謊的」：X 誠實時，我真的是說謊的；X 說謊時，我其實不是說謊的。
        return f"如果你問{nm[x]}，它會說我是說謊的。", lambda w, x=x, me=me: (w.roles[me] == "L") == (w.roles[x] == "T")
    if kind == "neighbors":
        # 排成一列，旁邊的兩個（或一個）
        nb = [i for i in (me - 1, me + 1) if 0 <= i < n]
        if len(nb) == 2:
            return "我旁邊的兩個是同一種。", lambda w, a=nb[0], b=nb[1]: w.roles[a] == w.roles[b]
        return f"我旁邊的{nm[nb[0]]}說謊。", lambda w, a=nb[0]: w.roles[a] == "L"
    if kind == "btn" and has_button:
        return f"分身是{nm[x]}。", lambda w, x=x: w.button == x
    if kind == "btnnot" and has_button:
        return f"分身不是{nm[x]}。", lambda w, x=x: w.button != x
    if kind == "btnme" and has_button:
        return "我不是分身。", lambda w, me=me: w.button != me
    if kind == "btnliar" and has_button:
        return "分身是說謊的。", lambda w: w.roles[w.button] == "L"
    if kind == "btnnext" and has_button:
        return f"分身就在{nm[x]}旁邊。", lambda w, x=x: abs(w.button - x) == 1
    if kind == "spyis" and has_spy:
        return f"間諜是{nm[x]}。", lambda w, x=x: w.roles[x] == "S"
    if kind == "spynot" and has_spy:
        return f"間諜不是{nm[x]}。", lambda w, x=x: w.roles[x] != "S"
    return None


def worlds(n, has_button, spies, exactly_t=None):
    for roles in itertools.product("TL" + ("S" if spies else ""), repeat=n):
        if spies and roles.count("S") != spies:
            continue
        if exactly_t is not None and roles.count("T") != exactly_t:
            continue
        for b in (range(n) if has_button else [None]):
            yield World(roles, b)


def consistent(w, stmts, narrator):
    for who, _, f in stmts:
        if w.roles[who] == "S":
            continue
        if f(w) != (w.roles[who] == "T"):
            return False
    for _, f, truth in narrator:
        if f(w) != truth:
            return False
    return True


def answer_of(w, goal):
    if goal == "liars":
        return tuple(i for i, r in enumerate(w.roles) if r == "L")
    # 找分身／找間諜的關卡，也要標出所有說謊的（不然 3–6 顆一顆一顆試就過了），所以答案要連說謊的一起唯一。
    liars = tuple(i for i, r in enumerate(w.roles) if r == "L")
    if goal == "button":
        return (liars, w.button)
    if goal == "spy":
        return (liars, w.roles.index("S"))
    if goal == "honest":
        return w.roles.index("T")
    raise ValueError(goal)


def answers(spec, stmts, narrator):
    out = set()
    for w in worlds(spec["n"], spec.get("button", False), spec.get("spies", 0), spec.get("exactly_t")):
        if consistent(w, stmts, narrator):
            out.add(answer_of(w, spec["goal"]))
    return out


def make(spec, seed):
    rnd = random.Random(seed)
    n = spec["n"]
    per = spec.get("per", 1)
    for attempt in range(spec.get("attempts", 200000)):
        # 先決定真正的身分，再讓每一句話符合身分（間諜隨便說）。
        roles = [rnd.choice("TL") for _ in range(n)]
        if spec.get("spies"):
            roles[rnd.randrange(n)] = "S"
        truth = World(tuple(roles), rnd.randrange(n) if spec.get("button") else None)
        stmts = []
        must = list(spec.get("must", []))
        for me in range(n):
            for _ in range(per):
                for _try in range(200):
                    kinds = [must[0]] if must else spec["kinds"]
                    st = make_statement(rnd, me, n, kinds, spec.get("button", False), spec.get("spies", 0))
                    if st is None:
                        continue
                    if roles[me] == "S" or st[1](truth) == (roles[me] == "T"):
                        break
                else:
                    st = None
                if st is None:
                    break
                if must:
                    must.pop(0)
                stmts.append((me, st[0], st[1]))
        if len(stmts) != n * per:
            continue
        texts = [t for _, t, _ in stmts]
        if len(set(texts)) < len(texts):
            continue
        # 「X 是說真話的／說謊的」這種直接的句子太多，會變成一條鏈，一下就推完。
        direct = sum(1 for t in texts if not t.startswith("如果你問") and (t.endswith("是說真話的。") or t.endswith("是說謊的。") or "不是說" in t))
        if direct > spec.get("max_direct", len(texts)):
            continue
        narrator = []
        if spec.get("narrator"):
            nt = None
            while nt is None:
                nt = make_statement(rnd, -1, n, spec["kinds"], spec.get("button", False), spec.get("spies", 0))
            narrator = [(nt[0], nt[1], nt[1](truth))]
            if spec.get("narrator_lies") is not None and narrator[0][2] == spec["narrator_lies"]:
                continue
        ans = answers(spec, stmts, narrator)
        if len(ans) != 1:
            continue
        # 每一句都用得到
        ok = True
        # 一人一句：每一句都要用得到；一人兩句：每一個人說的話都要用得到。
        units = [[j] for j in range(len(stmts))] if per == 1 else [[j for j, s in enumerate(stmts) if s[0] == me] for me in range(n)]
        for unit in units:
            if len(answers(spec, [s for j, s in enumerate(stmts) if j not in unit], narrator)) < 2:
                ok = False
                break
        if ok and narrator and len(answers(spec, stmts, [])) < 2:
            ok = False
        if not ok:
            continue
        # 難度：隨便拿掉一半的句子，還要有好幾種可能
        half = rnd.sample(stmts, len(stmts) // 2)
        if len(answers(spec, half, [])) < spec.get("min_half", 2):
            continue
        # 角色不能全部一樣
        if spec["goal"] == "liars" and (roles.count("L") in (0, n)):
            continue
        a = ans.pop()
        assert a == answer_of(truth, spec["goal"])
        return stmts, narrator, a, truth
    raise RuntimeError(f"找不到題目：{spec}")


BASIC = ["is", "isnot", "same", "diff", "me_same"]
COUNT = BASIC + ["count", "atleast", "oneof"]
LOGIC = COUNT + ["if", "would"]
BTN = ["btn", "btnnot", "btnme", "btnliar", "btnnext"]
SPY = ["spyis", "spynot"]


def S(n, goal, kinds, **kw):
    return dict(n=n, goal=goal, kinds=kinds, **kw)


SPECS = {
    341: S(2, "liars", ["is", "same", "diff", "me_same", "count"], min_half=1),
    342: S(3, "liars", BASIC, min_half=2),
    343: S(3, "liars", BASIC + ["count", "oneof"], min_half=2),
    344: S(3, "button", BASIC + BTN, button=True, min_half=2),
    345: S(4, "liars", ["same", "diff", "me_same"], min_half=3, max_direct=1),
    346: S(4, "liars", COUNT, min_half=3, max_direct=2),
    347: S(4, "liars", ["if", "is", "same", "oneof"], must=["if"], min_half=3, max_direct=2),
    348: S(3, "liars", COUNT, per=2, min_half=3),
    349: S(3, "spy", BASIC + SPY, spies=1, min_half=2),
    350: S(4, "liars", COUNT, narrator=True, narrator_lies=True, min_half=3, max_direct=2),
    351: S(5, "liars", ["neighbors", "is", "same", "count"], must=["neighbors", "neighbors"], min_half=4, max_direct=2),
    352: S(4, "liars", ["would", "is", "same", "diff"], must=["would", "would"], min_half=3, max_direct=1),
    353: S(5, "button", COUNT + BTN, button=True, min_half=4, max_direct=2),
    354: S(4, "button", COUNT + BTN + SPY, button=True, spies=1, min_half=3, attempts=400000),
    355: S(5, "liars", COUNT + ["if"], min_half=5, max_direct=1),
    356: S(4, "liars", LOGIC, per=2, min_half=3, max_direct=2),
    357: S(5, "liars", LOGIC, narrator=True, narrator_lies=True, min_half=5, max_direct=2),
    358: S(4, "spy", COUNT + SPY, spies=1, per=2, min_half=3, max_direct=2),
    359: S(6, "liars", LOGIC + ["neighbors"], min_half=6, max_direct=2),
    360: S(6, "button", LOGIC + BTN + SPY, button=True, spies=1, narrator=True, narrator_lies=True, min_half=6, max_direct=2),
}


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "import tw.dynity.wrongbutton.R\n\n",
        "// 由 tools/s3/liars.py 產生（答案唯一、每一句都用得到）。請改產生器再重新產生。\n",
        "internal object Ch23Puzzles {\n",
    ]
    xml = ['<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/s3/liars.py 產生。台灣繁體中文。 -->\n<resources>\n']
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        stmts, narrator, ans, w = make(spec, lvl)
        for j, (who, t, _) in enumerate(stmts):
            xml.append(f'    <string name="l{lvl}_s{j}">{t}</string>\n')
        st = ", ".join(f"Said({who}, R.string.l{lvl}_s{j})" for j, (who, _, _) in enumerate(stmts))
        roles = "".join(w.roles)
        if spec["goal"] == "liars":
            ans_kt = f"setOf({', '.join(map(str, ans))})"
            ans_txt = "、".join(NAMES[i] for i in ans)
        else:
            liars_, who = ans
            ans_kt = f"setOf({who})"
            ans_txt = NAMES[who] + ("；說謊的：" + "、".join(NAMES[i] for i in liars_) if liars_ else "；沒有人說謊")
        opts = [f'goal = LiarGoal.{spec["goal"].capitalize()}', f'roles = "{roles}"']
        if spec.get("button"):
            opts.append(f"button = {w.button}")
        if spec.get("spies"):
            opts.append("spy = true")
        if spec.get("exactly_t") is not None:
            opts.append("oneHonest = true")
        if narrator:
            xml.append(f'    <string name="l{lvl}_narrator">{narrator[0][0]}</string>\n')
            opts.append(f"narrator = R.string.l{lvl}_narrator")
            opts.append(f"narratorTrue = {'true' if narrator[0][2] else 'false'}")
        kt.append(f"    val l{lvl} = LiarPuzzle({spec['n']}, listOf({st}), answer = {ans_kt}, {', '.join(opts)})\n")
        info[lvl] = dict(answer=ans_txt, roles=roles, stmts=[f"{NAMES[who]}：{t}" for who, t, _ in stmts],
                         narrator=(narrator[0][0], narrator[0][2]) if narrator else None)
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    xml.append("</resources>\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(OUT_XML, "w") as f:
            f.write("".join(xml))
        with open(os.path.join(os.path.dirname(__file__), "ch23_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
