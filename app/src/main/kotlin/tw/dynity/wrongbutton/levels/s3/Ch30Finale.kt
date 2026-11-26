package tw.dynity.wrongbutton.levels.s3

import tw.dynity.wrongbutton.core.character.FaceCover

// 第 30 章〈終章〉：第 21–29 章的九種謎題各兩題最難的，第 500 關是最後一場「誰在說謊」。
// 題目由 tools/s3/finale.py 借用各章的產生器產生（Ch30Puzzles.kt）。

object Level481LastMines : MineLevel(Ch30Meta.l481, Ch30Puzzles.l481, subtle = true)
object Level482LastCaptcha : CaptchaLevel(Ch30Meta.l482, Ch30Puzzles.l482)
object Level483LastTrial : LiarLevel(Ch30Meta.l483, Ch30Puzzles.l483, cover = FaceCover.Eyes, subtle = true)
object Level484LastRule : RuleLevel(Ch30Meta.l484, Ch30Puzzles.l484, subtle = true)
object Level485LastCode : CodeLevel(Ch30Meta.l485, Ch30Puzzles.l485, cover = FaceCover.Mouth, subtle = true)
object Level486LastCircuit : WireLevel(Ch30Meta.l486, Ch30Puzzles.l486)
object Level487LastQueue : OrderLevel(Ch30Meta.l487, Ch30Puzzles.l487)
object Level488LastMaze : MazeLevel(Ch30Meta.l488, Ch30Puzzles.l488, cover = FaceCover.Eyes, subtle = true)
object Level489LastPassword : PinLevel(Ch30Meta.l489, Ch30Puzzles.l489)
object Level490LastMirror : MirrorLevel(Ch30Meta.l490, Ch30Puzzles.l490, subtle = true)
object Level491FlagsAgain : MineLevel(Ch30Meta.l491, Ch30Puzzles.l491, subtle = true)
object Level492HumanAgain : CaptchaLevel(Ch30Meta.l492, Ch30Puzzles.l492)
object Level493SpyAgain : LiarLevel(Ch30Meta.l493, Ch30Puzzles.l493)
object Level494RuleAgain : RuleLevel(Ch30Meta.l494, Ch30Puzzles.l494, cover = FaceCover.Mouth, subtle = true)
object Level495CodeAgain : CodeLevel(Ch30Meta.l495, Ch30Puzzles.l495)
object Level496CircuitAgain : WireLevel(Ch30Meta.l496, Ch30Puzzles.l496)
object Level497QueueAgain : OrderLevel(Ch30Meta.l497, Ch30Puzzles.l497, cover = FaceCover.Eyes, subtle = true)
object Level498MazeAgain : MazeLevel(Ch30Meta.l498, Ch30Puzzles.l498, subtle = true)
object Level499PasswordAgain : PinLevel(Ch30Meta.l499, Ch30Puzzles.l499, cover = FaceCover.Mouth, subtle = true)
object Level500TheButton : LiarLevel(Ch30Meta.l500, Ch30Puzzles.l500, cover = FaceCover.Mouth, subtle = true)

val Chapter30 = listOf(
    Level481LastMines, Level482LastCaptcha, Level483LastTrial, Level484LastRule, Level485LastCode,
    Level486LastCircuit, Level487LastQueue, Level488LastMaze, Level489LastPassword, Level490LastMirror,
    Level491FlagsAgain, Level492HumanAgain, Level493SpyAgain, Level494RuleAgain, Level495CodeAgain,
    Level496CircuitAgain, Level497QueueAgain, Level498MazeAgain, Level499PasswordAgain, Level500TheButton,
)
