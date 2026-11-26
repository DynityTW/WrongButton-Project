#!/usr/bin/env python3
"""
第 28 章〈密碼規則〉的題目產生器與檢查器。

「請設定密碼」：幾位數字，加上一串規則（數字加起來、倍數、哪一位比哪一位大、包含／不包含……）。
設定出唯一符合所有規則的密碼。規則旁邊**不會**即時打勾，所以不能用試的。

暴力列舉所有密碼（最多 10 萬組），檢查：
- 答案唯一，每一條規則都用得到。
- 只看一半的規則，可能的密碼還很多。
- 有些規則要聽 The Button 說（說謊時規則是假的），把謊話當真會設錯；有些關卡看不到臉，只知道剛好幾條是假的。

輸出 app/src/main/kotlin/.../levels/s3/Ch28Puzzles.kt、app/src/main/res/values/strings_ch28_puzzles.xml、tools/s3/ch28_puzzles.json。
"""
import itertools
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch28Puzzles.kt")
OUT_XML = os.path.join(ROOT, "app/src/main/res/values/strings_ch28_puzzles.xml")
ORD = ["第一", "第二", "第三", "第四", "第五"]


def value(p):
    v = 0
    for d in p:
        v = v * 10 + d
    return v


def is_prime(v):
    if v < 2:
        return False
    i = 2
    while i * i <= v:
        if v % i == 0:
            return False
        i += 1
    return True


def rule(rnd, L, kinds, target):
    kind = rnd.choice(kinds)
    i, j = rnd.sample(range(L), 2)
    d = rnd.randrange(10)
    if kind == "distinct":
        return "每一位數字都不一樣。", lambda p: len(set(p)) == L
    if kind == "sum":
        s = sum(target) + rnd.choice([0, 0, rnd.randint(-3, 3)])
        return f"所有數字加起來是 {s}。", lambda p, s=s: sum(p) == s
    if kind == "sumlt":
        s = rnd.randint(5, 9 * L - 5)
        return f"所有數字加起來小於 {s}。", lambda p, s=s: sum(p) < s
    if kind == "gt":
        return f"{ORD[i]}位比{ORD[j]}位大。", lambda p, i=i, j=j: p[i] > p[j]
    if kind == "diff":
        k = rnd.randint(1, 5)
        return f"{ORD[i]}位比{ORD[j]}位大 {k}。", lambda p, i=i, j=j, k=k: p[i] - p[j] == k
    if kind == "has":
        return f"密碼裡有 {d}。", lambda p, d=d: d in p
    if kind == "hasnot":
        return f"密碼裡沒有 {d}。", lambda p, d=d: d not in p
    if kind == "even":
        return f"{ORD[i]}位是偶數。", lambda p, i=i: p[i] % 2 == 0
    if kind == "odd":
        return f"{ORD[i]}位是奇數。", lambda p, i=i: p[i] % 2 == 1
    if kind == "mult":
        k = rnd.choice([3, 4, 6, 7, 9, 11])
        return f"整個密碼是 {k} 的倍數。", lambda p, k=k: value(p) % k == 0
    if kind == "evens":
        k = rnd.randint(0, L)
        text = "沒有任何一位是偶數。" if k == 0 else "每一位都是偶數。" if k == L else f"剛好有 {k} 位是偶數。"
        return text, lambda p, k=k: sum(1 for x in p if x % 2 == 0) == k
    if kind == "asc":
        return "數字由左到右越來越大。", lambda p: all(a < b for a, b in zip(p, p[1:]))
    if kind == "noadj":
        return "相鄰的兩位數字都不一樣。", lambda p: all(a != b for a, b in zip(p, p[1:]))
    if kind == "first_nonzero":
        return "第一位不是 0。", lambda p: p[0] != 0
    if kind == "prod":
        prod = 1
        for x in target:
            prod *= x
        return f"所有數字相乘是 {prod}。", lambda p, prod=prod: eval_prod(p) == prod
    if kind == "prime":
        return "整個密碼是質數。", lambda p: is_prime(value(p))
    if kind == "max":
        k = rnd.randint(3, 9)
        return f"最大的數字是 {k}。", lambda p, k=k: max(p) == k
    if kind == "pos":
        return f"{ORD[i]}位是 {d}。", lambda p, i=i, d=d: p[i] == d
    if kind == "sumij":
        s = target[i] + target[j]
        return f"{ORD[i]}位加{ORD[j]}位是 {s}。", lambda p, i=i, j=j, s=s: p[i] + p[j] == s
    if kind == "palin":
        return "密碼倒過來念也一樣。", lambda p: list(p) == list(p)[::-1]
    raise ValueError(kind)


