package dev.gitlive.firebase.storage.externals

/** The browser's global `File` type (a `Blob` subtype). Minimal surface actually used here. */
public external class JsFile : JsAny {
    public val name: String
}

/**
 * The browser's global `Uint8Array` type. Kotlin/Wasm doesn't expose `org.khronos.webgl.*`
 * (unlike the js(IR) target), so this module binds directly to the browser global instead.
 */
public external class JsUint8Array : JsAny {
    public val length: Int
}

public fun newJsUint8Array(length: Int): JsUint8Array = js("new Uint8Array(length)")
public fun getByteAt(array: JsUint8Array, index: Int): Int = js("array[index]")
public fun setByteAt(array: JsUint8Array, index: Int, value: Int) {
    js("array[index] = value;")
}
public fun arrayBufferToJsUint8Array(buffer: JsAny): JsUint8Array = js("new Uint8Array(buffer)")
