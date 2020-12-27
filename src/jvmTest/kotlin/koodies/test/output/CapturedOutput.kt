package koodies.test.output

interface CapturedOutput : CharSequence {
    val all: String
    val allLines: List<String>
    val out: String
    val outLines: List<String>
    val err: String
    val errLines: List<String>
}
