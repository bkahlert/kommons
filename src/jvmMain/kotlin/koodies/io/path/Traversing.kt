package koodies.io.path

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

public fun <T> Path.traverse(
    initial: T,
    operation: (acc: T, element: T) -> T,
    transform: Path.() -> T,
): T =
    if (isDirectory()) listDirectoryEntries().fold(initial) { acc, file ->
        file.traverse(operation(acc, transform(file)), operation, transform)
    } else initial

public fun <T> Path.traverse(
    initial: T,
    transform: Path.() -> T,
    operation: (acc: T, element: T) -> T,
): T =
    if (isDirectory()) listDirectoryEntries().fold(initial) { acc, file ->
        file.traverse(operation(acc, transform(file)), transform, operation)
    } else initial
