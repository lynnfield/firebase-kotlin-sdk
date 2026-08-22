package dev.gitlive.firebase.auth

import kotlin.js.JsArray
import kotlin.js.JsBoolean
import kotlin.js.JsNumber
import kotlin.js.JsString

internal fun jsObjectKeys(obj: JsAny): JsArray<JsString> = js("Object.keys(obj)")
internal fun jsPropertyGet(obj: JsAny, key: String): JsAny? = js("obj[key]")

internal fun JsAny?.toKotlinAny(): Any? = when (this) {
    null -> null
    is JsString -> toString()
    is JsBoolean -> toBoolean()
    is JsNumber -> toDouble()
    else -> this
}
