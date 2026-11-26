package tw.dynity.wrongbutton.levels.s3

import tw.dynity.wrongbutton.R

// 由 tools/s3/finale.py 產生（借用第 21–29 章的產生器，檢查都照舊）。請改產生器再重新產生。
internal object Ch30Puzzles {
    // ---- 第 481 關：mines ----
    // 第 481 關：8×8、13 顆雷；推理 24 步，其中 6 步要把幾個數字放在一起想。第一步：C3 安全。
    val l481 = MineBoard.parse(
        "...ooo*.",
        ".**ooo..",
        "R..ooo**",
        ".*..*...",
        "...L..*.",
        ".....R.*",
        "*..*....",
        "LR...**.",
        reported = mapOf(16 to 2, 35 to 2, 45 to 1, 56 to 0, 57 to 1), counter = true,
    )
    // ---- 第 482 關：captcha ----
    val l482 = listOf(
        Captcha(4, listOf(Tile(1, 2, 3), Tile(2, 0, 1), Tile(1, 3, 3), Tile(1, 3, 2), Tile(0, 0, 2), Tile(1, 1, 3), Tile(1, 3, 3), Tile(1, 2, 3), Tile(1, 2, 3), Tile(3, 3, 2), Tile(3, 3, 3), Tile(1, 0, 1), Tile(3, 2, 1), Tile(3, 3, 1), Tile(0, 3, 1), Tile(0, 2, 1)), listOf(R.string.l482_r0_s0, R.string.l482_r0_s1, R.string.l482_r0_s2, R.string.l482_r0_s3, R.string.l482_r0_s4), answer = setOf(3, 4, 9, 14), spoken = true, lies = setOf(0, 3), subtle = true),
    )
    // ---- 第 483 關：liars ----
    val l483 = LiarPuzzle(6, listOf(Said(0, R.string.l483_s0), Said(1, R.string.l483_s1), Said(2, R.string.l483_s2), Said(3, R.string.l483_s3), Said(4, R.string.l483_s4), Said(5, R.string.l483_s5)), answer = setOf(0, 1, 2, 3, 5), goal = LiarGoal.Liars, roles = "LLLLTL", narrator = R.string.l483_narrator, narratorTrue = false)
    // ---- 第 484 關：rules ----
    // 規則：三角形但不是紅色（和範例一致的規則還有 4 條，但它們對候選的判斷都一樣）
    val l484 = RuleQuiz(listOf(Example(Tile(1, 3, 2), false), Example(Tile(2, 0, 3), false), Example(Tile(3, 0, 3), false), Example(Tile(1, 3, 1), false), Example(Tile(2, 2, 3), true), Example(Tile(3, 2, 2), true), Example(Tile(1, 1, 3), false), Example(Tile(0, 3, 3), false)), listOf(Tile(2, 2, 2), Tile(2, 1, 1), Tile(3, 3, 2), Tile(0, 3, 2)), answer = 0, spoken = setOf(0, 1, 3, 6, 7), lies = setOf(6, 7))
    // ---- 第 485 關：rules ----
    val l485 = CodeQuiz(6, listOf(2, 0, 4, 4), listOf(Guess(listOf(5, 0, 1, 3), 2, 2), Guess(listOf(5, 3, 2, 4), 1, 1), Guess(listOf(0, 5, 0, 1), 0, 1), Guess(listOf(3, 4, 0, 2), 0, 3), Guess(listOf(5, 2, 3, 0), 0, 2), Guess(listOf(1, 0, 4, 0), 0, 3)), lies = setOf(0, 5), hiddenLies = 0, spoken = true, dup = true)
    // ---- 第 486 關：wires ----
    val l486 = WireQuiz(5, 5, listOf(Trial(3, 23), Trial(19, 27), Trial(25, 20), Trial(26, 7), Trial(12, 30), Trial(20, 26)), target = 5, answer = 31, xor = true, spoken = setOf(), lies = setOf(2), hiddenLies = 1, wiring = listOf(2, 6, 8, 24, 5))
    // ---- 第 487 關：order ----
    val l487 = OrderQuiz(5, listOf(R.string.l487_c0, R.string.l487_c1, R.string.l487_c2, R.string.l487_c3, R.string.l487_c4, R.string.l487_c5, R.string.l487_c6, R.string.l487_c7, R.string.l487_c8, R.string.l487_c9), colors = listOf(2, 1, 0, 3, 4), drinks = listOf(3, 2, 1, 4, 0), spoken = setOf(), lies = setOf(3), hiddenLies = 1)
    // ---- 第 488 關：maze ----
    // 路線：A1 → E1 → E6 → F6 → F7 → F2 → F5 → F1 → D1 → B1 → C1 → C5 → C7 → C3 → E3 → E5（走得到 41 格）
    val l488 = MazeQuiz(7, listOf("4", "R", "4", "2", "5", "2", "L", "4", "5", "L", "R", "U", "D", "U", "U", "L", "2", "U", "U", "4", "D", "1", "D", "R", "U", "5", "D", "3", "1", "D", "D", "1", "G", "4", "R", "U", "D", "R", "2", "L", "D", "D", "D", "3", "4", "1", "D", "5", "R"), goal = 32, steps = 15, spoken = setOf(10, 18, 20, 34, 36, 39), lies = setOf(18, 39), path = listOf(0, 4, 39, 40, 47, 12, 33, 5, 3, 1, 2, 30, 44, 16, 18, 32))
    // ---- 第 489 關：pins ----
    val l489 = PinQuiz(listOf(0, 0, 4, 6, 7), listOf(R.string.l489_r0, R.string.l489_r1, R.string.l489_r2, R.string.l489_r3, R.string.l489_r4, R.string.l489_r5, R.string.l489_r6, R.string.l489_r7, R.string.l489_r8), spoken = setOf(), lies = setOf(2), hiddenLies = 1)
    // ---- 第 490 關：mirrors ----
    // 光線經過 19 格、轉 15 次彎。
    val l490 = MirrorQuiz(7, listOf("R", "R", "R", ".", ".", ".", ".", "R", "R", "R", "R", ".", ".", ".", "\\", "R", "R", ".", ".", ".", ".", ".", ".", "/", "/", ".", ".", "\\", "/", ".", ".", ".", ".", ".", ".", ".", "\\", "\\", ".", "\\", "/", "\\", "\\", ".", "/", "/", ".", ".", "/"), row = 0, exitSide = 1, exitPos = 0, answer = listOf("\\", "/", "\\", "\\", "/", "\\", "\\", "/", "\\"), spoken = setOf(23, 24, 27, 28, 39, 42), lies = setOf(23, 42))
    // ---- 第 491 關：mines ----
    // 第 491 關：8×8、14 顆雷；推理 19 步，其中 11 步要把幾個數字放在一起想。第一步：D1 安全。
    val l491 = MineBoard.parse(
        "....o...",
        "..*....L",
        "L.oo.*..",
        ".*R*.o*o",
        ".R***.R*",
        "o*.oo.oo",
        "*o.**o.o",
        ".......*",
        reported = mapOf(15 to 2, 16 to 2, 26 to 4, 33 to 3, 38 to 2), mode = MineMode.Flag, counter = true,
    )
    // ---- 第 492 關：captcha ----
    val l492 = listOf(
        Captcha(4, listOf(Tile(1, 2, 2), Tile(3, 3, 3), Tile(1, 0, 1), Tile(3, 2, 3), Tile(1, 2, 2), Tile(1, 1, 2), Tile(1, 0, 1), Tile(1, 2, 3), Tile(2, 0, 2), Tile(3, 3, 1), Tile(1, 3, 2), Tile(0, 1, 3), Tile(1, 0, 3), Tile(1, 2, 2), Tile(3, 0, 3), Tile(0, 1, 2)), listOf(R.string.l492_r0_s0, R.string.l492_r0_s1, R.string.l492_r0_s2, R.string.l492_r0_s3, R.string.l492_r0_s4), answer = setOf(0, 3, 4), exactlyTrue = 2),
        Captcha(4, listOf(Tile(2, 3, 2), Tile(0, 3, 2), Tile(2, 3, 1), Tile(3, 1, 3), Tile(0, 3, 3), Tile(3, 3, 1), Tile(2, 2, 1), Tile(1, 0, 2), Tile(3, 0, 2), Tile(1, 1, 3), Tile(2, 1, 1), Tile(0, 1, 1), Tile(2, 3, 1), Tile(0, 1, 1), Tile(3, 2, 3), Tile(3, 2, 2)), listOf(R.string.l492_r1_s0, R.string.l492_r1_s1, R.string.l492_r1_s2, R.string.l492_r1_s3, R.string.l492_r1_s4), answer = setOf(2, 5, 9), spoken = true, lies = setOf(0, 3), subtle = true),
    )
    // ---- 第 493 關：liars ----
    val l493 = LiarPuzzle(5, listOf(Said(0, R.string.l493_s0), Said(0, R.string.l493_s1), Said(1, R.string.l493_s2), Said(1, R.string.l493_s3), Said(2, R.string.l493_s4), Said(2, R.string.l493_s5), Said(3, R.string.l493_s6), Said(3, R.string.l493_s7), Said(4, R.string.l493_s8), Said(4, R.string.l493_s9)), answer = setOf(2), goal = LiarGoal.Spy, roles = "TLSTT", spy = true)
    // ---- 第 494 關：rules ----
    // 規則：剛好 3 個圖形但不是三角形（和範例一致的規則還有 5 條，但它們對候選的判斷都一樣）
    val l494 = RuleQuiz(listOf(Example(Tile(2, 0, 2), false), Example(Tile(3, 1, 3), true), Example(Tile(0, 1, 1), false), Example(Tile(0, 3, 1), false), Example(Tile(1, 2, 2), false), Example(Tile(0, 3, 3), true), Example(Tile(3, 0, 1), false)), listOf(Tile(1, 2, 1), Tile(1, 1, 1), Tile(3, 3, 2), Tile(0, 1, 3)), answer = 3, spoken = setOf(1, 2, 3, 5, 6), lies = setOf(2, 6))
    // ---- 第 495 關：rules ----
    val l495 = CodeQuiz(6, listOf(2, 1, 0, 5), listOf(Guess(listOf(3, 4, 1, 2), 0, 2), Guess(listOf(1, 0, 5, 3), 0, 3), Guess(listOf(5, 2, 0, 1), 1, 0), Guess(listOf(4, 3, 2, 1), 0, 2), Guess(listOf(0, 4, 2, 5), 1, 2), Guess(listOf(4, 2, 3, 0), 0, 2)), lies = setOf(2), hiddenLies = 1, spoken = false, dup = false)
    // ---- 第 496 關：wires ----
    val l496 = WireQuiz(5, 5, listOf(Trial(16, 24), Trial(28, 30), Trial(9, 13), Trial(15, 23), Trial(18, 31), Trial(4, 18)), target = 28, answer = 24, xor = false, spoken = setOf(), lies = setOf(13), hiddenLies = 1, wiring = listOf(3, 6, 10, 16, 22))
    // ---- 第 497 關：order ----
    val l497 = OrderQuiz(6, listOf(R.string.l497_c0, R.string.l497_c1, R.string.l497_c2, R.string.l497_c3, R.string.l497_c4, R.string.l497_c5, R.string.l497_c6, R.string.l497_c7, R.string.l497_c8), colors = listOf(3, 1, 4, 5, 2, 0), drinks = null, spoken = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8), lies = setOf(4, 8), hiddenLies = 0)
    // ---- 第 498 關：maze ----
    // 路線：A1 → B1 → B6 → F6 → F3 → G3 → C3 → C4 → E4 → E2 → E7 → D7（走得到 45 格）
    val l498 = MazeQuiz(7, listOf("R", "D", "R", "R", "U", "U", "R", "D", "L", "L", "D", "U", "D", "D", "L", "U", "D", "L", "R", "R", "L", "L", "D", "R", "L", "U", "D", "L", "D", "R", "R", "L", "L", "R", "L", "U", "L", "D", "L", "L", "U", "R", "U", "L", "D", "G", "L", "U", "D"), goal = 45, steps = 11, spoken = setOf(10, 11, 19, 36, 39, 42), lies = setOf(11, 36), path = listOf(0, 1, 36, 40, 19, 20, 16, 23, 25, 11, 46, 45))
    // ---- 第 499 關：pins ----
    val l499 = PinQuiz(listOf(3, 6, 2, 5, 1), listOf(R.string.l499_r0, R.string.l499_r1, R.string.l499_r2, R.string.l499_r3, R.string.l499_r4, R.string.l499_r5, R.string.l499_r6, R.string.l499_r7), spoken = setOf(0, 1, 2, 3, 4, 5, 6, 7), lies = setOf(2, 4), hiddenLies = 0)
    // ---- 第 500 關：liars ----
    val l500 = LiarPuzzle(6, listOf(Said(0, R.string.l500_s0), Said(1, R.string.l500_s1), Said(2, R.string.l500_s2), Said(3, R.string.l500_s3), Said(4, R.string.l500_s4), Said(5, R.string.l500_s5)), answer = setOf(1), goal = LiarGoal.Button, roles = "TLSLTT", button = 1, spy = true, narrator = R.string.l500_narrator, narratorTrue = false)
}
