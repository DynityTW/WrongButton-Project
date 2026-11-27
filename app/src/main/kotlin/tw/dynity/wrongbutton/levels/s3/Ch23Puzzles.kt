package tw.dynity.wrongbutton.levels.s3

import tw.dynity.wrongbutton.R

// 由 tools/s3/liars.py 產生（答案唯一、每一句都用得到）。請改產生器再重新產生。
internal object Ch23Puzzles {
    val l341 = LiarPuzzle(2, listOf(Said(0, R.string.l341_s0), Said(1, R.string.l341_s1)), answer = setOf(0), goal = LiarGoal.Liars, roles = "LT")
    val l342 = LiarPuzzle(3, listOf(Said(0, R.string.l342_s0), Said(1, R.string.l342_s1), Said(2, R.string.l342_s2)), answer = setOf(0, 1), goal = LiarGoal.Liars, roles = "LLT")
    val l343 = LiarPuzzle(3, listOf(Said(0, R.string.l343_s0), Said(1, R.string.l343_s1), Said(2, R.string.l343_s2)), answer = setOf(1), goal = LiarGoal.Liars, roles = "TLT")
    val l344 = LiarPuzzle(3, listOf(Said(0, R.string.l344_s0), Said(1, R.string.l344_s1), Said(2, R.string.l344_s2)), answer = setOf(2), goal = LiarGoal.Button, roles = "TTL", button = 2)
    val l345 = LiarPuzzle(4, listOf(Said(0, R.string.l345_s0), Said(1, R.string.l345_s1), Said(2, R.string.l345_s2), Said(3, R.string.l345_s3)), answer = setOf(2, 3), goal = LiarGoal.Liars, roles = "TTLL")
    val l346 = LiarPuzzle(4, listOf(Said(0, R.string.l346_s0), Said(1, R.string.l346_s1), Said(2, R.string.l346_s2), Said(3, R.string.l346_s3)), answer = setOf(1, 2), goal = LiarGoal.Liars, roles = "TLLT")
    val l347 = LiarPuzzle(4, listOf(Said(0, R.string.l347_s0), Said(1, R.string.l347_s1), Said(2, R.string.l347_s2), Said(3, R.string.l347_s3)), answer = setOf(0, 2, 3), goal = LiarGoal.Liars, roles = "LTLL")
    val l348 = LiarPuzzle(3, listOf(Said(0, R.string.l348_s0), Said(0, R.string.l348_s1), Said(1, R.string.l348_s2), Said(1, R.string.l348_s3), Said(2, R.string.l348_s4), Said(2, R.string.l348_s5)), answer = setOf(1), goal = LiarGoal.Liars, roles = "TLT")
    val l349 = LiarPuzzle(3, listOf(Said(0, R.string.l349_s0), Said(1, R.string.l349_s1), Said(2, R.string.l349_s2)), answer = setOf(1), goal = LiarGoal.Spy, roles = "LSL", spy = true)
    val l350 = LiarPuzzle(4, listOf(Said(0, R.string.l350_s0), Said(1, R.string.l350_s1), Said(2, R.string.l350_s2), Said(3, R.string.l350_s3)), answer = setOf(1, 2, 3), goal = LiarGoal.Liars, roles = "TLLL", narrator = R.string.l350_narrator, narratorTrue = false)
    val l351 = LiarPuzzle(5, listOf(Said(0, R.string.l351_s0), Said(1, R.string.l351_s1), Said(2, R.string.l351_s2), Said(3, R.string.l351_s3), Said(4, R.string.l351_s4)), answer = setOf(1, 2, 3, 4), goal = LiarGoal.Liars, roles = "TLLLL")
    val l352 = LiarPuzzle(4, listOf(Said(0, R.string.l352_s0), Said(1, R.string.l352_s1), Said(2, R.string.l352_s2), Said(3, R.string.l352_s3)), answer = setOf(1, 2), goal = LiarGoal.Liars, roles = "TLLT")
    val l353 = LiarPuzzle(5, listOf(Said(0, R.string.l353_s0), Said(1, R.string.l353_s1), Said(2, R.string.l353_s2), Said(3, R.string.l353_s3), Said(4, R.string.l353_s4)), answer = setOf(3), goal = LiarGoal.Button, roles = "LTLTT", button = 3)
    val l354 = LiarPuzzle(4, listOf(Said(0, R.string.l354_s0), Said(1, R.string.l354_s1), Said(2, R.string.l354_s2), Said(3, R.string.l354_s3)), answer = setOf(0), goal = LiarGoal.Button, roles = "LTST", button = 0, spy = true)
    val l355 = LiarPuzzle(5, listOf(Said(0, R.string.l355_s0), Said(1, R.string.l355_s1), Said(2, R.string.l355_s2), Said(3, R.string.l355_s3), Said(4, R.string.l355_s4)), answer = setOf(1, 2), goal = LiarGoal.Liars, roles = "TLLTT")
    val l356 = LiarPuzzle(4, listOf(Said(0, R.string.l356_s0), Said(0, R.string.l356_s1), Said(1, R.string.l356_s2), Said(1, R.string.l356_s3), Said(2, R.string.l356_s4), Said(2, R.string.l356_s5), Said(3, R.string.l356_s6), Said(3, R.string.l356_s7)), answer = setOf(2), goal = LiarGoal.Liars, roles = "TTLT")
    val l357 = LiarPuzzle(5, listOf(Said(0, R.string.l357_s0), Said(1, R.string.l357_s1), Said(2, R.string.l357_s2), Said(3, R.string.l357_s3), Said(4, R.string.l357_s4)), answer = setOf(0, 3, 4), goal = LiarGoal.Liars, roles = "LTTLL", narrator = R.string.l357_narrator, narratorTrue = false)
    val l358 = LiarPuzzle(4, listOf(Said(0, R.string.l358_s0), Said(0, R.string.l358_s1), Said(1, R.string.l358_s2), Said(1, R.string.l358_s3), Said(2, R.string.l358_s4), Said(2, R.string.l358_s5), Said(3, R.string.l358_s6), Said(3, R.string.l358_s7)), answer = setOf(0), goal = LiarGoal.Spy, roles = "STTL", spy = true)
    val l359 = LiarPuzzle(6, listOf(Said(0, R.string.l359_s0), Said(1, R.string.l359_s1), Said(2, R.string.l359_s2), Said(3, R.string.l359_s3), Said(4, R.string.l359_s4), Said(5, R.string.l359_s5)), answer = setOf(0, 1, 3, 5), goal = LiarGoal.Liars, roles = "LLTLTL")
    val l360 = LiarPuzzle(6, listOf(Said(0, R.string.l360_s0), Said(1, R.string.l360_s1), Said(2, R.string.l360_s2), Said(3, R.string.l360_s3), Said(4, R.string.l360_s4), Said(5, R.string.l360_s5)), answer = setOf(0), goal = LiarGoal.Button, roles = "SLTTLL", button = 0, spy = true, narrator = R.string.l360_narrator, narratorTrue = false)
}
