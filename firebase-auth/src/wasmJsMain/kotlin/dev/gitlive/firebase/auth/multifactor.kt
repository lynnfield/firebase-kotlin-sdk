package dev.gitlive.firebase.auth

import dev.gitlive.firebase.auth.externals.MultiFactorUser
import kotlinx.coroutines.await
import dev.gitlive.firebase.auth.externals.MultiFactorAssertion as JsMultiFactorAssertion
import dev.gitlive.firebase.auth.externals.MultiFactorInfo as JsMultiFactorInfo
import dev.gitlive.firebase.auth.externals.MultiFactorResolver as JsMultiFactorResolver
import dev.gitlive.firebase.auth.externals.MultiFactorSession as JsMultiFactorSession

public val MultiFactor.js: MultiFactorUser get() = native

public actual class MultiFactor(internal val native: MultiFactorUser) {
    public actual val enrolledFactors: List<MultiFactorInfo>
        get() = rethrow { native.enrolledFactors.toList().map { MultiFactorInfo(it) } }
    public actual suspend fun enroll(multiFactorAssertion: MultiFactorAssertion, displayName: String?): Unit = rethrow { native.enroll(multiFactorAssertion.js, displayName).await<JsAny?>() }
    public actual suspend fun getSession(): MultiFactorSession = rethrow { MultiFactorSession(native.getSession().await()) }
    public actual suspend fun unenroll(multiFactorInfo: MultiFactorInfo): Unit = rethrow { native.unenroll(multiFactorInfo.js).await<JsAny?>() }
    public actual suspend fun unenroll(factorUid: String): Unit = rethrow { native.unenroll(factorUid).await<JsAny?>() }
}

public val MultiFactorInfo.js: JsMultiFactorInfo get() = native

public actual class MultiFactorInfo(internal val native: JsMultiFactorInfo) {
    public actual val displayName: String?
        get() = rethrow { native.displayName }
    public actual val enrollmentTime: Double
        get() = rethrow { parseDateToEpochSeconds(native.enrollmentTime) }
    public actual val factorId: String
        get() = rethrow { native.factorId }
    public actual val uid: String
        get() = rethrow { native.uid }
}

public val MultiFactorAssertion.js: JsMultiFactorAssertion get() = native

public actual class MultiFactorAssertion(internal val native: JsMultiFactorAssertion) {
    public actual val factorId: String
        get() = rethrow { native.factorId }
}

public val MultiFactorSession.js: JsMultiFactorSession get() = native

public actual class MultiFactorSession(internal val native: JsMultiFactorSession)

public val MultiFactorResolver.js: JsMultiFactorResolver get() = native

public actual class MultiFactorResolver(internal val native: JsMultiFactorResolver) {
    public actual val auth: FirebaseAuth = rethrow { FirebaseAuth(native.auth) }
    public actual val hints: List<MultiFactorInfo> = rethrow { native.hints.toList().map { MultiFactorInfo(it) } }
    public actual val session: MultiFactorSession = rethrow { MultiFactorSession(native.session) }

    public actual suspend fun resolveSignIn(assertion: MultiFactorAssertion): AuthResult = rethrow { AuthResult(native.resolveSignIn(assertion.js).await()) }
}

/** Parses a JS Date-parseable ISO string (as returned by the Firebase Auth SDK) into epoch seconds. */
internal fun parseDateToEpochSeconds(dateString: String): Double = jsParseDateToEpochMillis(dateString) / 1000.0

private fun jsParseDateToEpochMillis(dateString: String): Double = js("new Date(dateString).getTime()")
