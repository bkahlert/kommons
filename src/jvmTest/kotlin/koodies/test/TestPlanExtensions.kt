package koodies.test

import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Method

val TestPlan.rootIds: List<String> get() = roots.map { root -> root.uniqueId }
val TestPlan.allTestIdentifiers: List<TestIdentifier> get() = roots.flatMap { getDescendants(it) }
val TestPlan.parentMappings: Map<String, String> get() = allTestIdentifiers.mapNotNull { it.relation }.toMap()
val TestPlan.leaves: List<String> get() = parentMappings.values - parentMappings.keys

val TestPlan.allTests: List<TestIdentifier> get() = allTestIdentifiers.filter { it.isTest || it.isLeaf(this) }
val TestPlan.allMethodSources: List<MethodSource> get() = allTests.mapNotNull { it.source.orElse(null) as? MethodSource }
val TestPlan.allTestJavaMethods: List<Method> get() = allMethodSources.map { it.javaMethod }

val TestPlan.allTestContainers: List<TestIdentifier> get() = allTestIdentifiers.filter { it.isTopLevelContainer(this) }
val TestPlan.allContainerSources: List<ClassSource> get() = allTestContainers.mapNotNull { it.source.orElse(null) as? ClassSource }
val TestPlan.allContainerJavaClasses: List<Class<*>> get() = allContainerSources.mapNotNull { it.javaClass }

fun TestIdentifier.isTopLevelContainer(testPlan: TestPlan): Boolean = parentId.map { testPlan.rootIds.contains(it) }.orElse(true)
fun TestIdentifier.isLeaf(testPlan: TestPlan) = testPlan.leaves.contains(uniqueId)
val TestIdentifier.relation get() = parentId.map { it to uniqueId }.orElse(null)
