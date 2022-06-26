package com.bkahlert.kommons.test

import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TestPlanAnalysis(private val testPlan: TestPlan) {
    val allTestIdentifiers: List<TestIdentifier> by lazy { testPlan.roots.flatMap { testPlan.getDescendants(it) } }
    val parentMappings: Map<String, String> by lazy { allTestIdentifiers.associate { it.relation } }
    val leaves: List<String> by lazy { parentMappings.values - parentMappings.keys }

    val allTests: List<TestIdentifier> by lazy { allTestIdentifiers.filter { it.isTest || it.isLeaf(testPlan) } }
    val allMethodSources: List<MethodSource> by lazy { allTests.mapNotNull { it.source.orElse(null) as? MethodSource } }
    val allTestJavaMethods: List<Method> by lazy { allMethodSources.map { it.javaMethod } }
}

private val analyses = mutableMapOf<TestPlan, TestPlanAnalysis>()
private val lock = ReentrantLock()
private val TestPlan.cachedAnalysis get() = lock.withLock { analyses.getOrPut(this) { TestPlanAnalysis(this) } }

val TestPlan.leaves: List<String> get() = cachedAnalysis.leaves
val TestPlan.allTests: List<TestIdentifier> get() = cachedAnalysis.allTests
val TestPlan.allTestJavaMethods: List<Method> get() = cachedAnalysis.allTestJavaMethods

fun TestIdentifier.isLeaf(testPlan: TestPlan) = testPlan.leaves.contains(uniqueId)
val TestIdentifier.relation: Pair<String, String> get() = parentId.map { it to uniqueId }.orElse(null)
