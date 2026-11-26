package tw.dynity.wrongbutton.levels.s3

import tw.dynity.wrongbutton.R

// 由 tools/s3/captcha.py 產生（答案唯一、每一句都用得到、謊話有陷阱）。請改產生器再重新產生。
internal object Ch22Puzzles {
    val l321 = listOf(
        Captcha(3, listOf(Tile(2, 0, 1), Tile(2, 1, 1), Tile(1, 0, 1), Tile(0, 0, 1), Tile(1, 0, 1), Tile(1, 0, 1), Tile(0, 1, 1), Tile(2, 1, 1), Tile(0, 1, 1)), listOf(R.string.l321_r0_s0), answer = setOf(1, 6, 7, 8)),
    )
    val l322 = listOf(
        Captcha(3, listOf(Tile(2, 2, 2), Tile(2, 2, 1), Tile(1, 0, 2), Tile(1, 1, 2), Tile(0, 2, 1), Tile(0, 0, 1), Tile(2, 1, 1), Tile(1, 2, 1), Tile(2, 0, 2)), listOf(R.string.l322_r0_s0), answer = setOf(0, 1, 2, 3, 5, 6, 7, 8)),
        Captcha(3, listOf(Tile(0, 1, 1), Tile(0, 0, 1), Tile(2, 2, 1), Tile(2, 1, 2), Tile(0, 0, 2), Tile(0, 2, 1), Tile(0, 2, 2), Tile(2, 0, 2), Tile(2, 1, 3)), listOf(R.string.l322_r1_s0), answer = setOf(3, 8)),
    )
    val l323 = listOf(
        Captcha(3, listOf(Tile(0, 1, 2), Tile(2, 1, 3), Tile(1, 0, 3), Tile(0, 1, 2), Tile(0, 1, 3), Tile(0, 0, 2), Tile(1, 2, 1), Tile(0, 0, 1), Tile(1, 0, 2)), listOf(R.string.l323_r0_s0, R.string.l323_r0_s1), answer = setOf(6, 7)),
        Captcha(3, listOf(Tile(1, 1, 3), Tile(0, 1, 2), Tile(2, 0, 2), Tile(0, 2, 3), Tile(0, 0, 1), Tile(1, 1, 2), Tile(1, 1, 1), Tile(1, 0, 3), Tile(1, 2, 1)), listOf(R.string.l323_r1_s0, R.string.l323_r1_s1), answer = setOf(0, 1, 2, 3, 5, 7)),
    )
    val l324 = listOf(
        Captcha(3, listOf(Tile(2, 1, 2), Tile(0, 1, 3), Tile(0, 2, 3), Tile(2, 2, 1), Tile(0, 0, 1), Tile(0, 1, 3), Tile(0, 0, 2), Tile(1, 2, 3), Tile(1, 0, 2)), listOf(R.string.l324_r0_s0), answer = setOf(1, 5)),
        Captcha(3, listOf(Tile(2, 0, 3), Tile(0, 1, 2), Tile(0, 1, 1), Tile(1, 2, 1), Tile(2, 1, 2), Tile(1, 2, 2), Tile(2, 2, 1), Tile(2, 1, 2), Tile(1, 2, 2)), listOf(R.string.l324_r1_s0, R.string.l324_r1_s1), answer = setOf(2, 3, 6)),
        Captcha(3, listOf(Tile(2, 1, 3), Tile(2, 2, 2), Tile(0, 0, 3), Tile(1, 2, 3), Tile(2, 1, 1), Tile(0, 2, 3), Tile(1, 2, 3), Tile(0, 2, 1), Tile(2, 0, 3)), listOf(R.string.l324_r2_s0, R.string.l324_r2_s1, R.string.l324_r2_s2), answer = setOf(2, 3, 5, 7)),
    )
    val l325 = listOf(
        Captcha(3, listOf(Tile(2, 1, 3), Tile(2, 0, 1), Tile(2, 0, 2), Tile(1, 1, 2), Tile(0, 0, 1), Tile(1, 0, 3), Tile(1, 0, 2), Tile(2, 1, 3), Tile(0, 1, 1)), listOf(R.string.l325_r0_s0), answer = setOf(1, 2, 3, 4, 6, 8), spoken = true, lies = setOf(0), complementRule = true),
        Captcha(3, listOf(Tile(2, 0, 3), Tile(2, 1, 3), Tile(0, 0, 3), Tile(2, 0, 2), Tile(2, 0, 3), Tile(2, 2, 1), Tile(1, 2, 3), Tile(0, 1, 1), Tile(2, 2, 1)), listOf(R.string.l325_r1_s0), answer = setOf(0, 1, 3, 4, 5, 6, 8), spoken = true),
        Captcha(3, listOf(Tile(0, 1, 1), Tile(1, 0, 3), Tile(0, 1, 3), Tile(0, 2, 1), Tile(0, 1, 2), Tile(2, 2, 1), Tile(1, 2, 2), Tile(0, 2, 3), Tile(2, 1, 2)), listOf(R.string.l325_r2_s0), answer = setOf(0, 1, 2, 3, 4, 6, 7, 8), spoken = true, lies = setOf(0), complementRule = true),
    )
    val l326 = listOf(
        Captcha(3, listOf(Tile(0, 1, 3), Tile(1, 0, 1), Tile(2, 1, 2), Tile(0, 0, 1), Tile(0, 1, 2), Tile(0, 1, 3), Tile(1, 1, 3), Tile(0, 0, 1), Tile(1, 2, 3)), listOf(R.string.l326_r0_s0, R.string.l326_r0_s1, R.string.l326_r0_s2), answer = setOf(0, 4)),
        Captcha(3, listOf(Tile(1, 1, 3), Tile(0, 1, 3), Tile(0, 1, 3), Tile(1, 1, 2), Tile(1, 1, 3), Tile(2, 0, 3), Tile(0, 2, 3), Tile(2, 0, 2), Tile(0, 0, 1)), listOf(R.string.l326_r1_s0, R.string.l326_r1_s1, R.string.l326_r1_s2), answer = setOf(4, 8)),
    )
    val l327 = listOf(
        Captcha(3, listOf(Tile(0, 0, 3), Tile(2, 2, 3), Tile(1, 0, 3), Tile(0, 0, 1), Tile(2, 2, 3), Tile(2, 2, 1), Tile(0, 1, 3), Tile(1, 2, 1), Tile(2, 1, 2)), listOf(R.string.l327_r0_s0, R.string.l327_r0_s1, R.string.l327_r0_s2), answer = setOf(2, 3, 6, 8)),
        Captcha(3, listOf(Tile(2, 2, 1), Tile(1, 1, 2), Tile(0, 2, 2), Tile(1, 1, 1), Tile(0, 2, 3), Tile(0, 1, 3), Tile(1, 2, 1), Tile(1, 0, 3), Tile(1, 2, 2)), listOf(R.string.l327_r1_s0, R.string.l327_r1_s1, R.string.l327_r1_s2), answer = setOf(0, 2, 6, 8)),
    )
    val l328 = listOf(
        Captcha(3, listOf(Tile(2, 0, 2), Tile(0, 2, 1), Tile(0, 2, 3), Tile(1, 2, 1), Tile(2, 0, 3), Tile(1, 2, 2), Tile(0, 2, 1), Tile(0, 1, 2), Tile(1, 1, 2)), listOf(R.string.l328_r0_s0, R.string.l328_r0_s1, R.string.l328_r0_s2), answer = setOf(0, 4, 8)),
        Captcha(3, listOf(Tile(0, 0, 2), Tile(2, 2, 3), Tile(2, 2, 3), Tile(1, 1, 1), Tile(1, 2, 3), Tile(1, 2, 3), Tile(1, 0, 2), Tile(1, 1, 2), Tile(0, 2, 3)), listOf(R.string.l328_r1_s0, R.string.l328_r1_s1, R.string.l328_r1_s2), answer = setOf(0, 4, 8)),
    )
    val l329 = listOf(
        Captcha(3, listOf(Tile(0, 2, 3), Tile(2, 1, 1), Tile(0, 2, 1), Tile(1, 0, 2), Tile(0, 0, 3), Tile(1, 2, 1), Tile(0, 1, 2), Tile(1, 1, 2), Tile(2, 2, 1)), listOf(R.string.l329_r0_s0, R.string.l329_r0_s1, R.string.l329_r0_s2), answer = setOf(1, 3, 8), spoken = true, lies = setOf(1), cover = tw.dynity.wrongbutton.core.character.FaceCover.Eyes),
        Captcha(3, listOf(Tile(0, 2, 2), Tile(2, 0, 3), Tile(0, 0, 1), Tile(0, 1, 1), Tile(0, 2, 3), Tile(0, 0, 2), Tile(0, 2, 1), Tile(2, 2, 3), Tile(1, 2, 1)), listOf(R.string.l329_r1_s0, R.string.l329_r1_s1, R.string.l329_r1_s2), answer = setOf(1, 2), spoken = true, lies = setOf(0), cover = tw.dynity.wrongbutton.core.character.FaceCover.Eyes),
    )
    val l330 = listOf(
        Captcha(3, listOf(Tile(1, 0, 3), Tile(2, 2, 3), Tile(1, 0, 1), Tile(1, 0, 1), Tile(0, 0, 1), Tile(2, 1, 2), Tile(1, 1, 1), Tile(2, 2, 3), Tile(1, 1, 3)), listOf(R.string.l330_r0_s0, R.string.l330_r0_s1, R.string.l330_r0_s2), answer = setOf(0, 1, 2, 3, 5, 6, 7, 8), exactlyTrue = 1),
    )
    val l331 = listOf(
        Captcha(3, listOf(Tile(1, 1, 2), Tile(1, 2, 2), Tile(2, 1, 2), Tile(2, 2, 2), Tile(2, 0, 1), Tile(0, 0, 1), Tile(0, 2, 2), Tile(2, 1, 3), Tile(2, 0, 1)), listOf(R.string.l331_r0_s0, R.string.l331_r0_s1, R.string.l331_r0_s2), answer = setOf(3, 5, 7)),
        Captcha(3, listOf(Tile(1, 2, 3), Tile(2, 0, 2), Tile(2, 1, 3), Tile(2, 1, 2), Tile(1, 1, 1), Tile(2, 1, 2), Tile(0, 0, 2), Tile(2, 1, 3), Tile(1, 0, 3)), listOf(R.string.l331_r1_s0, R.string.l331_r1_s1, R.string.l331_r1_s2), answer = setOf(0, 2, 8)),
    )
    val l332 = listOf(
        Captcha(4, listOf(Tile(1, 2, 2), Tile(3, 2, 1), Tile(2, 1, 3), Tile(2, 0, 2), Tile(0, 1, 2), Tile(3, 2, 1), Tile(0, 2, 1), Tile(1, 0, 3), Tile(1, 1, 3), Tile(3, 0, 3), Tile(1, 2, 3), Tile(2, 2, 1), Tile(0, 0, 1), Tile(3, 0, 1), Tile(1, 0, 2), Tile(2, 0, 3)), listOf(R.string.l332_r0_s0, R.string.l332_r0_s1, R.string.l332_r0_s2, R.string.l332_r0_s3), answer = setOf(1, 4, 6)),
        Captcha(4, listOf(Tile(1, 2, 2), Tile(2, 0, 1), Tile(3, 1, 1), Tile(2, 1, 2), Tile(3, 0, 2), Tile(0, 1, 1), Tile(1, 2, 2), Tile(0, 2, 1), Tile(0, 1, 1), Tile(1, 2, 1), Tile(3, 2, 3), Tile(0, 2, 3), Tile(0, 0, 2), Tile(3, 0, 2), Tile(0, 2, 3), Tile(3, 0, 1)), listOf(R.string.l332_r1_s0, R.string.l332_r1_s1, R.string.l332_r1_s2, R.string.l332_r1_s3), answer = setOf(4, 12)),
    )
    val l333 = listOf(
        Captcha(3, listOf(Tile(1, 1, 2), Tile(2, 2, 3), Tile(2, 0, 1), Tile(1, 2, 1), Tile(1, 2, 1), Tile(0, 1, 1), Tile(2, 1, 1), Tile(2, 2, 3), Tile(2, 0, 3)), listOf(R.string.l333_r0_s0, R.string.l333_r0_s1, R.string.l333_r0_s2, R.string.l333_r0_s3), answer = setOf(0, 4, 6), spoken = true, lies = setOf(0, 1)),
    )
    val l334 = listOf(
        Captcha(3, listOf(Tile(1, 2, 3), Tile(2, 1, 2), Tile(2, 2, 2), Tile(2, 0, 2), Tile(2, 0, 3), Tile(2, 2, 3), Tile(2, 1, 2), Tile(2, 2, 3), Tile(1, 1, 3)), listOf(R.string.l334_r0_s0, R.string.l334_r0_s1, R.string.l334_r0_s2, R.string.l334_r0_s3), answer = setOf(3, 4), exactlyTrue = 2),
    )
    val l335 = listOf(
        Captcha(3, listOf(Tile(1, 2, 1), Tile(2, 2, 2), Tile(1, 1, 2), Tile(2, 0, 3), Tile(0, 0, 2), Tile(2, 1, 1), Tile(2, 0, 3), Tile(2, 1, 1), Tile(2, 2, 2)), listOf(R.string.l335_r0_s0, R.string.l335_r0_s1, R.string.l335_r0_s2), answer = setOf(1, 5)),
        Captcha(3, listOf(Tile(2, 2, 2), Tile(0, 1, 2), Tile(1, 1, 2), Tile(1, 0, 2), Tile(0, 0, 3), Tile(0, 2, 3), Tile(1, 1, 1), Tile(0, 0, 2), Tile(1, 2, 2)), listOf(R.string.l335_r1_s0, R.string.l335_r1_s1, R.string.l335_r1_s2), answer = setOf(4, 8)),
    )
    val l336 = listOf(
        Captcha(4, listOf(Tile(0, 0, 1), Tile(2, 0, 2), Tile(0, 0, 3), Tile(2, 1, 3), Tile(2, 2, 1), Tile(3, 1, 1), Tile(0, 3, 1), Tile(0, 3, 3), Tile(1, 0, 1), Tile(1, 0, 1), Tile(3, 3, 1), Tile(0, 2, 2), Tile(3, 0, 2), Tile(0, 0, 1), Tile(1, 2, 1), Tile(1, 1, 1)), listOf(R.string.l336_r0_s0, R.string.l336_r0_s1, R.string.l336_r0_s2, R.string.l336_r0_s3), answer = setOf(2, 7, 8, 13)),
        Captcha(4, listOf(Tile(0, 0, 1), Tile(1, 1, 1), Tile(2, 1, 3), Tile(0, 2, 2), Tile(3, 3, 2), Tile(3, 1, 1), Tile(3, 1, 2), Tile(3, 3, 3), Tile(1, 2, 3), Tile(0, 0, 3), Tile(0, 3, 3), Tile(2, 1, 1), Tile(3, 3, 1), Tile(3, 2, 1), Tile(2, 3, 3), Tile(0, 2, 2)), listOf(R.string.l336_r1_s0, R.string.l336_r1_s1, R.string.l336_r1_s2, R.string.l336_r1_s3), answer = setOf(2, 5, 11, 12)),
    )
    val l337 = listOf(
        Captcha(3, listOf(Tile(2, 0, 2), Tile(2, 1, 2), Tile(1, 0, 3), Tile(1, 0, 2), Tile(2, 2, 3), Tile(2, 0, 3), Tile(1, 0, 2), Tile(2, 0, 1), Tile(0, 2, 1)), listOf(R.string.l337_r0_s0, R.string.l337_r0_s1, R.string.l337_r0_s2, R.string.l337_r0_s3), answer = setOf(3, 4, 8), spoken = true, lies = setOf(1, 3), cover = tw.dynity.wrongbutton.core.character.FaceCover.Mouth),
    )
    val l338 = listOf(
        Captcha(3, listOf(Tile(2, 1, 3), Tile(2, 1, 2), Tile(2, 2, 3), Tile(0, 0, 1), Tile(1, 1, 1), Tile(0, 0, 3), Tile(2, 0, 2), Tile(0, 2, 2), Tile(1, 2, 1)), listOf(R.string.l338_r0_s0, R.string.l338_r0_s1, R.string.l338_r0_s2, R.string.l338_r0_s3), answer = setOf(1, 6), exactlyTrue = 1),
        Captcha(3, listOf(Tile(1, 0, 2), Tile(1, 2, 1), Tile(1, 1, 2), Tile(0, 2, 1), Tile(1, 2, 3), Tile(0, 2, 1), Tile(2, 2, 2), Tile(2, 1, 2), Tile(1, 2, 2)), listOf(R.string.l338_r1_s0, R.string.l338_r1_s1, R.string.l338_r1_s2, R.string.l338_r1_s3), answer = setOf(0, 2, 6, 8), exactlyTrue = 2),
    )
    val l339 = listOf(
        Captcha(4, listOf(Tile(3, 1, 1), Tile(1, 2, 3), Tile(1, 2, 2), Tile(1, 2, 2), Tile(0, 2, 2), Tile(1, 1, 1), Tile(3, 0, 1), Tile(2, 1, 2), Tile(3, 1, 1), Tile(1, 0, 2), Tile(1, 1, 3), Tile(2, 2, 2), Tile(3, 2, 2), Tile(2, 1, 1), Tile(1, 0, 3), Tile(3, 2, 3)), listOf(R.string.l339_r0_s0, R.string.l339_r0_s1, R.string.l339_r0_s2, R.string.l339_r0_s3), answer = setOf(0, 2, 5, 7, 8, 13)),
        Captcha(4, listOf(Tile(1, 3, 3), Tile(0, 1, 1), Tile(1, 0, 1), Tile(0, 3, 3), Tile(2, 0, 3), Tile(2, 0, 2), Tile(1, 1, 1), Tile(2, 3, 3), Tile(1, 3, 2), Tile(3, 2, 3), Tile(1, 2, 3), Tile(1, 3, 3), Tile(0, 3, 1), Tile(2, 0, 1), Tile(1, 1, 2), Tile(2, 1, 1)), listOf(R.string.l339_r1_s0, R.string.l339_r1_s1, R.string.l339_r1_s2, R.string.l339_r1_s3), answer = setOf(2, 7, 8, 13)),
        Captcha(4, listOf(Tile(1, 0, 2), Tile(3, 2, 2), Tile(2, 2, 3), Tile(0, 1, 2), Tile(1, 1, 2), Tile(3, 3, 1), Tile(1, 2, 2), Tile(0, 3, 1), Tile(3, 0, 2), Tile(2, 2, 1), Tile(3, 2, 3), Tile(1, 0, 2), Tile(2, 3, 2), Tile(1, 1, 3), Tile(2, 0, 2), Tile(2, 1, 1)), listOf(R.string.l339_r2_s0, R.string.l339_r2_s1, R.string.l339_r2_s2, R.string.l339_r2_s3), answer = setOf(0, 2, 4, 9)),
    )
    val l340 = listOf(
        Captcha(4, listOf(Tile(0, 3, 2), Tile(2, 1, 2), Tile(0, 0, 1), Tile(0, 2, 2), Tile(1, 2, 1), Tile(3, 1, 1), Tile(3, 3, 3), Tile(3, 2, 2), Tile(3, 0, 2), Tile(3, 2, 3), Tile(1, 1, 3), Tile(1, 2, 3), Tile(2, 1, 1), Tile(3, 2, 3), Tile(3, 0, 1), Tile(0, 3, 1)), listOf(R.string.l340_r0_s0, R.string.l340_r0_s1, R.string.l340_r0_s2, R.string.l340_r0_s3, R.string.l340_r0_s4), answer = setOf(1, 6, 11, 12), spoken = true, lies = setOf(3, 4), subtle = true),
        Captcha(4, listOf(Tile(0, 1, 1), Tile(0, 3, 1), Tile(3, 3, 3), Tile(2, 0, 1), Tile(3, 3, 1), Tile(2, 0, 1), Tile(0, 3, 1), Tile(1, 2, 2), Tile(1, 1, 2), Tile(0, 3, 3), Tile(1, 2, 1), Tile(1, 3, 3), Tile(1, 0, 3), Tile(1, 1, 3), Tile(2, 2, 2), Tile(0, 1, 1)), listOf(R.string.l340_r1_s0, R.string.l340_r1_s1, R.string.l340_r1_s2, R.string.l340_r1_s3, R.string.l340_r1_s4), answer = setOf(2, 3, 4), exactlyTrue = 3),
    )
}
