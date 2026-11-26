#!/usr/bin/env python3
"""
第 25 章〈開關與線路〉的題目產生器與檢查器。

幾個開關（A–E）用看不到的電線接到幾盞燈（1–5）。
- 並聯（OR）：燈接到的開關只要有一個打開，燈就亮。
- 切換（XOR）：燈接到的開關每打開一個，燈就切換一次（開兩個又暗了）。開關數不能多於燈數，不然一定有兩組開關效果一樣。
畫面上是之前做過的幾次實驗（哪些開關打開 → 哪些燈亮）。要推出唯一一組開關，剛好讓「目標」的燈亮、其他燈暗。

每盞燈的接線互相獨立，所以可以逐盞燈列舉所有接法。產生器檢查：
- 不管電線實際怎麼接（只要和實驗一致），答案都是同一組開關，而且沒有別組開關可能剛好成功。
- 每一次實驗都用得到。
- 有些格子要聽 The Button 說（說謊時亮暗相反），把謊話當真會得到錯的答案；
  看不到臉的關卡，只知道所有實驗結果裡剛好一格是假的。

輸出 app/src/main/kotlin/.../levels/s3/Ch25Puzzles.kt 與 tools/s3/ch25_puzzles.json。
"""
import itertools
import json
import os
import random
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_KT = os.path.join(ROOT, "app/src/main/kotlin/tw/dynity/wrongbutton/levels/s3/Ch25Puzzles.kt")


def lamp(config, col, xor):
    return (bin(config & col).count("1") % 2 == 1) if xor else (config & col) != 0


def consistent_cols(n, exps, j, xor):
    """exps: [(config, lampbits)]"""
    return [col for col in range(1 << n) if all(lamp(c, col, xor) == bool(bits >> j & 1) for c, bits in exps)]


def analyze(n, m, exps, target, xor):
    """回傳 (一定成功的開關組, 可能成功的開關組)；某盞燈沒有一致的接法時回傳 None。"""
    cols = [consistent_cols(n, exps, j, xor) for j in range(m)]
    if any(not c for c in cols):
        return None
    sure, maybe = set(), set()
    for c in range(1, 1 << n):
        ok_all = all(all(lamp(c, col, xor) == bool(target >> j & 1) for col in cols[j]) for j in range(m))
        ok_any = all(any(lamp(c, col, xor) == bool(target >> j & 1) for col in cols[j]) for j in range(m))
        if ok_all:
            sure.add(c)
        if ok_any:
            maybe.add(c)
    return sure, maybe


def unique_answer(n, m, exps, target, xor, hidden=0):
    """答案唯一時回傳那組開關，否則 None。hidden：看不到臉，剛好幾格是假的。"""
    if not hidden:
        r = analyze(n, m, exps, target, xor)
        if r is None:
            return None
        sure, maybe = r
        return next(iter(sure)) if len(sure) == 1 and maybe == sure else None
    cells = [(k, j) for k in range(len(exps)) for j in range(m)]
    answer, any_world = None, False
    for bad in itertools.combinations(cells, hidden):
        flipped = list(exps)
        for k, j in bad:
            c, bits = flipped[k]
            flipped[k] = (c, bits ^ (1 << j))
        r = analyze(n, m, flipped, target, xor)
        if r is None:
            continue
        any_world = True
        sure, maybe = r
        if len(sure) != 1 or maybe != sure:
            return None
        a = next(iter(sure))
        if answer is not None and a != answer:
            return None
        answer = a
    return answer if any_world else None


def result(config, wiring, m, xor):
    return sum(1 << j for j in range(m) if lamp(config, wiring[j], xor))


