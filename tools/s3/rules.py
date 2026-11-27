#!/usr/bin/env python3
"""
第 24 章〈猜規則〉的題目產生器與檢查器。

兩種題目：
1. 找規則（第 361–370 關）：The Button 心裡有一條規則（例如「紅色或三角形」）。畫面上有幾張已經標好「符合／不符合」的圖片，
   下面六張候選要一張一張判斷符不符合（符合的可能不只一張），全部判斷對才算過。產生器列舉約三百條可能的規則，檢查：
   - 和所有範例一致的規則，對四張候選的判斷完全相同，而且剛好一張符合（不用猜）。
   - 每一張範例都用得到：拿掉任何一張，候選的答案就不確定了。
   - 有些範例的標籤要聽 The Button 說；說謊時標籤相反。把謊話當真會選錯。
2. 猜密碼（第 371–380 關）：密碼是幾個顏色排成一排。畫面上有幾次之前的猜測，每次都有回饋：
   ● 顏色和位置都對的數量、○ 顏色對但位置不對的數量。推出唯一的密碼。
   - 每一次猜測都用得到。
   - 有些回饋要聽 The Button 說（說謊時，真正的回饋一定不是它說的那個）；有些關卡看不到臉，只知道剛好一行是假的。

輸出 app/src/main/kotlin/.../levels/s3/Ch24Puzzles.kt 與 tools/s3/ch24_puzzles.json。
"""
import itertools
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch24Puzzles.kt")

COLORS = ["紅色", "藍色", "綠色", "黃色"]
SHAPES = ["圓形", "方形", "三角形", "星星"]
ALL_TILES = [(c, s, k) for c in range(4) for s in range(4) for k in (1, 2, 3)]


# ---------------------------------------------------------------- 找規則

def atoms():
    out = []
    for c in range(4):
        out.append((f"{COLORS[c]}", lambda t, c=c: t[0] == c, 1))
    for s in range(4):
        out.append((f"{SHAPES[s]}", lambda t, s=s: t[1] == s, 1))
    for k in (1, 2, 3):
        out.append((f"剛好 {k} 個圖形", lambda t, k=k: t[2] == k, 1))
    out.append(("2 個以上圖形", lambda t: t[2] >= 2, 1))
    return out


def rules():
    A = atoms()
    out = []
    for name, f, w in A:
        out.append((name, f, 1))
        out.append((f"不是{name}", lambda t, f=f: not f(t), 2))
    for (n1, f1, _), (n2, f2, _) in itertools.combinations(A, 2):
        out.append((f"{n1}而且 {n2}" if n2[0].isdigit() else f"{n1}而且{n2}", lambda t, f1=f1, f2=f2: f1(t) and f2(t), 2))
        out.append((f"{n1}或 {n2}" if n2[0].isdigit() else f"{n1}或{n2}", lambda t, f1=f1, f2=f2: f1(t) or f2(t), 3))
    for (n1, f1, _), (n2, f2, _) in itertools.permutations(A, 2):
        out.append((f"{n1}但不是{n2}", lambda t, f1=f1, f2=f2: f1(t) and not f2(t), 3))
    # 只留下會把 48 張圖片分成兩邊的規則，去掉完全相同的
    seen, uniq = set(), []
    for name, f, w in out:
        sig = tuple(f(t) for t in ALL_TILES)
        if all(sig) or not any(sig) or sig in seen:
            continue
        seen.add(sig)
        uniq.append((name, f, w))
    return uniq


RULES = rules()
USED = set()  # 每一關的規則都不一樣


def consistent_rules(examples):
    """examples: [(tile, label)]"""
    return [r for r in RULES if all(r[1](t) == lab for t, lab in examples)]


def candidates_ok(rs, cands):
    """
    所有一致的規則，對每一張候選的判斷都相同時，回傳「符合的是哪幾張」（位置的 tuple）；否則 None。
    至少一張符合、至少一張不符合（玩家要逐張判斷，不能只挑一張）。
    """
    if not rs:
        return None
    verdicts = [tuple(r[1](c) for c in cands) for r in rs]
    if len(set(verdicts)) != 1:
        return None
    v = verdicts[0]
    if not (1 <= sum(v) <= len(v) - 1):
        return None
    return tuple(i for i, x in enumerate(v) if x)


