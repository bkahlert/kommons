package koodies.test.output

import java.io.PrintStream
import java.util.function.Consumer

/**
 * A [PrintStream] implementation that captures written strings.
 */
class PrintStreamCapture(val parent: PrintStream, copy: Consumer<String>) :
    PrintStream(OutputStreamCapture(getSystemStream(parent), copy)) {

    companion object {
        private fun getSystemStream(printStream: PrintStream): PrintStream {
            var systemStream = printStream
            while (systemStream is PrintStreamCapture) systemStream = systemStream.parent
            return systemStream
        }
    }
}
