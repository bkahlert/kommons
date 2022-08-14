package com.bkahlert.kommons

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

/** Typical locations on this system. */
public object SystemLocations {

    /**
     * Working directory, that is, the directory in which this binary is located.
     */
    public val Work: Path by lazy { FileSystems.getDefault().getPath(String.EMPTY).toAbsolutePath() }

    /**
     * Home directory of the logged-in user.
     */
    public val Home: Path by lazy { Paths.get(System.getProperty("user.home")) }

    /**
     * Directory, in which temporary data can be stored.
     */
    public val Temp: Path by lazy { Paths.get(System.getProperty("java.io.tmpdir")) }

    /** Directory of the running Java distribution. */
    public val JavaHome: Path by lazy { Paths.get(System.getProperty("java.home")) }
}
