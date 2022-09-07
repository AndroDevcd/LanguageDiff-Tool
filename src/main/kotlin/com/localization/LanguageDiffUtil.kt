@file:JvmName("LanguageDiffUtil")

package com.localization.tool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.localization.tool.parser.CsvExporter
import com.localization.tool.parser.CsvParser
import com.localization.tool.parser.MissingStringsParser

fun main(vararg args: String) = LanguageDiffer().main(args.toList())

private class LanguageDiffer : CliktCommand() {

    private val src: String by option("-s", "--src").default(".")
    private val language: String? by option("-l", "--lang", help = "Language")
    private val csv: String by option("-csv", help = "Csv file to compare against" ).default("")
    private val export: Boolean by option("-exportcsv", help = "Convert all translations to a csv export file" ).flag()


    override fun run() {
        if(language == null) return

        when {
            csv.isEmpty() && !export -> {
                MissingStringsParser(src, language!!).run()
            }
            export -> {
                CsvExporter(src, language!!).run()
            }
            else  -> {
                CsvParser(csv, src, language!!).run()
            }
        }
    }

}

