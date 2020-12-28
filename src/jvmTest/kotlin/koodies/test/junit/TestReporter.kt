package koodies.test.junit

import koodies.test.allTestIdentifiers
import koodies.text.LineSeparators.LF
import org.junit.platform.engine.TestTag
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

class TestReporter : TestExecutionListener {
    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        val type = TagDistributionReportRenderer.bySystemProperty("typedTestReport", TagDistributionReportRenderer.NONE)
        print(type.render(testPlan))
    }
}

private enum class TagDistributionReportRenderer(val render: (TestPlan) -> String) {
    NONE({ testPlan ->
        ""
    }),
    SUMMARY({ testPlan ->
        with(testPlan.tagDistribution) {
            "Report \"Tag Distribution\": " +
                joinToString("; ", postfix = LF) { (tags, tests) ->
                    (if (tags.isEmpty()) "NO TAGS" else tags.joinToString("+") { it.name }) + ": " + tests.size
                }
        }
    }),
    FULL({ testPlan ->
        with(testPlan.tagDistribution) {
            "Report: TAG DISTRIBUTION\n\n" + joinToString("\n") { (tags, tests) ->
                (if (tags.isEmpty()) "Tags: NONE" else tags.joinToString(" + ", prefix = "Tags: ", postfix = "") { it.name }) +
                    tests.joinToString("\n- ", prefix = "\n- ", postfix = "\n") { it.displayName }
            }
        }
    });

    companion object {
        fun bySystemProperty(propertyName: String, aDefault: TagDistributionReportRenderer): TagDistributionReportRenderer {
            val propertyValue = System.getProperty(propertyName)
            return values().firstOrNull { it.name.equals(propertyValue, ignoreCase = true) } ?: aDefault
        }
    }
}

private val TestPlan.tagDistribution: List<Pair<MutableSet<TestTag>, List<TestIdentifier>>>
    get() = allTestIdentifiers.filter { it.isTest }.groupBy { it.tags }.toList()
//private val TestPlan.allTestIdentifiers: List<TestIdentifier>
//    get() = roots.flatMap { getDescendants(it) }