def eval_prod(p):
    r = 1
    for x in p:
        r *= x
    return r


def all_pins(L):
    return list(itertools.product(range(10), repeat=L))


class Space:
    """所有密碼，加上每條規則的位元遮罩（第 i 位＝第 i 個密碼符合）。用位元運算，五位數也很快。"""

    def __init__(self, L):
        self.pins = all_pins(L)
        self.full = (1 << len(self.pins)) - 1
        self.cache = {}

    def mask(self, r):
        text, f = r
        if text not in self.cache:
            m = 0
            for i, p in enumerate(self.pins):
                if f(p):
                    m |= 1 << i
            self.cache[text] = m
        return self.cache[text]

    def solve(self, rules, lies=(), one_false=False):
        ms = [self.mask(r) for r in rules]
        if one_false:
            out = 0
            for j in range(len(ms)):
                m = self.full & ~ms[j]
                for i, x in enumerate(ms):
                    if i != j:
                        m &= x
                out |= m
            return out
        m = self.full
        for j, x in enumerate(ms):
            m &= (self.full & ~x) if j in lies else x
        return m

    def pins_of(self, m, limit=2):
        out = []
        while m and len(out) < limit:
            low = m & -m
            out.append(self.pins[low.bit_length() - 1])
            m ^= low
        return out


SPACES = {}


