/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.externals.Auth
import dev.gitlive.firebase.auth.externals.getAuth
import dev.gitlive.firebase.auth.externals.applyActionCode
import dev.gitlive.firebase.auth.externals.confirmPasswordReset
import dev.gitlive.firebase.auth.externals.createUserWithEmailAndPassword
import dev.gitlive.firebase.auth.externals.sendPasswordResetEmail
import dev.gitlive.firebase.auth.externals.fetchSignInMethodsForEmail
import dev.gitlive.firebase.auth.externals.sendSignInLinkToEmail
import dev.gitlive.firebase.auth.externals.isSignInWithEmailLink
import dev.gitlive.firebase.auth.externals.signInWithEmailAndPassword
import dev.gitlive.firebase.auth.externals.signInWithCustomToken
import dev.gitlive.firebase.auth.externals.signInAnonymously
import dev.gitlive.firebase.auth.externals.signInWithCredential
import dev.gitlive.firebase.auth.externals.signInWithEmailLink
import dev.gitlive.firebase.auth.externals.signOut
import dev.gitlive.firebase.auth.externals.updateCurrentUser
import dev.gitlive.firebase.auth.externals.verifyPasswordResetCode
import dev.gitlive.firebase.auth.externals.checkActionCode
import dev.gitlive.firebase.auth.externals.connectAuthEmulator
import dev.gitlive.firebase.auth.externals.IdTokenResult
import dev.gitlive.firebase.js
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.js.JsArray
import kotlin.js.JsException
import kotlin.js.JsString
import dev.gitlive.firebase.auth.externals.AuthResult as JsAuthResult
import dev.gitlive.firebase.auth.externals.AdditionalUserInfo as JsAdditionalUserInfo

public actual val Firebase.auth: FirebaseAuth
    get() = rethrow { FirebaseAuth(getAuth()) }

public actual fun Firebase.auth(app: FirebaseApp): FirebaseAuth = rethrow { FirebaseAuth(getAuth(app.js)) }

public val FirebaseAuth.js: Auth get() = native

