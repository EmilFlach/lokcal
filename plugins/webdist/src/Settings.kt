package com.emilflach.lokcal.plugins.webdist

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface WebDistSettings {
    /** Port for `serveWeb`. */
    val port: Int get() = 8099
}