def make(spec, seed):
    rnd = random.Random(seed)
    L = spec["L"]
    sp = SPACES.setdefault(L, Space(L))
    lo, hi = spec["rules"]
    for attempt in range(spec.get("attempts", 300)):
        target = tuple(rnd.randrange(10) for _ in range(L))
        rules = []
        tries = 0
        while bin(sp.solve(rules)).count("1") != 1 and tries < 300:
            tries += 1
            r = rule(rnd, L, spec["kinds"], target)
            if not r[1](target) or any(r[0] == x[0] for x in rules):
                continue
            rules.append(r)
        if bin(sp.solve(rules)).count("1") != 1:
            continue
        # 拿掉多餘的規則
        order = list(range(len(rules)))
        rnd.shuffle(order)
        keep = list(rules)
        for r in [rules[i] for i in order]:
            trial = [x for x in keep if x is not r]
            if bin(sp.solve(trial)).count("1") == 1:
                keep = trial
        rules = keep
        if sum(1 for t, _ in rules if "位是 " in t) > spec.get("max_direct", 0):
            continue
        if sum(1 for t, _ in rules if t.startswith("密碼裡沒有")) > 2:
            continue
        lies, spoken = set(), []
        k = spec.get("lies", 0) or spec.get("hidden", 0)
        for _ in range(k):
            for _t in range(300):
                r = rule(rnd, L, spec["kinds"], target)
                if not r[1](target) and all(r[0] != x[0] for x in rules):
                    break
            pos = rnd.randrange(len(rules) + 1)
            rules.insert(pos, r)
            lies = {x + (x >= pos) for x in lies} | {pos}
        if spec.get("hidden"):
            if sp.pins_of(sp.solve(rules, one_false=True)) != [target]:
                continue
        else:
            if sp.pins_of(sp.solve(rules, lies)) != [target]:
                continue
            if lies and sp.pins_of(sp.solve(rules)) == [target]:
                continue  # 謊話沒有陷阱
            if lies:
                spoken = sorted(set(rnd.sample(range(len(rules)), min(len(rules), spec.get("spoken", len(rules))))) | lies)
        if not (lo <= len(rules) <= hi):
            continue
        truthful = [r for j, r in enumerate(rules) if j not in lies]
        half = rnd.sample(truthful, max(1, len(truthful) // 2))
        if bin(sp.solve(half)).count("1") < spec.get("min_half", 10):
            continue
        return dict(target=target, rules=rules, lies=sorted(lies), spoken=spoken, hidden=spec.get("hidden", 0))
    raise RuntimeError(f"找不到題目：{spec}")


A = ["distinct", "sum", "gt", "has", "hasnot", "even", "odd", "noadj", "first_nonzero", "sumlt"]
B = A + ["diff", "mult", "evens", "max", "sumij"]
C = B + ["prod", "prime", "asc", "palin"]


def P(L, rules, kinds, **kw):
    return dict(L=L, rules=rules, kinds=kinds, **kw)


SPECS = {
    441: P(3, (3, 5), A, min_half=20),
    442: P(3, (3, 6), A, min_half=30),
    443: P(3, (4, 6), B, min_half=30),
    444: P(3, (4, 7), B, lies=1, min_half=30),
    445: P(3, (4, 7), C, min_half=40),
    446: P(4, (4, 7), A, min_half=100),
    447: P(4, (5, 8), B, min_half=150),
    448: P(4, (5, 8), B, lies=1, spoken=4, min_half=150),
    449: P(4, (5, 8), C, min_half=150),
    450: P(4, (5, 9), B, hidden=1, min_half=150),
    451: P(4, (5, 9), C, lies=2, spoken=5, min_half=150),
    452: P(4, (6, 9), C, min_half=300),
    453: P(4, (6, 9), C, lies=1, spoken=5, min_half=300),
    454: P(4, (6, 10), C, hidden=1, min_half=300),
    455: P(4, (6, 10), C, lies=2, spoken=6, min_half=300),
    456: P(5, (6, 10), B, min_half=400, attempts=300),
    457: P(5, (6, 10), C, lies=1, spoken=5, min_half=400, attempts=300),
    458: P(5, (6, 11), C, hidden=1, min_half=400, attempts=300),
    459: P(5, (7, 11), C, lies=2, spoken=6, min_half=400, attempts=300),
    460: P(5, (7, 12), C, lies=2, spoken=7, min_half=600, attempts=300),
}


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "import tw.dynity.wrongbutton.R\n\n",
        "// 由 tools/s3/pins.py 產生（答案唯一、每一條規則都用得到、謊話有陷阱）。請改產生器再重新產生。\n",
        "internal object Ch28Puzzles {\n",
    ]
    xml = ['<?xml version="1.0" encoding="utf-8"?>\n<!-- 由 tools/s3/pins.py 產生。台灣繁體中文。 -->\n<resources>\n']
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        p = make(spec, lvl)
        for j, (t, _) in enumerate(p["rules"]):
            xml.append(f'    <string name="l{lvl}_r{j}">{t}</string>\n')
        rl = ", ".join(f"R.string.l{lvl}_r{j}" for j in range(len(p["rules"])))
        kt.append(
            f"    val l{lvl} = PinQuiz(listOf({', '.join(map(str, p['target']))}), listOf({rl}), "
            f"spoken = setOf({', '.join(map(str, p['spoken']))}), lies = setOf({', '.join(map(str, p['lies']))}), hiddenLies = {p['hidden']})\n"
        )
        info[lvl] = dict(answer="".join(map(str, p["target"])), lies=[j + 1 for j in p["lies"]], rules=[t for t, _ in p["rules"]])
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    xml.append("</resources>\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(OUT_XML, "w") as f:
            f.write("".join(xml))
        with open(os.path.join(os.path.dirname(__file__), "ch28_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
