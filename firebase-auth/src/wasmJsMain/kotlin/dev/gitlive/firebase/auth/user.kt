package dev.gitlive.firebase.auth

import dev.gitlive.firebase.auth.externals.*
import kotlinx.coroutines.await
import dev.gitlive.firebase.auth.externals.UserInfo as JsUserInfo

public val FirebaseUser.js: User get() = native

public actual class FirebaseUser internal constructor(internal val native: User) {
    public actual val uid: String
        get() = rethrow { native.uid }
    public actual val displayName: String?
        get() = rethrow { native.displayName }
    public actual val email: String?
        get() = rethrow { native.email }
    public actual val phoneNumber: String?
        get() = rethrow { native.phoneNumber }
    public actual val photoURL: String?
        get() = rethrow { native.photoURL }
    public actual val isAnonymous: Boolean
        get() = rethrow { native.isAnonymous }
    public actual val isEmailVerified: Boolean
        get() = rethrow { native.emailVerified }
    public actual val metaData: UserMetaData?
        get() = rethrow { UserMetaData(native.metadata) }
    public actual val multiFactor: MultiFactor
        get() = rethrow { MultiFactor(multiFactor(native)) }
    public actual val providerData: List<UserInfo>
        get() = rethrow { native.providerData.toList().map { UserInfo(it) } }
    public actual val providerId: String
        get() = rethrow { native.providerId }
    public actual suspend fun delete(): Unit = rethrow { native.delete().await<JsAny?>() }
    public actual suspend fun reload(): Unit = rethrow { native.reload().await<JsAny?>() }
    public actual suspend fun getIdToken(forceRefresh: Boolean): String? = rethrow { native.getIdToken(forceRefresh).await<kotlin.js.JsString>().toString() }
    public actual suspend fun getIdTokenResult(forceRefresh: Boolean): AuthTokenResult = rethrow { AuthTokenResult(getIdTokenResult(native, forceRefresh).await()) }
    public actual suspend fun linkWithCredential(credential: AuthCredential): AuthResult = rethrow { AuthResult(linkWithCredential(native, credential.js).await()) }
    public actual suspend fun reauthenticate(credential: AuthCredential): Unit = rethrow {
        reauthenticateWithCredential(native, credential.js).await<AuthResult>()
    }
    public actual suspend fun reauthenticateAndRetrieveData(credential: AuthCredential): AuthResult = rethrow { AuthResult(reauthenticateWithCredential(native, credential.js).await()) }

    public actual suspend fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?): Unit = rethrow { sendEmailVerification(native, actionCodeSettings?.toJsAny()).await<JsAny?>() }
    public actual suspend fun unlink(provider: String): FirebaseUser? = rethrow { FirebaseUser(unlink(native, provider).await()) }
    public actual suspend fun updateEmail(email: String): Unit = rethrow { updateEmail(native, email).await<JsAny?>() }
    public actual suspend fun updatePassword(password: String): Unit = rethrow { updatePassword(native, password).await<JsAny?>() }
    public actual suspend fun updatePhoneNumber(credential: PhoneAuthCredential): Unit = rethrow { updatePhoneNumber(native, credential.js).await<JsAny?>() }
    public actual suspend fun updateProfile(displayName: String?, photoUrl: String?): Unit = rethrow {
        updateProfile(native, profileUpdateRequestToJsAny(displayName, photoUrl)).await<JsAny?>()
    }
    public actual suspend fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?): Unit = rethrow { verifyBeforeUpdateEmail(native, newEmail, actionCodeSettings?.toJsAny()).await<JsAny?>() }
}

private fun profileUpdateRequestToJsAny(displayName: String?, photoURL: String?): JsAny = js(
    "({ displayName: displayName, photoURL: photoURL })",
)

public val UserInfo.js: JsUserInfo get() = native

public actual class UserInfo(internal val native: JsUserInfo) {
    public actual val displayName: String?
        get() = rethrow { native.displayName }
    public actual val email: String?
        get() = rethrow { native.email }
    public actual val phoneNumber: String?
        get() = rethrow { native.phoneNumber }
    public actual val photoURL: String?
        get() = rethrow { native.photoURL }
    public actual val providerId: String
        get() = rethrow { native.providerId }
    public actual val uid: String
        get() = rethrow { native.uid }
}

public val UserMetaData.js: UserMetadata get() = native

public actual class UserMetaData(internal val native: UserMetadata) {
    public actual val creationTime: Double?
        get() = rethrow { native.creationTime?.let { parseDateToEpochSeconds(it) } }
    public actual val lastSignInTime: Double?
        get() = rethrow { native.lastSignInTime?.let { parseDateToEpochSeconds(it) } }
}
