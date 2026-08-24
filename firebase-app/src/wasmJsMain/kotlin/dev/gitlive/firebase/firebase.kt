/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import dev.gitlive.firebase.externals.deleteApp
import dev.gitlive.firebase.externals.getApp
import dev.gitlive.firebase.externals.getApps
import dev.gitlive.firebase.externals.initializeApp
import kotlinx.coroutines.await
import dev.gitlive.firebase.externals.FirebaseApp as JsFirebaseApp

public actual val Firebase.app: FirebaseApp
    get() = FirebaseApp(getApp())

public actual fun Firebase.app(name: String): FirebaseApp = FirebaseApp(getApp(name))

public actual fun Firebase.initialize(context: Any?): FirebaseApp? = throw UnsupportedOperationException("Cannot initialize firebase without options in JS")

public actual fun Firebase.initialize(context: Any?, options: FirebaseOptions, name: String): FirebaseApp = FirebaseApp(initializeApp(options.toJsAny(), name))

public actual fun Firebase.initialize(context: Any?, options: FirebaseOptions): FirebaseApp = FirebaseApp(initializeApp(options.toJsAny()))

public val FirebaseApp.js: JsFirebaseApp get() = native

public actual class FirebaseApp internal constructor(internal val native: JsFirebaseApp) {
    public actual val name: String
        get() = native.name
    public actual val options: FirebaseOptions
        get() = native.options.run {
            FirebaseOptions(appId, apiKey, databaseURL, gaTrackingId, storageBucket, projectId, messagingSenderId, authDomain)
        }

    public actual suspend fun delete() {
        deleteApp(native).await<JsAny?>()
    }
}

public actual fun Firebase.apps(context: Any?): List<FirebaseApp> = getApps().toList().map { FirebaseApp(it) }

private fun FirebaseOptions.toJsAny(): JsAny = firebaseOptionsToJsAny(
    apiKey = apiKey,
    appId = applicationId,
    databaseURL = databaseUrl,
    storageBucket = storageBucket,
    projectId = projectId,
    gaTrackingId = gaTrackingId,
    messagingSenderId = gcmSenderId,
    authDomain = authDomain,
)

private fun firebaseOptionsToJsAny(
    apiKey: String,
    appId: String,
    databaseURL: String?,
    storageBucket: String?,
    projectId: String?,
    gaTrackingId: String?,
    messagingSenderId: String?,
    authDomain: String?,
): JsAny = js(
    "({ apiKey: apiKey, appId: appId, databaseURL: databaseURL, storageBucket: storageBucket, projectId: projectId, gaTrackingId: gaTrackingId, messagingSenderId: messagingSenderId, authDomain: authDomain })",
)

public actual open class FirebaseException(code: String?, cause: Throwable) : Exception("$code: ${cause.message}", cause)
public actual open class FirebaseNetworkException(code: String?, cause: Throwable) : FirebaseException(code, cause)
public actual open class FirebaseTooManyRequestsException(code: String?, cause: Throwable) : FirebaseException(code, cause)
public actual open class FirebaseApiNotAvailableException(code: String?, cause: Throwable) : FirebaseException(code, cause)