public actual class FirebaseAuth internal constructor(internal val native: Auth) {

    public actual val currentUser: FirebaseUser?
        get() = rethrow { native.currentUser?.let { FirebaseUser(it) } }

    public actual val authStateChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val unsubscribe = native.onAuthStateChanged {
            trySend(it?.let { FirebaseUser(it) })
        }
        awaitClose { unsubscribe() }
    }

    public actual val idTokenChanged: Flow<FirebaseUser?> get() = callbackFlow {
        val unsubscribe = native.onIdTokenChanged {
            trySend(it?.let { FirebaseUser(it) })
        }
        awaitClose { unsubscribe() }
    }

    public actual var languageCode: String
        get() = native.languageCode ?: ""
        set(value) {
            native.languageCode = value
        }

    public actual suspend fun applyActionCode(code: String): Unit = rethrow { applyActionCode(native, code).await<JsAny?>() }
    public actual suspend fun confirmPasswordReset(code: String, newPassword: String): Unit = rethrow { confirmPasswordReset(native, code, newPassword).await<JsAny?>() }

    public actual suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult = rethrow { AuthResult(createUserWithEmailAndPassword(native, email, password).await()) }

    public actual suspend fun fetchSignInMethodsForEmail(email: String): List<String> = rethrow { fetchSignInMethodsForEmail(native, email).await<JsArray<JsString>>().toList().map { it.toString() } }

    public actual suspend fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?): Unit = rethrow { sendPasswordResetEmail(native, email, actionCodeSettings?.toJsAny()).await<JsAny?>() }

    public actual suspend fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings): Unit = rethrow { sendSignInLinkToEmail(native, email, actionCodeSettings.toJsAny()).await<JsAny?>() }

    public actual fun isSignInWithEmailLink(link: String): Boolean = rethrow { isSignInWithEmailLink(native, link) }

    public actual suspend fun signInWithEmailAndPassword(email: String, password: String): AuthResult = rethrow { AuthResult(signInWithEmailAndPassword(native, email, password).await()) }

    public actual suspend fun signInWithCustomToken(token: String): AuthResult = rethrow { AuthResult(signInWithCustomToken(native, token).await()) }

    public actual suspend fun signInAnonymously(): AuthResult = rethrow { AuthResult(signInAnonymously(native).await()) }

    public actual suspend fun signInWithCredential(authCredential: AuthCredential): AuthResult = rethrow { AuthResult(signInWithCredential(native, authCredential.js).await()) }

    public actual suspend fun signInWithEmailLink(email: String, link: String): AuthResult = rethrow { AuthResult(signInWithEmailLink(native, email, link).await()) }

    public actual suspend fun signOut(): Unit = rethrow { signOut(native).await<JsAny?>() }

    public actual suspend fun updateCurrentUser(user: FirebaseUser): Unit = rethrow { updateCurrentUser(native, user.js).await<JsAny?>() }

    public actual suspend fun verifyPasswordResetCode(code: String): String = rethrow { verifyPasswordResetCode(native, code).await<JsString>().toString() }

    public actual suspend fun <T : ActionCodeResult> checkActionCode(code: String): T = rethrow {
        val result = checkActionCode(native, code).await<dev.gitlive.firebase.auth.externals.ActionCodeInfo>()
        @Suppress("UNCHECKED_CAST")
        return when (result.operation) {
            "EMAIL_SIGNIN" -> ActionCodeResult.SignInWithEmailLink
            "VERIFY_EMAIL" -> ActionCodeResult.VerifyEmail(result.data.email!!)
            "PASSWORD_RESET" -> ActionCodeResult.PasswordReset(result.data.email!!)
            "RECOVER_EMAIL" -> ActionCodeResult.RecoverEmail(result.data.email!!, result.data.previousEmail!!)
            "VERIFY_AND_CHANGE_EMAIL" -> ActionCodeResult.VerifyBeforeChangeEmail(
                result.data.email!!,
                result.data.previousEmail!!,
            )
            "REVERT_SECOND_FACTOR_ADDITION" -> ActionCodeResult.RevertSecondFactorAddition(
                result.data.email!!,
                result.data.multiFactorInfo?.let { MultiFactorInfo(it) },
            )
            else -> throw UnsupportedOperationException(result.operation)
        } as T
    }

    public actual fun useEmulator(host: String, port: Int): Unit = rethrow { connectAuthEmulator(native, "http://$host:$port") }
}

public val AuthResult.js: JsAuthResult get() = native

public actual class AuthResult(internal val native: JsAuthResult) {
    public actual val user: FirebaseUser?
        get() = rethrow { native.user?.let { FirebaseUser(it) } }
    public actual val credential: AuthCredential?
        get() = rethrow { native.credential?.let { AuthCredential(it) } }
    public actual val additionalUserInfo: AdditionalUserInfo?
        get() = rethrow { native.additionalUserInfo?.let { AdditionalUserInfo(it) } }
}

public val AdditionalUserInfo.js: JsAdditionalUserInfo get() = native

public actual class AdditionalUserInfo(
    internal val native: JsAdditionalUserInfo,
) {
    public actual val providerId: String?
        get() = native.providerId
    public actual val username: String?
        get() = native.username
    public actual val profile: Map<String, Any?>?
        get() = rethrow {
            val profile = native.profile ?: return@rethrow null
            jsObjectKeys(profile).toList().associate { key -> key.toString() to jsPropertyGet(profile, key.toString()).toKotlinAny() }
        }
    public actual val isNewUser: Boolean
        get() = native.newUser
}

public val AuthTokenResult.js: IdTokenResult get() = native

public actual class AuthTokenResult(internal val native: IdTokenResult) {
//    actual val authTimestamp: Long
//        get() = native.authTime
    public actual val claims: Map<String, Any>
        get() = jsObjectKeys(native.claims).toList().mapNotNull { key ->
            jsPropertyGet(native.claims, key.toString()).toKotlinAny()?.let { key.toString() to it }
        }.toMap()

//    actual val expirationTimestamp: Long
//        get() = android.expirationTime
//    actual val issuedAtTimestamp: Long
//        get() = native.issuedAtTime
    public actual val signInProvider: String?
        get() = native.signInProvider
    public actual val token: String?
        get() = native.token
}

