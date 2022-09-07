package com.localization.tool.parser

import java.io.File

class MissingStringsParser(
    src: String,
    language: String
) : Parser(src, language) {

    override fun parse() {
        val diff = englishResources.keys.minus(alternateResources.keys)

        println("\nDiff Report:\n--  Found ${diff.size} string translations missing in values-${language}/strings.xml")

        if(diff.size > 0) {
            val outputFile = File("$srcDir/missing-${language}-translations.xml")
            println("created${outputFile.createNewFile()}")

            outputFile.bufferedWriter().use { out ->
                out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                out.newLine()
                out.write("<resources>")
                out.newLine()
                for (key in diff) {
                    out.write("\t<string name=\"${key}\">${englishResources[key]}</string>")
                    out.newLine()
                }
                out.write("</resources>")
            }
        }
    }

    override fun finalize() { /* do nothing */ }
}