def make_zendo(spec, seed):
    rnd = random.Random(seed)
    lo, hi = spec["examples"]
    for attempt in range(spec.get("attempts", 3000)):
        pool = [r for r in RULES if spec.get("min_weight", 1) <= r[2] <= spec.get("max_weight", 3)]
        secret = rnd.choice(pool)
        if secret[0] in USED:
            continue
        # 關卡標題教的是哪一種規則（不是／而且／但不是／或），規則就要是那一種
        form = spec.get("form")
        if form == "not" and not secret[0].startswith("不是"):
            continue
        if form == "and" and "而且" not in secret[0]:
            continue
        if form == "but" and "但不是" not in secret[0]:
            continue
        if form == "or" and "或" not in secret[0]:
            continue
        f = secret[1]
        pos = [t for t in ALL_TILES if f(t)]
        neg = [t for t in ALL_TILES if not f(t)]
        if len(pos) < 3 or len(neg) < 6:
            continue
        # 六張候選，符合的有一到三張（玩家不知道有幾張）
        k = rnd.randint(1, 3)
        cands = rnd.sample(pos, k) + rnd.sample(neg, 6 - k)
        rnd.shuffle(cands)
        rest = [t for t in ALL_TILES if t not in cands]
        # 一張一張加範例，直到所有一致的規則對候選的判斷都一樣
        ex = []
        while candidates_ok(consistent_rules(ex), cands) is None:
            rs = consistent_rules(ex)
            want = tuple(f(c) for c in cands)
            wrong = [r for r in rs if tuple(r[1](c) for c in cands) != want]
            useful = [t for t in rest if t not in [e[0] for e in ex] and any(r[1](t) != f(t) for r in wrong)]
            if not useful:
                break
            t = rnd.choice(useful)
            ex.append((t, f(t)))
        if candidates_ok(consistent_rules(ex), cands) is None:
            continue
        # 像數獨挖空：拿掉多餘的範例，剩下的每一張都用得到
        order = list(range(len(ex)))
        rnd.shuffle(order)
        keep = list(ex)
        for j in sorted(order, reverse=True):
            pass
        for t in [ex[j] for j in order]:
            trial = [e for e in keep if e != t]
            if candidates_ok(consistent_rules(trial), cands) is not None:
                keep = trial
        ex = keep
        rnd.shuffle(ex)
        if not (lo <= len(ex) <= hi):
            continue
        npos = sum(1 for _, l in ex if l)
        if npos < 2 or npos > len(ex) - 2:
            continue
        ans = candidates_ok(consistent_rules(ex), cands)
        n_ex = len(ex)
        lies = set()
        spoken = sorted(rnd.sample(range(n_ex), min(spec.get("spoken", 0), n_ex)))
        if spec.get("lies"):
            lies = set(rnd.sample(spoken, spec["lies"]))
            trusting = [(t, (not l) if j in lies else l) for j, (t, l) in enumerate(ex)]
            if candidates_ok(consistent_rules(trusting), cands) == ans:
                continue  # 謊話沒有陷阱
        rs = consistent_rules(ex)
        USED.add(secret[0])
        return dict(secret=secret[0], examples=ex, cands=cands, answer=ans, spoken=spoken, lies=sorted(lies), rules=len(rs))
    raise RuntimeError(f"找不到題目：{spec}")


# ---------------------------------------------------------------- 猜密碼

def feedback(secret, guess):
    black = sum(1 for a, b in zip(secret, guess) if a == b)
    common = sum(min(secret.count(c), guess.count(c)) for c in set(guess))
    return black, common - black


def codes(n_colors, length, dup):
    it = itertools.product(range(n_colors), repeat=length) if dup else itertools.permutations(range(n_colors), length)
    return list(it)


def solve_mm(all_codes, rows, lies_known, hidden_lies):
    """rows: [(guess, (b, w))]。lies_known：看得到臉、是謊話的行。hidden_lies：看不到臉，剛好幾行是假的。"""
    out = []
    for s in all_codes:
        bad = 0
        ok = True
        for j, (g, fb) in enumerate(rows):
            match = feedback(s, g) == fb
            if hidden_lies:
                bad += not match
            elif match == (j in lies_known):
                ok = False
                break
        if hidden_lies and bad != hidden_lies:
            ok = False
        if ok:
            out.append(s)
            if len(out) > 1:
                break
    return out


def make_mm(spec, seed):
    rnd = random.Random(seed)
    nc, L, dup = spec["colors"], spec["length"], spec.get("dup", False)
    all_codes = codes(nc, L, dup)
    lo, hi = spec["rows"]
    k_lies = spec.get("lies", 0) or spec.get("hidden_lies", 0)
    hidden = bool(spec.get("hidden_lies"))
    for attempt in range(spec.get("attempts", 2000)):
        secret = rnd.choice(all_codes)
        rows = []
        # 一行一行加猜測，直到密碼唯一
        while len(solve_mm_none(all_codes, rows)) > 1:
            g = rnd.choice(all_codes)
            if g == secret or any(g == r[0] for r in rows):
                continue
            rows.append((g, feedback(secret, g)))
            if len(rows) > 12:
                break
        if len(solve_mm_none(all_codes, rows)) != 1:
            continue
        # 拿掉多餘的行
        order = list(range(len(rows)))
        rnd.shuffle(order)
        keep = list(rows)
        for r in [rows[j] for j in order]:
            trial = [x for x in keep if x != r]
            if len(solve_mm_none(all_codes, trial)) == 1:
                keep = trial
        rows = keep
        # 加上謊話的行（插在隨機的位置）
        lies = set()
        for _ in range(k_lies):
            while True:
                g = rnd.choice(all_codes)
                if g != secret and all(g != r[0] for r in rows):
                    break
            b, w = feedback(secret, g)
            opts = [(bb, ww) for bb in range(L + 1) for ww in range(L + 1 - bb) if (bb, ww) != (b, w) and (bb, ww) != (L - 1, 1) and bb != L]
            pos = rnd.randrange(len(rows) + 1)
            rows.insert(pos, (g, rnd.choice(opts)))
            lies = {x + (x >= pos) for x in lies} | {pos}
        if not (lo <= len(rows) <= hi):
            continue
        sol = solve_mm(all_codes, rows, set() if hidden else lies, k_lies if hidden else 0)
        if sol != [secret]:
            continue
        if lies and not hidden:
            # 謊話要有陷阱：全部當真的話，沒有答案或答案不同
            if solve_mm_none(all_codes, rows) == [secret]:
                continue
        return dict(secret=list(secret), rows=[(list(g), fb) for g, fb in rows], lies=sorted(lies), hidden=k_lies if hidden else 0)
    raise RuntimeError(f"找不到題目：{spec}")


