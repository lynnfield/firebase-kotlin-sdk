package dev.gitlive.firebase.firestore.internal

internal val SetOptions.js: JsAny
    get() = when (this) {
        is SetOptions.Merge -> mapOf("merge" to true)
        is SetOptions.Overwrite -> mapOf("merge" to false)
        is SetOptions.MergeFields -> mapOf("mergeFields" to fields)
        is SetOptions.MergeFieldPaths -> mapOf("mergeFields" to encodedFieldPaths)
    }.toJsAny()!!
