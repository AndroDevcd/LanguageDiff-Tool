package com.localization.tool.parser

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.localization.tool.util.Util
import com.localization.tool.util.Util.NO_TRANSLATION
import com.localization.tool.util.getWordAccuracyPercent
import java.io.File
import java.nio.file.FileSystems

class CsvParser(
    val csv: String,
    src: String,
    language: String
) : Parser(src, language) {

    var skippedEnglishTranslations : MutableMap<String, String> = mutableMapOf()
    var skippedAltResourceTranslations : MutableMap<String, String> = mutableMapOf()
    var missingStrings : MutableMap<String, String> = mutableMapOf()
    val englishCSVStrings = arrayListOf<String>()
    val altResourceCSVStrings = arrayListOf<String>()
    val stringResIds = arrayListOf<String>()
    var skippedStrings = 0
    var updatedStrings = 0
    var createdStrings = 0
    var simpleCSV = true

    override fun parse() {
       parseCSVFile()

        if(simpleCSV) {
            processSimpleCsv()
        } else {
            processAdvancedCsv()
        }
    }

    /**
     * We require the csv to be formatted as follows
     *
     * Simple CSV Format:
     *
     *       [ English ]       :      [ Spanish ]
     * [ english translation]    [ spanish translation]
     * [           ...       ]    [         ...        ]
     *
     * or
     *
     * Advanced CSV Format:
     *
     *          [ English ]      :      [ Spanish ]       :      [ String Res Id ]
     *   [ english translation]    [ spanish translation]    [ home_screen_greeting ]
     *  [         ...         ]    [         ...        ]    [         ...        ]
     */
    fun parseCSVFile() {
        csvReader().open(FileSystems.getDefault().getPath(srcDir).toString() + File.separator + csv) {
            readAllAsSequence().forEachIndexed { i, row ->
                when {
                    i == Util.FIRST_ITEM -> {
                        if(row.size > 2) simpleCSV = false
                        else if(row.size != 2) {
                            parseErr("expected column size must be 2, format for csv must be [english-str, alt-res-str] or [str-res-id, english-str, alt-res-str]")
                        }
                    }
                    simpleCSV -> {
                        englishCSVStrings.add(row[Util.ENGLISH_COLUMN])
                        altResourceCSVStrings.add(row[Util.ALTERNATE_LANGUAGE_COLUMN])
                    }
                    else -> { // complexCsv format [stringId, english string, alternate res string]
                        stringResIds.add(row[Util.STRING_RES_ID_COLUMN])
                        englishCSVStrings.add(row[Util.ENGLISH_COLUMN])
                        altResourceCSVStrings.add(row[Util.ALTERNATE_LANGUAGE_COLUMN])
                    }
                }
            }
        }
    }

    fun processAdvancedCsv() {
        stringResIds.forEachIndexed { i, stringId ->
            val engStringValue = englishCSVStrings[i]
            val altResStringValue = altResourceCSVStrings[i]

            if(stringIdExists(stringId, englishResources)) {

                val oldValue = englishResources[stringId] ?: ""
                if (findAndUpdateEngStringResource(
                        stringId = stringId,
                        oldValue = oldValue,
                        newValue = engStringValue
                    )) {
                    if(oldValue != engStringValue)
                        updatedStrings++
                } else {
                    logSkippedEnglishTranslation(stringId, engStringValue)
                }
            }

            if(altResStringValue != NO_TRANSLATION) {
                if (stringIdExists(stringId, alternateResources)) {

                    val oldValue = alternateResources[stringId] ?: ""
                    if (findAndUpdateAltStringResource(
                            stringId = stringId,
                            oldValue = oldValue,
                            newValue = altResStringValue
                        )
                    ) {
                        if(oldValue != altResStringValue)
                            updatedStrings++
                    } else {
                        logSkippedAltResTranslation(stringId, i)
                    }
                } else {
                    logMissingTranslation(stringId, i, altResourceCSVStrings)
                }
            }
        }
    }

    fun processSimpleCsv() {
        // first we need to loop through all the english strings that need translations
        englishCSVStrings.forEachIndexed { i, stringValue ->
            if (!stringValue.isEmpty()) {
                val englishResNames = getAllStringsWithValue(stringValue, englishResources)
                if (englishResNames.isEmpty()) {
                    logSkippedAltResTranslation(stringValue, i)
                }

                // we then find all the english resources with the string "name" that matches the csv value and update
                // all spanish translations with the new required value
                englishResNames.forEach { stringName ->
                    var oldValue: String? = alternateResources.get(stringName)

                    if ((oldValue) == null) {
                        logMissingTranslation(stringName, i, altResourceCSVStrings)
                    } else {
                        if (oldValue != altResourceCSVStrings[i]) {
                            if (findAndUpdateAltStringResource(
                                    stringId = stringName,
                                    oldValue = oldValue,
                                    newValue = altResourceCSVStrings[i]
                            )) {
                                updatedStrings++
                            } else {
                                logSkippedAltResTranslation(stringValue, i)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finalize() {
        addStringsToAlternateResData(missingStrings)


        println("\nDiff Report:\n--  Updated $updatedStrings string(s), skipped $skippedStrings strings(s), and created $createdStrings string(s)")
        updateAlternateResourceFile()
        updateEnglishResourceFile()


        createSkippedTranslationFile(
            fileName = "skipped-${language}-translations",
            skippedTranslationMap = skippedAltResourceTranslations,
            columnName1 = "english",
            columnName2 = "${language}-translation"
        )

        createSkippedTranslationFile(
            fileName = "skipped-english-translations",
            skippedTranslationMap = skippedEnglishTranslations,
            columnName1 = "stringId",
            columnName2 = "english-translation"
        )
    }

    fun createSkippedTranslationFile(
        fileName: String,
        skippedTranslationMap: MutableMap<String, String>,
        columnName1: String,
        columnName2: String) {

        skippedTranslationMap.toList().let { translationsList ->
            if (translationsList.isNotEmpty()) {
                val skippedTranslationsFile = File("$fileName.csv")
                skippedTranslationsFile.createNewFile()
                skippedTranslationsFile.bufferedWriter().use { out ->
                    out.write("$columnName1,$columnName2")
                    out.newLine()

                    translationsList.forEach {
                        out.write("${it.first},${it.second}")
                        out.newLine()
                    }
                }
            }
        }
    }

    private fun getAllStringsWithValue(stringValue: String, resource: Map<String, String?>) : Set<String> {
        return resource.filterValues {
            if (it != null) {
                // because there may be typos in the csv for the direct english translation we want a very close high probability equvalent
                it.toLowerCase().trim()
                    .getWordAccuracyPercent(stringValue.toLowerCase().trim()) >= 90
            } else false
        }.keys
    }

    private fun stringIdExists(stringId: String, resource: Map<String, String?>) : Boolean {
        return resource.containsKey(stringId)
    }

    private fun logSkippedEnglishTranslation(stringId: String, englishValue: String) {
        skippedStrings++;
        skippedEnglishTranslations.put(stringId, englishValue)
    }

    private fun logSkippedAltResTranslation(skippedTranslation: String, translationIndex: Int) {
        skippedStrings++;
        skippedAltResourceTranslations.put(skippedTranslation, altResourceCSVStrings[translationIndex])
    }

    private fun logMissingTranslation(stringId: String, translationIndex: Int, requiredTranslationStrings: ArrayList<String>) {
        createdStrings++;
        missingStrings.put(stringId, requiredTranslationStrings[translationIndex])
    }
}