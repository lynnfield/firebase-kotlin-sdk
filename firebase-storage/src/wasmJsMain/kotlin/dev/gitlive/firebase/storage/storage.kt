/*
 * Copyright (c) 2023 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.storage

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.js
import dev.gitlive.firebase.storage.externals.JsFile
import dev.gitlive.firebase.storage.externals.JsUint8Array
import dev.gitlive.firebase.storage.externals.ListOptions
import dev.gitlive.firebase.storage.externals.SettableMetadata
import dev.gitlive.firebase.storage.externals.UploadMetadata
import dev.gitlive.firebase.storage.externals.connectStorageEmulator
import dev.gitlive.firebase.storage.externals.deleteObject
import dev.gitlive.firebase.storage.externals.getBytes
import dev.gitlive.firebase.storage.externals.getDownloadURL
import dev.gitlive.firebase.storage.externals.getMetadata
import dev.gitlive.firebase.storage.externals.getStorage
import dev.gitlive.firebase.storage.externals.list
import dev.gitlive.firebase.storage.externals.listAll
import dev.gitlive.firebase.storage.externals.ref
import dev.gitlive.firebase.storage.externals.updateMetadata
import dev.gitlive.firebase.storage.externals.uploadBytes
import dev.gitlive.firebase.storage.externals.uploadBytesResumable
import kotlinx.coroutines.await
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

public actual val Firebase.storage: FirebaseStorage
    get() = FirebaseStorage(getStorage())

public actual fun Firebase.storage(url: String): FirebaseStorage = FirebaseStorage(getStorage(null, url))

public actual fun Firebase.storage(app: FirebaseApp): FirebaseStorage = FirebaseStorage(getStorage(app.js))

public actual fun Firebase.storage(app: FirebaseApp, url: String): FirebaseStorage = FirebaseStorage(getStorage(app.js, url))

public val FirebaseStorage.js: dev.gitlive.firebase.storage.externals.FirebaseStorage get() = native

public actual class FirebaseStorage(internal val native: dev.gitlive.firebase.storage.externals.FirebaseStorage) {
    public actual val maxOperationRetryTime: Duration = native.maxOperationRetryTime.milliseconds
    public actual val maxUploadRetryTime: Duration = native.maxUploadRetryTime.milliseconds

    public actual fun setMaxOperationRetryTime(maxOperationRetryTime: Duration) {
        native.maxOperationRetryTime = maxOperationRetryTime.toDouble(DurationUnit.MILLISECONDS)
    }

    public actual fun setMaxUploadRetryTime(maxUploadRetryTime: Duration) {
        native.maxUploadRetryTime = maxUploadRetryTime.toDouble(DurationUnit.MILLISECONDS)
    }

    public actual fun useEmulator(host: String, port: Int) {
        connectStorageEmulator(native, host, port.toDouble())
    }

    public actual val reference: StorageReference get() = StorageReference(ref(native))

    public actual fun reference(location: String): StorageReference = rethrow { StorageReference(ref(native, location)) }

    public actual fun getReferenceFromUrl(fullUrl: String): StorageReference = rethrow { StorageReference(ref(native, fullUrl)) }
}

public val StorageReference.js: dev.gitlive.firebase.storage.externals.StorageReference get() = native

public actual class StorageReference(internal val native: dev.gitlive.firebase.storage.externals.StorageReference) {
    public actual val path: String get() = native.fullPath
    public actual val name: String get() = native.name
    public actual val bucket: String get() = native.bucket
    public actual val parent: StorageReference? get() = native.parent?.let { StorageReference(it) }
    public actual val root: StorageReference get() = StorageReference(native.root)
    public actual val storage: FirebaseStorage get() = FirebaseStorage(native.storage)

    public actual suspend fun getMetadata(): FirebaseStorageMetadata? = rethrow { getMetadata(native).await<dev.gitlive.firebase.storage.externals.FullMetadata>().toFirebaseStorageMetadata() }

    public actual suspend fun getData(maxDownloadSizeBytes: Long): Data = rethrow { Data(jsArrayBufferToByteArray(getBytes(native, maxDownloadSizeBytes.toDouble()).await<JsAny>())) }

    public actual suspend fun updateMetadata(metadata: FirebaseStorageMetadata): FirebaseStorageMetadata? = rethrow { updateMetadata(native, metadata.toStorageMetadata()).await<dev.gitlive.firebase.storage.externals.FullMetadata>().toFirebaseStorageMetadata() }

    public actual fun child(path: String): StorageReference = StorageReference(ref(native, path))

    public actual suspend fun delete(): Unit = rethrow { deleteObject(native).await<JsAny?>() }

    public actual suspend fun getDownloadUrl(): String = rethrow { getDownloadURL(native).await<JsString>().toString() }

    public actual suspend fun list(maxResults: Int, pageToken: String?): ListResult = rethrow {
        ListResult(
            list(
                native,
                newListOptions(maxResults.toDouble(), pageToken),
            ).await(),
        )
    }

    public actual suspend fun listAll(): ListResult = rethrow { ListResult(listAll(native).await()) }

    public actual suspend fun putFile(file: File, metadata: FirebaseStorageMetadata?): Unit = rethrow { uploadBytes(native, file, metadata?.toStorageMetadata()).await() }

    public actual suspend fun putData(data: Data, metadata: FirebaseStorageMetadata?): Unit = rethrow { uploadBytes(native, byteArrayToJsUint8Array(data.data), metadata?.toStorageMetadata()).await() }

    public actual fun putDataResumable(data: Data, metadata: FirebaseStorageMetadata?): ProgressFlow = rethrow {
        val uploadTask = uploadBytesResumable(native, byteArrayToJsUint8Array(data.data), metadata?.toStorageMetadata())

        val flow = callbackFlow {
            val unsubscribe = uploadTask.on(
                "state_changed",
                {
                    when (it.state) {
                        "paused" -> trySend(Progress.Paused(it.bytesTransferred, it.totalBytes))
                        "running" -> trySend(Progress.Running(it.bytesTransferred, it.totalBytes))
                        "canceled" -> cancel()
                        "success", "error" -> Unit
                        else -> TODO("Unknown state ${it.state}")
                    }
                },
                { close(errorToException(it)) },
                { close() },
            )
            awaitClose { unsubscribe() }
        }

        return object : ProgressFlow {
            override suspend fun collect(collector: FlowCollector<Progress>) = collector.emitAll(flow)
            override fun pause() = uploadTask.pause().run {}
            override fun resume() = uploadTask.resume().run {}
            override fun cancel() = uploadTask.cancel().run {}
        }
    }

    public actual fun putFileResumable(file: File, metadata: FirebaseStorageMetadata?): ProgressFlow = rethrow {
        val uploadTask = uploadBytesResumable(native, file, metadata?.toStorageMetadata())

        val flow = callbackFlow {
            val unsubscribe = uploadTask.on(
                "state_changed",
                {
                    when (it.state) {
                        "paused" -> trySend(Progress.Paused(it.bytesTransferred, it.totalBytes))
                        "running" -> trySend(Progress.Running(it.bytesTransferred, it.totalBytes))
                        "canceled" -> cancel()
                        "success", "error" -> Unit
                        else -> TODO("Unknown state ${it.state}")
                    }
                },
                { close(errorToException(it)) },
                { close() },
            )
            awaitClose { unsubscribe() }
        }

        return object : ProgressFlow {
            override suspend fun collect(collector: FlowCollector<Progress>) = collector.emitAll(flow)
            override fun pause() = uploadTask.pause().run {}
            override fun resume() = uploadTask.resume().run {}
            override fun cancel() = uploadTask.cancel().run {}
        }
    }
}

public actual class ListResult(js: dev.gitlive.firebase.storage.externals.ListResult) {
    public actual val prefixes: List<StorageReference> = js.prefixes.toList().map { StorageReference(it) }
    public actual val items: List<StorageReference> = js.items.toList().map { StorageReference(it) }
    public actual val pageToken: String? = js.nextPageToken
}

public actual typealias File = JsFile
public actual val File.name: String get() = name
public actual class Data(public val data: ByteArray)

public actual open class FirebaseStorageException(code: String, cause: Throwable) : FirebaseException(code, cause)

internal inline fun <R> rethrow(function: () -> R): R {
    try {
        return function()
    } catch (e: Exception) {
        throw errorToException(e)
    }
}

internal fun errorToException(cause: Exception) = FirebaseStorageException(cause.message.orEmpty().lowercase(), cause)

internal fun errorToException(error: dev.gitlive.firebase.storage.externals.StorageError) = FirebaseStorageException(error.code.lowercase(), Exception(error.message))

private fun newListOptions(maxResults: Double, pageToken: String?): ListOptions = js(
    "({ maxResults: maxResults, pageToken: pageToken })",
)

internal fun UploadMetadata.toFirebaseStorageMetadata(): FirebaseStorageMetadata {
    val sdkMetadata = this
    return storageMetadata {
        md5Hash = sdkMetadata.md5Hash
        cacheControl = sdkMetadata.cacheControl
        contentDisposition = sdkMetadata.contentDisposition
        contentEncoding = sdkMetadata.contentEncoding
        contentLanguage = sdkMetadata.contentLanguage
        contentType = sdkMetadata.contentType
        customMetadata = sdkMetadata.customMetadata?.let { jsObjectToStringMap(it) }.orEmpty().toMutableMap()
    }
}

internal fun FirebaseStorageMetadata.toStorageMetadata(): SettableMetadata = newSettableMetadata(
    cacheControl = cacheControl,
    contentDisposition = contentDisposition,
    contentEncoding = contentEncoding,
    contentLanguage = contentLanguage,
    contentType = contentType,
    customMetadata = newJsObjectFromStringMap(customMetadata),
)

private fun newSettableMetadata(
    cacheControl: String?,
    contentDisposition: String?,
    contentEncoding: String?,
    contentLanguage: String?,
    contentType: String?,
    customMetadata: JsAny,
): SettableMetadata = js(
    "({ cacheControl: cacheControl, contentDisposition: contentDisposition, contentEncoding: contentEncoding, contentLanguage: contentLanguage, contentType: contentType, customMetadata: customMetadata })",
)

private fun newJsObjectFromStringMap(map: Map<String, String>): JsAny = newJsObject().also { obj ->
    map.forEach { (key, value) -> setJsProperty(obj, key, value.toJsString()) }
}

private fun jsObjectToStringMap(obj: JsAny): Map<String, String> = jsObjectKeys(obj).toList().associate { key ->
    val keyString = key.toString()
    keyString to (getJsProperty(obj, keyString)?.toString().orEmpty())
}

private fun newJsObject(): JsAny = js("({})")

private fun setJsProperty(obj: JsAny, key: String, value: JsAny?) {
    js("obj[key] = value;")
}

private fun getJsProperty(obj: JsAny, key: String): JsAny? = js("obj[key]")

private fun jsObjectKeys(obj: JsAny): kotlin.js.JsArray<JsString> = js("Object.keys(obj)")

private fun byteArrayToJsUint8Array(bytes: ByteArray): JsUint8Array = dev.gitlive.firebase.storage.externals.newJsUint8Array(bytes.size).also { array ->
    bytes.forEachIndexed { index, byte -> dev.gitlive.firebase.storage.externals.setByteAt(array, index, byte.toInt() and 0xFF) }
}

private fun jsArrayBufferToByteArray(buffer: JsAny): ByteArray {
    val array = dev.gitlive.firebase.storage.externals.arrayBufferToJsUint8Array(buffer)
    return ByteArray(array.length) { dev.gitlive.firebase.storage.externals.getByteAt(array, it).toByte() }
}
