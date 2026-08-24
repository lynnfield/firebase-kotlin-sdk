package dev.gitlive.firebase.firestore.internal

/**
 * wasmJs has no `dynamic`/transparent JS<->Any interop (unlike the old js(IR) target), so Firestore
 * document data has to be explicitly converted at the JS interop boundary: raw JS values coming out
 * of the Firestore JS SDK (DocumentSnapshot.data()/get(), QuerySnapshot, etc.) are normalized here
 * into Kotlin-native Map<String, Any?>/List<Any?>/String/Double/Boolean/null trees before they reach
 * the shared decode() pipeline (which, on wasmJs, works against Kotlin-native trees exactly like the
 * Android/JVM actuals do — see firebase-common-internal's wasmJs _decoders.kt). The reverse conversion
 * (toJsAny) builds a plain JS object/array from the Kotlin-native trees produced by the shared encode()
 * pipeline, for handing to setDoc/updateDoc/addDoc/Transaction.set/WriteBatch.set.
 *
 * Firestore's own special value types (Timestamp/GeoPoint/DocumentReference/FieldValue) are JS class
 * instances, not plain object literals — they're detected via prototype (isPlainJsObject returns false
 * for them) and passed through untouched in both directions, exactly as the shared encode/decode
 * pipeline expects (see isSpecialValue in _encoders.kt).
 */

private fun jsTypeOf(value: JsAny): String = js("typeof value")
private fun jsIsArray(value: JsAny): Boolean = js("Array.isArray(value)")
private fun jsIsPlainObject(value: JsAny): Boolean = js("Object.getPrototypeOf(value) === Object.prototype")
private fun jsObjectKeys(value: JsAny): JsArray<JsString> = js("Object.keys(value)")
private fun jsGet(value: JsAny, key: String): JsAny? = js("value[key]")
private fun jsSet(value: JsAny, key: String, v: JsAny?): Unit = js("value[key] = v")
private fun jsNewObject(): JsAny = js("({})")
private fun jsNewArray(): JsAny = js("([])")
private fun jsArrayPush(array: JsAny, v: JsAny?): Unit = js("array.push(v)")
private fun jsArrayLength(array: JsAny): Int = js("array.length")
private fun jsArrayGet(array: JsAny, index: Int): JsAny? = js("array[index]")

private fun jsAnyToDouble(value: JsAny): Double = js("value")
private fun jsAnyToBoolean(value: JsAny): Boolean = js("value")
private fun jsAnyToKotlinString(value: JsAny): String = js("value")
private fun doubleToJsAny(value: Double): JsAny = js("value")
private fun stringToJsAny(value: String): JsAny = js("value")
private fun booleanToJsAny(value: Boolean): JsAny = js("value")

internal fun JsAny?.toKotlin(): Any? {
    val value = this ?: return null
    return when (jsTypeOf(value)) {
        "string" -> jsAnyToKotlinString(value)
        "number" -> jsAnyToDouble(value)
        "boolean" -> jsAnyToBoolean(value)
        "undefined" -> null
        else -> when {
            jsIsArray(value) -> (0 until jsArrayLength(value)).map { jsArrayGet(value, it).toKotlin() }
            jsIsPlainObject(value) -> jsObjectKeys(value).toKotlinList().associate { key -> key to jsGet(value, key).toKotlin() }
            else -> value
        }
    }
}

internal fun Any?.toJsAny(): JsAny? = when (val value = this) {
    null -> null
    is String -> stringToJsAny(value)
    is Boolean -> booleanToJsAny(value)
    is Double -> doubleToJsAny(value)
    is Float -> doubleToJsAny(value.toDouble())
    is Int -> doubleToJsAny(value.toDouble())
    is Long -> doubleToJsAny(value.toDouble())
    is Short -> doubleToJsAny(value.toDouble())
    is Byte -> doubleToJsAny(value.toDouble())
    is Map<*, *> -> jsNewObject().also { obj ->
        value.forEach { (k, v) ->
            jsSet(obj, k as? String ?: k.toString(), v.toJsAny())
        }
    }
    is List<*> -> jsNewArray().also { array -> value.forEach { jsArrayPush(array, it.toJsAny()) } }
    else -> {
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        value as JsAny
    }
}

private fun JsArray<JsString>.toKotlinList(): List<String> {
    val result = mutableListOf<String>()
    for (i in 0 until length) {
        get(i)?.let { result.add(jsAnyToKotlinString(it)) }
    }
    return result
}

public fun <T : JsAny> JsArray<T>.toKotlinList(): List<T> {
    val result = mutableListOf<T>()
    for (i in 0 until length) {
        get(i)?.let { result.add(it) }
    }
    return result
}
