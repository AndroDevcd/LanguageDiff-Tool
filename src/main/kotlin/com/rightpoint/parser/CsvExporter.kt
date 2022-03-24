package com.rightpoint.parser

import com.rightpoint.util.Util.NO_TRANSLATION
import java.io.File

class CsvExporter(
    src: String,
    language: String
) : Parser(src, language) {

    var csvFileData: String = ""

    override fun parse() {
        csvFileData = "En-translation,${language}-translation,string-Id\n"
        englishResources.toList().let {
            if(it.isNotEmpty()) {
                it.forEachIndexed { i, item ->
                    csvFileData += "\"${item.second}\",\"${alternateResources[item.first] ?: NO_TRANSLATION}\",${item.first}\n"
                }
            }
        }

    }

    override fun finalize() {
        val outputFile = File("$srcDir${File.separator}string-translations.csv")
        println("created ${outputFile.name}")

        outputFile.bufferedWriter().use { out ->
            out.write(csvFileData)
            out.newLine()
        }
    }
}