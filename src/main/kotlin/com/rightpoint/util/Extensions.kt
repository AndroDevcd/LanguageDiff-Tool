package com.localization.tool.util

fun String.replaceNewLineWithStringVersion() : String {
    return this.replace("\n", "\\n")
}

fun String.getWordAccuracyPercent(str: String): Int {
    val longestWord = if (this.length > str.length) this.length.toDouble() else str.length.toDouble()
    val dst = Util.computeDistance(this, str).toDouble()

    return (100 - ((dst / longestWord) * 100)).toInt()
}