def solve_mm_none(all_codes, rows):
    return [s for s in all_codes if all(feedback(s, g) == fb for g, fb in rows)][:2]


# ---------------------------------------------------------------- 關卡

Z = lambda **kw: dict(kind="zendo", **kw)
M = lambda **kw: dict(kind="mm", **kw)

SPECS = {
    361: Z(examples=(4, 6), max_weight=1),
    362: Z(examples=(4, 6), max_weight=1),
    363: Z(form="not", examples=(6, 8), min_weight=2, max_weight=2),
    364: Z(form="and", examples=(7, 9), min_weight=2, max_weight=2),
    365: Z(form="but", examples=(7, 9), min_weight=3, max_weight=3),
    366: Z(examples=(6, 8), spoken=3, lies=1, max_weight=2),
    367: Z(form="or", examples=(5, 12), min_weight=3, max_weight=3),
    368: Z(examples=(7, 9), spoken=4, lies=1, min_weight=2, max_weight=3),
    369: Z(examples=(7, 11), spoken=5, lies=2, min_weight=2, max_weight=3),
    370: Z(examples=(8, 13), spoken=4, lies=2, min_weight=3, max_weight=3),
    371: M(colors=4, length=3, rows=(3, 4)),
    372: M(colors=5, length=3, rows=(3, 5)),
    373: M(colors=5, length=4, rows=(4, 6)),
    374: M(colors=6, length=4, rows=(4, 7)),
    375: M(colors=4, length=4, rows=(4, 7), dup=True),
    376: M(colors=5, length=3, rows=(4, 6), lies=1),
    377: M(colors=6, length=4, rows=(5, 8), lies=1),
    378: M(colors=5, length=4, rows=(5, 8), hidden_lies=1),
    379: M(colors=5, length=4, rows=(5, 8), dup=True, lies=1),
    380: M(colors=6, length=4, rows=(6, 9), lies=2),
}

MM_COLORS = ["紅", "藍", "綠", "黃", "紫", "橘"]


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "// 由 tools/s3/rules.py 產生（不用猜、每一個線索都用得到、謊話有陷阱）。請改產生器再重新產生。\n",
        "internal object Ch24Puzzles {\n",
    ]
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        if spec["kind"] == "zendo":
            z = make_zendo(spec, lvl)
            ex = ", ".join(f"Example(Tile({t[0]}, {t[1]}, {t[2]}), {'true' if l else 'false'})" for t, l in z["examples"])
            cd = ", ".join(f"Tile({t[0]}, {t[1]}, {t[2]})" for t in z["cands"])
            kt.append(f"    // 規則：{z['secret']}（和範例一致的規則還有 {z['rules']} 條，但它們對候選的判斷都一樣）\n")
            kt.append(f"    val l{lvl} = RuleQuiz(listOf({ex}), listOf({cd}), answers = setOf({', '.join(map(str, z['answer']))}), spoken = setOf({', '.join(map(str, z['spoken']))}), lies = setOf({', '.join(map(str, z['lies']))}))\n")
            info[lvl] = dict(kind="zendo", secret=z["secret"], answer=[i + 1 for i in z["answer"]], lies=[j + 1 for j in z["lies"]], spoken=[j + 1 for j in z["spoken"]])
        else:
            m = make_mm(spec, lvl)
            rows = ", ".join(f"Guess(listOf({', '.join(map(str, g))}), {b}, {w})" for g, (b, w) in m["rows"])
            kt.append(f"    val l{lvl} = CodeQuiz({spec['colors']}, listOf({', '.join(map(str, m['secret']))}), listOf({rows}), lies = setOf({', '.join(map(str, m['lies']))}), hiddenLies = {m['hidden']}, spoken = {'true' if spec.get('lies') else 'false'}, dup = {'true' if spec.get('dup') else 'false'})\n")
            info[lvl] = dict(kind="mm", secret="".join(MM_COLORS[c] for c in m["secret"]), lies=[j + 1 for j in m["lies"]], rows=len(m["rows"]))
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(os.path.join(os.path.dirname(__file__), "ch24_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
