import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.provider.Provider

fun DependencySet.add(element: Provider<out MinimalExternalModuleDependency>): Boolean = add(element.get())
fun DependencySet.add(element: MinimalExternalModuleDependency): Boolean = add(element.toDependency())

fun MinimalExternalModuleDependency.toDependency(): Dependency =
    DefaultExternalModuleDependency(
        module.group,
        module.name,
        versionConstraint.preferredVersion
    )
