/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.minutes

actual fun runTest(test: suspend CoroutineScope.() -> Unit) = kotlinx.coroutines.test.runTest(timeout = 5.minutes) { test() }
actual fun runBlockingTest(action: suspend CoroutineScope.() -> Unit) {
    kotlinx.coroutines.test.runTest { action() }
}

// wasmJs has no `dynamic`/json() JS-object builder, and the ported wasmJs decode/encode pipeline
// (see firebase-common-internal's wasmJs _decoders.kt/_encoders.kt) works against Kotlin-native
// Map/List trees rather than raw JS objects, so these mirror the Android/JVM actuals.
actual fun nativeMapOf(vararg pairs: Pair<Any, Any?>): Any = mapOf(*pairs)
actual fun nativeListOf(vararg elements: Any?): Any = listOf(*elements)
actual fun nativeAssertEquals(expected: Any?, actual: Any?) {
    kotlin.test.assertEquals(expected, actual)
}