internal fun ActionCodeSettings.toJsAny(): JsAny = actionCodeSettingsToJsAny(
    url = url,
    android = androidPackageName?.let { androidPackageNameToJsAny(it.packageName, it.installIfNotAvailable, it.minimumVersion) },
    linkDomain = linkDomain,
    dynamicLinkDomain = dynamicLinkDomain,
    handleCodeInApp = canHandleCodeInApp,
    ios = iOSBundleId?.let { iosBundleIdToJsAny(it) },
)

private fun actionCodeSettingsToJsAny(
    url: String,
    android: JsAny?,
    linkDomain: String?,
    dynamicLinkDomain: String?,
    handleCodeInApp: Boolean,
    ios: JsAny?,
): JsAny = js(
    "({ url: url, android: android, linkDomain: linkDomain, dynamicLinkDomain: dynamicLinkDomain, handleCodeInApp: handleCodeInApp, ios: ios })",
)

private fun androidPackageNameToJsAny(
    packageName: String,
    installApp: Boolean,
    minimumVersion: String?,
): JsAny = js(
    "({ packageName: packageName, installApp: installApp, minimumVersion: minimumVersion })",
)

private fun iosBundleIdToJsAny(bundleId: String): JsAny = js("({ bundleId: bundleId })")

public actual open class FirebaseAuthException(internal val authCode: String?, cause: Throwable) : FirebaseException(authCode, cause)
public actual val FirebaseAuthException.code: String? get() = authCode
public actual open class FirebaseAuthActionCodeException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthEmailException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthInvalidCredentialsException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthWeakPasswordException(code: String?, cause: Throwable) : FirebaseAuthInvalidCredentialsException(code, cause)
public actual open class FirebaseAuthInvalidUserException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthMultiFactorException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthRecentLoginRequiredException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthUserCollisionException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)
public actual open class FirebaseAuthWebException(code: String?, cause: Throwable) : FirebaseAuthException(code, cause)

internal inline fun <T, R> T.rethrow(function: T.() -> R): R = dev.gitlive.firebase.auth.rethrow { function() }

private inline fun <R> rethrow(function: () -> R): R {
    try {
        return function()
    } catch (e: Exception) {
        throw e
    } catch (e: Throwable) {
        throw errorToException(e)
    }
}

private fun errorToException(cause: Throwable): Throwable {
    val jsError = (cause as? JsException)?.thrownValue
    val code = jsError?.let { jsPropertyGet(it, "code") }?.let { (it as? JsString)?.toString() }?.lowercase()
    return when (code) {
        "auth/invalid-user-token" -> FirebaseAuthInvalidUserException(code, cause)
        "auth/requires-recent-login" -> FirebaseAuthRecentLoginRequiredException(code, cause)
        "auth/user-disabled" -> FirebaseAuthInvalidUserException(code, cause)
        "auth/user-token-expired" -> FirebaseAuthInvalidUserException(code, cause)
        "auth/web-storage-unsupported" -> FirebaseAuthWebException(code, cause)
        "auth/network-request-failed" -> FirebaseNetworkException(code, cause)
        "auth/timeout" -> FirebaseNetworkException(code, cause)
        "auth/weak-password" -> FirebaseAuthWeakPasswordException(code, cause)
        "auth/invalid-credential",
        "auth/invalid-verification-code",
        "auth/missing-verification-code",
        "auth/invalid-verification-id",
        "auth/missing-verification-id",
        "auth/wrong-password",
        -> FirebaseAuthInvalidCredentialsException(code, cause)
        "auth/maximum-second-factor-count-exceeded",
        "auth/second-factor-already-in-use",
        -> FirebaseAuthMultiFactorException(code, cause)
        "auth/credential-already-in-use" -> FirebaseAuthUserCollisionException(code, cause)
        "auth/email-already-in-use" -> FirebaseAuthUserCollisionException(code, cause)
        "auth/invalid-email" -> FirebaseAuthEmailException(code, cause)
        else -> {
            println("Unknown error code for auth exception: $code")
            FirebaseAuthException(code, cause)
        }
    }
}
