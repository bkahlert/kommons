plugins {
    id("java-platform")
    id("kommons-publishing-conventions")
}

group = "com.bkahlert.kommons"
description = "Features for Kotlin™ Multiplatform You Didn't Know You Were Missing"

val bomProject = project

// Explicitly exclude subprojects that will never be published
// so that when configuring this project, we don't force their
// configuration and do unecessary work.
val excludeFromBom: List<String> = emptyList()
fun projectsFilter(candidateProject: Project) =
    excludeFromBom.all { !candidateProject.name.contains(it) } &&
        candidateProject.name != bomProject.name

// Declare that this subproject depends on all subprojects matching the filter
// When this subproject is configured, it will force configuration of all subprojects
// so that we can declare dependencies on them
rootProject.subprojects.filter(::projectsFilter).forEach { bomProject.evaluationDependsOn(it.path) }

dependencies {
    constraints {
        rootProject.subprojects.filter { project ->
            // Only declare dependencies on projects that will have publications
            projectsFilter(project) && project.tasks.findByName("publish")?.enabled == true
        }.forEach { api(project(it.path)) }
    }
}

mavenPublishing {
    pom {
        name.set("Kommons Bill of Materials")
    }
}
