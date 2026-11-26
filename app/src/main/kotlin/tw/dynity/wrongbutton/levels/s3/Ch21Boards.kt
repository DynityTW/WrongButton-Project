package tw.dynity.wrongbutton.levels.s3

// 由 tools/s3/mines.py 產生（每個盤面都檢查過：不用猜、謊話有陷阱）。請改產生器再重新產生。
// 圖例：* 地雷、. 沒翻開的安全格、o 一開始就翻開、R The Button 說的真話、L The Button 說的謊話。
internal object Ch21Boards {
    // 第 301 關：4×4、2 顆雷；推理 2 步，其中 0 步要把幾個數字放在一起想。第一步：B3 安全。
    val l301 = MineBoard.parse(
        "oooo",
        "oooo",
        "*..*",
        "....",
        reported = mapOf(),
    )
    // 第 302 關：5×5、4 顆雷；推理 7 步，其中 0 步要把幾個數字放在一起想。第一步：A3 安全。
    val l302 = MineBoard.parse(
        ".oooo",
        "*oooo",
        "...*.",
        ".**..",
        ".....",
        reported = mapOf(),
    )
    // 第 303 關：5×5、4 顆雷；推理 4 步，其中 0 步要把幾個數字放在一起想。第一步：C1 安全。
    val l303 = MineBoard.parse(
        "*o.o.",
        "oo...",
        "o.*.o",
        ".*o..",
        ".o.*.",
        reported = mapOf(), mode = MineMode.Flag, counter = true,
    )
    // 第 304 關：6×6、7 顆雷；推理 14 步，其中 3 步要把幾個數字放在一起想。第一步：D3 安全。
    val l304 = MineBoard.parse(
        "..*ooo",
        "...ooo",
        ".*..*.",
        ".*....",
        ".**.*.",
        "......",
        reported = mapOf(),
    )
    // 第 305 關：6×6、7 顆雷；推理 13 步，其中 3 步要把幾個數字放在一起想。第一步：C4 安全。
    val l305 = MineBoard.parse(
        "oooo*.",
        "oooo..",
        "ooo*..",
        ".*.**.",
        "......",
        "..**..",
        reported = mapOf(), counter = true,
    )
    // 第 306 關：6×6、6 顆雷；推理 5 步，其中 0 步要把幾個數字放在一起想。第一步：C1 安全。
    val l306 = MineBoard.parse(
        ".*....",
        "oo....",
        "oo.*.*",
        "oo*..*",
        "ooL...",
        "*R....",
        reported = mapOf(26 to 0, 31 to 1),
    )
    // 第 307 關：6×6、7 顆雷；推理 11 步，其中 2 步要把幾個數字放在一起想。第一步：B3 安全。
    val l307 = MineBoard.parse(
        "**ooo.",
        "R.ooo*",
        "L.ooo.",
        "..*...",
        "*.*..*",
        ".R....",
        reported = mapOf(6 to 2, 12 to 1, 31 to 2),
    )
    // 第 308 關：6×6、9 顆雷；推理 5 步，其中 2 步要把幾個數字放在一起想。第一步：E4 安全。
    val l308 = MineBoard.parse(
        "..*o**",
        ".o*...",
        "......",
        "o*...*",
        "....o.",
        "..*o**",
        reported = mapOf(), rule = MineRule.Cross,
    )
    // 第 309 關：6×6、8 顆雷；推理 9 步，其中 4 步要把幾個數字放在一起想。第一步：A4 安全。
    val l309 = MineBoard.parse(
        "*.ooo.",
        "o.*..*",
        ".o.oo*",
        "..o*.*",
        ".o.*.*",
        "...o.o",
        reported = mapOf(), mode = MineMode.Flag, counter = true,
    )
    // 第 310 關：7×7、9 顆雷；推理 11 步，其中 2 步要把幾個數字放在一起想。第一步：C2 安全。
    val l310 = MineBoard.parse(
        "......*",
        ".*.*.RL",
        "ooo....",
        "ooo*...",
        "ooo....",
        ".***.*.",
        "..L..*.",
        reported = mapOf(12 to 1, 13 to 2, 44 to 4), cover = tw.dynity.wrongbutton.core.character.FaceCover.Eyes,
    )
    // 第 311 關：6×6、8 顆雷；推理 5 步，其中 2 步要把幾個數字放在一起想。第一步：B1 安全。
    val l311 = MineBoard.parse(
        "..o..*",
        "o...o.",
        "*.o.*o",
        "o.o.oo",
        "o*o.**",
        ".o*o.*",
        reported = mapOf(), rule = MineRule.Cross, mode = MineMode.Flag, counter = true,
    )
    // 第 312 關：7×7、10 顆雷；推理 9 步，其中 2 步要把幾個數字放在一起想。第一步：E5 安全。
    val l312 = MineBoard.parse(
        "....**.",
        "...*...",
        "*...**.",
        ".......",
        "*..*...",
        "..LLRR.",
        ".*ooRo*",
        reported = mapOf(37 to 4, 38 to 2, 39 to 1, 40 to 1, 46 to 0),
    )
    // 第 313 關：7×7、9 顆雷；推理 13 步，其中 3 步要把幾個數字放在一起想。第一步：C4 安全。
    val l313 = MineBoard.parse(
        ".....R.",
        "....*.R",
        "..*.*.*",
        "L*.*.*.",
        "oooo.*.",
        "oooo...",
        "oooo*..",
        reported = mapOf(5 to 1, 13 to 1, 21 to 0), cover = tw.dynity.wrongbutton.core.character.FaceCover.Mouth,
    )
    // 第 314 關：7×7、9 顆雷；推理 13 步，其中 4 步要把幾個數字放在一起想。第一步：E3 安全。
    val l314 = MineBoard.parse(
        ".*..R*.",
        "......*",
        "...*.oo",
        "*..**oo",
        "..LRooo",
        "R.**ooo",
        "....ooo",
        reported = mapOf(4 to 1, 30 to 5, 31 to 4, 35 to 0), hiddenLies = 1,
    )
    // 第 315 關：7×7、10 顆雷；推理 13 步，其中 4 步要把幾個數字放在一起想。第一步：C5 安全。
    val l315 = MineBoard.parse(
        ".....*.",
        ".**....",
        "oo*...*",
        "oo.....",
        "oo....*",
        "*.*....",
        "*.....*",
        reported = mapOf(), counter = true,
    )
    // 第 316 關：7×7、13 顆雷；推理 9 步，其中 2 步要把幾個數字放在一起想。第一步：E2 安全。
    val l316 = MineBoard.parse(
        "..R*.*.",
        "...*..o",
        "o.*ooo*",
        "*o*.R..",
        "*.*.*.*",
        ".......",
        "*L..o.*",
        reported = mapOf(2 to 1, 25 to 1, 43 to 0), rule = MineRule.Cross,
    )
    // 第 317 關：8×8、12 顆雷；推理 18 步，其中 5 步要把幾個數字放在一起想。第一步：C4 安全。
    val l317 = MineBoard.parse(
        "ooo*....",
        "ooo.*.*.",
        "ooo*....",
        "*.....*.",
        ".*.*....",
        "......*.",
        ".*....*.",
        ".......*",
        reported = mapOf(),
    )
    // 第 318 關：7×7、11 顆雷；推理 13 步，其中 6 步要把幾個數字放在一起想。第一步：A2 是雷。
    val l318 = MineBoard.parse(
        ".....o.",
        "*o..**o",
        "o*...oL",
        "*oL....",
        "*oo*...",
        ".RR.o**",
        "*...oo*",
        reported = mapOf(20 to 2, 23 to 1, 36 to 2, 37 to 1), mode = MineMode.Flag, counter = true,
    )
    // 第 319 關：8×8、12 顆雷；推理 19 步，其中 5 步要把幾個數字放在一起想。第一步：C3 安全。
    val l319 = MineBoard.parse(
        "*...**..",
        ".....R.*",
        ".*...L..",
        "ooo.*...",
        "ooo.**.L",
        "ooo*...*",
        "ooo*.*.R",
        "ooo.R...",
        reported = mapOf(13 to 2, 21 to 0, 39 to 0, 55 to 1, 60 to 2), counter = true, hiddenLies = 2,
    )
    // 第 320 關：8×8、13 顆雷；推理 17 步，其中 6 步要把幾個數字放在一起想。第一步：B6 安全。
    val l320 = MineBoard.parse(
        ".....L**",
        "...R...*",
        "..*.*...",
        ".*.*.*..",
        "...*..R.",
        "..ooo...",
        ".*ooo.*.",
        "L.ooo*.*",
        reported = mapOf(5 to 2, 11 to 2, 38 to 1, 56 to 0), counter = true,
    )
}
