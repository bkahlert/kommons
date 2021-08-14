package koodies.io.path

import koodies.io.path.PathWalkDirection.BOTTOM_UP
import koodies.io.path.PathWalkDirection.TOP_DOWN
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.util.ArrayDeque
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

/**
 * An enumeration to describe possible walk directions.
 * There are two of them: beginning from parents, ending with children,
 * and beginning from children, ending with parents. Both use depth-first search.
 */
public enum class PathWalkDirection {
    /** Depth-first search, directory is visited BEFORE its paths */
    TOP_DOWN,

    /** Depth-first search, directory is visited AFTER its paths */
    BOTTOM_UP
    // Do we want also breadth-first search?
}

/**
 * This class is intended to implement different path traversal methods.
 * It allows to iterate through all paths inside a given directory.
 *
 * Use [Path.walk], [Path.walkTopDown] or [Path.walkBottomUp] extension functions to instantiate a `PathTreeWalk` instance.

 * If the file path given is just a file, walker iterates only it.
 * If the file path given does not exist, walker iterates nothing, i.e. it's equivalent to an empty sequence.
 */
public class PathTreeWalk private constructor(
    private val start: Path,
    private val direction: PathWalkDirection = TOP_DOWN,
    private val onEnter: ((Path) -> Boolean)?,
    private val onLeave: ((Path) -> Unit)?,
    private val onFail: ((f: Path, e: FileSystemException) -> Unit)?,
    private val maxDepth: Int = Int.MAX_VALUE,
) : Sequence<Path> {

    internal constructor(start: Path, direction: PathWalkDirection = TOP_DOWN) : this(start, direction, null, null, null)


    /** Returns an iterator walking through paths. */
    override fun iterator(): Iterator<Path> = PathTreeWalkIterator()

    /** Abstract class that encapsulates path visiting in some order, beginning from a given [root] */
    private abstract class WalkState(val root: Path) {
        /** Call of this function proceeds to a next path for visiting and returns it */
        abstract fun step(): Path?
    }

    /** Abstract class that encapsulates directory visiting in some order, beginning from a given [root] */
    private abstract class DirectoryState(rootDir: Path) : WalkState(rootDir) {
        init {
            assert(rootDir.isDirectory()) { "rootDir must be verified to be directory beforehand." }
        }
    }

    private inner class PathTreeWalkIterator : AbstractIterator<Path>() {

        // Stack of directory states, beginning from the start directory
        private val state = ArrayDeque<WalkState>()

        init {
            state
            when {
                start.isDirectory() -> state.push(directoryState(start))
                start.isRegularFile() -> state.push(SinglePathState(start))
                else -> done()
            }
        }

        override fun computeNext() {
            val nextPath = gotoNext()
            if (nextPath != null)
                setNext(nextPath)
            else
                done()
        }


        private fun directoryState(root: Path): DirectoryState {
            return when (direction) {
                TOP_DOWN -> TopDownDirectoryState(root)
                BOTTOM_UP -> BottomUpDirectoryState(root)
            }
        }

        private tailrec fun gotoNext(): Path? {
            // Take next path from the top of the stack or return if there's nothing left
            val topState = state.peek() ?: return null
            val path = topState.step()
            if (path == null) {
                // There is nothing more on the top of the stack, go back
                state.pop()
                return gotoNext()
            } else {
                // Check that path/directory matches the filter
                if (path == topState.root || !path.isDirectory() || state.size >= maxDepth) {
                    // Proceed to a root directory or a simple path
                    return path
                } else {
                    // Proceed to a sub-directory
                    state.push(directoryState(path))
                    return gotoNext()
                }
            }
        }

        /** Visiting in bottom-up order */
        private inner class BottomUpDirectoryState(rootDir: Path) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var pathList: List<Path>? = null

            private var pathIndex = 0

            private var failed = false

            /** First all children, then root directory */
            override fun step(): Path? {
                if (!failed && pathList == null) {
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    pathList = root.listDirectoryEntries()
                    if (pathList == null) {
                        onFail?.invoke(root, java.nio.file.AccessDeniedException(root.toString(), null, "Cannot list paths in a directory"))
                        failed = true
                    }
                }
                if (pathList != null && pathIndex < pathList!!.size) {
                    // First visit all paths
                    return pathList!![pathIndex++]
                } else if (!rootVisited) {
                    // Then visit root
                    rootVisited = true
                    return root
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        /** Visiting in top-down order */
        private inner class TopDownDirectoryState(rootDir: Path) : DirectoryState(rootDir) {

            private var rootVisited = false

            private var pathList: List<Path>? = null

            private var pathIndex = 0

            /** First root directory, then all children */
            override fun step(): Path? {
                if (!rootVisited) {
                    // First visit root
                    if (onEnter?.invoke(root) == false) {
                        return null
                    }

                    rootVisited = true
                    return root
                } else if (pathList == null || pathIndex < pathList!!.size) {
                    if (pathList == null) {
                        // Then read an array of paths, if any
                        pathList = root.listDirectoryEntries()
                        if (pathList == null) {
                            onFail?.invoke(root, java.nio.file.AccessDeniedException("$root", null, "Cannot list paths in a directory"))
                        }
                        if (pathList == null || pathList!!.isEmpty()) {
                            onLeave?.invoke(root)
                            return null
                        }
                    }
                    // Then visit all paths
                    return pathList!![pathIndex++]
                } else {
                    // That's all
                    onLeave?.invoke(root)
                    return null
                }
            }
        }

        private inner class SinglePathState(rootPath: Path) : WalkState(rootPath) {
            private var visited: Boolean = false

            init {
                assert(rootPath.isRegularFile()) { "rootPath must be verified to be path beforehand." }
            }

            override fun step(): Path? {
                if (visited) return null
                visited = true
                return root
            }
        }
    }

    /**
     * Sets a predicate [function], that is called on any entered directory before its paths are visited
     * and before it is visited itself.
     *
     * If the [function] returns `false` the directory is not entered and neither it nor its paths are visited.
     */
    public fun onEnter(function: (Path) -> Boolean): PathTreeWalk {
        return PathTreeWalk(start, direction, onEnter = function, onLeave = onLeave, onFail = onFail, maxDepth = maxDepth)
    }

    /**
     * Sets a callback [function], that is called on any left directory after its paths are visited and after it is visited itself.
     */
    public fun onLeave(function: (Path) -> Unit): PathTreeWalk {
        return PathTreeWalk(start, direction, onEnter = onEnter, onLeave = function, onFail = onFail, maxDepth = maxDepth)
    }

    /**
     * Set a callback [function], that is called on a directory when it's impossible to get its path list.
     *
     * [onEnter] and [onLeave] callback functions are called even in this case.
     */
    public fun onFail(function: (Path, FileSystemException) -> Unit): PathTreeWalk {
        return PathTreeWalk(start, direction, onEnter = onEnter, onLeave = onLeave, onFail = function, maxDepth = maxDepth)
    }

    /**
     * Sets the maximum [depth] of a directory tree to traverse. By default there is no limit.
     *
     * The value must be positive and [Int.MAX_VALUE] is used to specify an unlimited depth.
     *
     * With a value of 1, walker visits only the origin directory and all its immediate children,
     * with a value of 2 also grandchildren, etc.
     */
    public fun maxDepth(depth: Int): PathTreeWalk {
        if (depth <= 0)
            throw IllegalArgumentException("depth must be positive, but was $depth.")
        return PathTreeWalk(start, direction, onEnter, onLeave, onFail, depth)
    }
}

/**
 * Gets a sequence for visiting this directory and all its content.
 *
 * @param direction walk direction, top-down (by default) or bottom-up.
 */
public fun Path.walk(direction: PathWalkDirection = TOP_DOWN): PathTreeWalk =
    PathTreeWalk(this, direction)

/**
 * Gets a sequence for visiting this directory and all its content in top-down order.
 * Depth-first search is used and directories are visited before all their paths.
 */
public fun Path.walkTopDown(): PathTreeWalk = walk(TOP_DOWN)

/**
 * Gets a sequence for visiting this directory and all its content in bottom-up order.
 * Depth-first search is used and directories are visited after all their paths.
 */
public fun Path.walkBottomUp(): PathTreeWalk = walk(BOTTOM_UP)
