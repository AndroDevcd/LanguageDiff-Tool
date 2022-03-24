package com.rightpoint.parser

import com.rightpoint.util.Util
import com.rightpoint.util.replaceNewLineWithStringVersion
import java.io.File
import java.nio.file.FileSystems
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

abstract class Parser(
    val srcDir: String,
    val language: String
) {

    private val documentBuilder : DocumentBuilder

    init {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isCoalescing = true
        documentBuilder = factory.newDocumentBuilder()
    }

    protected var englishResources : Map<String, String?> =
        walk(FileSystems.getDefault().getPath(srcDir).toFile(), "values")
    protected var alternateResources : Map<String, String?> =
        walk(FileSystems.getDefault().getPath(srcDir).toFile(), "values-${language}")


    private lateinit var alternateResourceFullPath : String
    private lateinit var englishResourceFullPath : String
    lateinit var alternateResourceStringData : String
    lateinit var englishResourceStringData : String

    protected abstract fun parse()
    protected abstract fun finalize()

    fun run() {
        parse()
        finalize()
    }

    private fun walk(resFile: File, resourceDirectoryName: String): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        if (resFile.isDirectory) {
            for (file in resFile.listFiles()) {
                val map = walk(file, resourceDirectoryName)
                result.putAll(map)
            }
        } else {
            if (resFile.name == "strings.xml") {
                if (resFile.parentFile.name == resourceDirectoryName) {
                    return parseStringsResourceFile(resFile.absolutePath, resFile.parentFile.name != "values")
                }
            }
        }
        return result
    }

    private fun parseStringsResourceFile(
        filename: String,
        isAlternateResource: Boolean
    ): Map<String, String?> {

        val file = File(filename)
        val map = mutableMapOf<String, String?>()

        if (isAlternateResource) {
            alternateResourceFullPath = file.absolutePath
            alternateResourceStringData = file.inputStream().readBytes().toString(Charsets.UTF_8)
        } else {
            englishResourceFullPath = file.absolutePath
            englishResourceStringData = file.inputStream().readBytes().toString(Charsets.UTF_8)
        }

        try {
            val document = documentBuilder.parse(file)
            document.documentElement.normalize()
            val nodeList = document.getElementsByTagName("string")
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)

                val translatableAttribute = node.attributes.getNamedItem("translatable")
                if (translatableAttribute?.nodeValue == "false") {
                    continue
                }

                val nameAttribute = node.attributes.getNamedItem("name")
                var value = node.firstChild
                while (value != null && value.nodeValue == null) {
                    value = value.firstChild
                }

                map[nameAttribute.nodeValue] = value?.nodeValue
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("${e.message}")
        }
        return map
    }

    fun findAndUpdateAltStringResource(
        stringId: String,
        oldValue: String,
        newValue: String): Boolean {

        val oldResourceValue = "<string name=\"$stringId\">$oldValue</string>"

        if (alternateResourceStringData.contains(oldResourceValue)) {
            alternateResourceStringData = alternateResourceStringData.replace(
                oldResourceValue,
                "<string name=\"$stringId\">${newValue.replaceNewLineWithStringVersion()}</string>"
            )

            return true
        }

        return false
    }

    fun findAndUpdateEngStringResource(
        stringId: String,
        oldValue: String,
        newValue: String): Boolean {

        val oldResourceValue = "<string name=\"$stringId\">$oldValue</string>"

        if (englishResourceStringData.contains(oldResourceValue)) {
            englishResourceStringData = englishResourceStringData.replace(
                oldResourceValue,
                "<string name=\"$stringId\">${newValue.replaceNewLineWithStringVersion()}</string>"
            )

            return true
        }

        return false
    }

    fun addStringsToAlternateResData(newStrings: MutableMap<String, String>) {
        newStrings.toList().let {
            it.forEachIndexed { i, missingTranslation ->
                if(i == Util.FIRST_ITEM) {
                    alternateResourceStringData = alternateResourceStringData.replace("</resources>", "")
                }

                alternateResourceStringData += ("\t<string name=\"${missingTranslation.first}\">${missingTranslation.second}</string>\n")
            }

            if(!it.isEmpty())
                alternateResourceStringData += ("</resources>")
        }
    }

    fun updateAlternateResourceFile() {
        File(alternateResourceFullPath).bufferedWriter().use { out ->
            out.write(alternateResourceStringData)
        }
    }

    fun updateEnglishResourceFile() {
        File(englishResourceFullPath).bufferedWriter().use { out ->
            out.write(englishResourceStringData)
        }
    }

    fun parseErr(message: String) {
        throw ParseErr(message)
    }
}