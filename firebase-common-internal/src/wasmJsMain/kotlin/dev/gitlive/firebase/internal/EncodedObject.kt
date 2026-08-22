package dev.gitlive.firebase.internal

/**
 * Exposes the raw encoded [Map] for a platform-specific consumer, mirroring the `.android`
 * accessor. wasmJs works against Kotlin-native Map/List trees (see [asNativeMap]) rather than raw
 * JS objects, so the JsAny<->Map conversion happens in the consuming module (e.g. firebase-firestore),
 * not here.
 */
public val EncodedObject.wasmJs: Map<String, Any?> get() = getRaw()

@PublishedApi
internal actual fun Any.asNativeMap(): Map<*, *>? = this as? Map<*, *>