def make(spec, seed):
    rnd = random.Random(seed)
    n, m, xor = spec["n"], spec["m"], spec.get("xor", False)
    lo, hi = spec["exps"]
    for attempt in range(spec.get("attempts", 3000)):
        # 每盞燈接 1–3 個開關
        wiring = []
        for j in range(m):
            k = rnd.randint(1, min(3, n))
            wiring.append(sum(1 << i for i in rnd.sample(range(n), k)))
        if len(set(wiring)) < m:
            continue
        answer = rnd.randrange(1, 1 << n)
        if bin(answer).count("1") < spec.get("min_on", 1):
            continue
        target = result(answer, wiring, m, xor)
        if target == 0 or target == (1 << m) - 1 and not spec.get("all_on"):
            continue
        # 只有這一組開關能剛好成功（以真實接線來說）
        if [c for c in range(1, 1 << n) if result(c, wiring, m, xor) == target] != [answer]:
            continue
        exps = []
        tried = set([answer, 0])
        while unique_answer(n, m, exps, target, xor) is None and len(tried) < (1 << n):
            c = rnd.randrange(1, 1 << n)
            if c in tried:
                continue
            tried.add(c)
            exps.append((c, result(c, wiring, m, xor)))
        if unique_answer(n, m, exps, target, xor) != answer:
            continue
        order = list(range(len(exps)))
        rnd.shuffle(order)
        keep = list(exps)
        for e in [exps[i] for i in order]:
            trial = [x for x in keep if x != e]
            if unique_answer(n, m, trial, target, xor) == answer:
                keep = trial
        exps = keep
        # 每次實驗至少開兩個開關（只開一個太直接）
        if spec.get("multi", True) and any(bin(c).count("1") < 2 for c, _ in exps) and len(exps) > 2:
            continue
        lies, spoken = set(), set()
        hidden = spec.get("hidden", 0)
        if hidden:
            # 有一格是假的：要多做幾次實驗才推得出來。繼續加實驗，直到「剛好一格假」的推理也唯一。
            shown = list(exps)
            k0 = rnd.randrange(len(shown))
            j0 = rnd.randrange(m)
            c0, b0 = shown[k0]
            shown[k0] = (c0, b0 ^ (1 << j0))
            while unique_answer(n, m, shown, target, xor, 1) != answer and len(tried) < (1 << n):
                c = rnd.randrange(1, 1 << n)
                if c in tried:
                    continue
                tried.add(c)
                shown.append((c, result(c, wiring, m, xor)))
            if unique_answer(n, m, shown, target, xor, 1) != answer:
                continue
            # 拿掉多餘的實驗（假的那一格所在的實驗留著）
            bad_exp = shown[k0]
            for e in list(shown):
                if e == bad_exp:
                    continue
                trial = [x for x in shown if x != e]
                if unique_answer(n, m, trial, target, xor, 1) == answer:
                    shown = trial
            rnd.shuffle(shown)
            k0 = shown.index(bad_exp)
            lies = {k0 * m + j0}
            exps_out = shown
        else:
            exps_out = list(exps)
            if spec.get("spoken"):
                cells = list(range(len(exps) * m))
                spoken = set(rnd.sample(cells, min(spec["spoken"], len(cells))))
                lies = set(rnd.sample(sorted(spoken), spec.get("lies", 0)))
                said = list(exps)
                for cell in lies:
                    k, j = divmod(cell, m)
                    c, bits = said[k]
                    said[k] = (c, bits ^ (1 << j))
                if lies and unique_answer(n, m, said, target, xor) == answer:
                    continue  # 謊話沒有陷阱
                exps_out = said
        if not (lo <= len(exps_out) <= hi):
            continue
        return dict(wiring=wiring, answer=answer, target=target, exps=exps_out, lies=sorted(lies), spoken=sorted(spoken), hidden=hidden)
    raise RuntimeError(f"找不到題目：{spec}")


def W(n, m, exps, **kw):
    return dict(n=n, m=m, exps=exps, **kw)


SPECS = {
    381: W(3, 3, (2, 3)),
    382: W(3, 3, (2, 4)),
    383: W(4, 3, (3, 4)),
    384: W(4, 4, (3, 5), min_on=2),
    385: W(4, 4, (3, 5), spoken=4, lies=1, min_on=2),
    386: W(3, 3, (2, 4), xor=True, min_on=2),
    387: W(3, 4, (3, 4), xor=True, min_on=2),
    388: W(4, 4, (3, 5), xor=True, min_on=2),
    389: W(5, 4, (4, 6), min_on=2),
    390: W(4, 4, (3, 5), spoken=5, lies=2, min_on=2),
    391: W(4, 4, (3, 5), xor=True, spoken=4, lies=1, min_on=2),
    392: W(5, 5, (4, 6), min_on=2),
    393: W(4, 5, (4, 6), xor=True, min_on=2),
    394: W(4, 4, (3, 6), hidden=1, min_on=2),
    395: W(4, 4, (4, 6), xor=True, hidden=1, min_on=2),
    396: W(5, 5, (4, 7), spoken=6, lies=2, min_on=2),
    397: W(5, 5, (4, 7), xor=True, min_on=2),
    398: W(5, 5, (4, 7), xor=True, spoken=6, lies=2, min_on=2),
    399: W(5, 5, (4, 7), hidden=1, min_on=2),
    400: W(5, 5, (5, 8), xor=True, hidden=1, min_on=2),
}


def main(only=None):
    kt = [
        "package tw.dynity.wrongbutton.levels.s3\n\n",
        "// 由 tools/s3/wires.py 產生（不管電線怎麼接答案都一樣、每次實驗都用得到、謊話有陷阱）。請改產生器再重新產生。\n",
        "internal object Ch25Puzzles {\n",
    ]
    info = {}
    for lvl, spec in SPECS.items():
        if only and lvl not in only:
            continue
        p = make(spec, lvl)
        ex = ", ".join(f"Trial({c}, {b})" for c, b in p["exps"])
        wiring = ", ".join(map(str, p["wiring"]))
        kt.append(
            f"    val l{lvl} = WireQuiz({spec['n']}, {spec['m']}, listOf({ex}), target = {p['target']}, answer = {p['answer']}, "
            f"xor = {'true' if spec.get('xor') else 'false'}, spoken = setOf({', '.join(map(str, p['spoken']))}), "
            f"lies = setOf({', '.join(map(str, p['lies']))}), hiddenLies = {p['hidden']}, wiring = listOf({wiring}))\n"
        )
        info[lvl] = dict(answer="".join("ABCDE"[i] for i in range(spec["n"]) if p["answer"] >> i & 1),
                         target=[j + 1 for j in range(spec["m"]) if p["target"] >> j & 1], exps=len(p["exps"]),
                         lies=[f"實驗 {c // spec['m'] + 1} 的燈 {c % spec['m'] + 1}" for c in p["lies"]])
        print(lvl, info[lvl], file=sys.stderr)
    kt.append("}\n")
    if not only:
        with open(OUT_KT, "w") as f:
            f.write("".join(kt))
        with open(os.path.join(os.path.dirname(__file__), "ch25_puzzles.json"), "w") as f:
            json.dump(info, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main({int(a) for a in sys.argv[1:]} or None)
