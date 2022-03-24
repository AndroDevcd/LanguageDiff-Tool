## Android Strings Resource Language Diff tool
In the event any additional languages are intended to be supported by your app.  
You can use this strings-diff tool to ensure all translations from english to the language in question are accounted for.

To use this tool simply first copy and paste the `language-diff-tool.jar` tool into the root directory of the current project you are attempting to apply language changes.

This tool offers the following features:

* Search for any string resources in english not supported in an alternte language and provide a xml document containing the missing strings found.
* Process a CSV file witch in the following formats:
  * A simple 2 column file containing english to alternate language translations
  * An advanced 3 column file containing english text, the alternate language translation, and the shared string res id
* Export a csv of all string in application for both english anf the alternate language    

### Searching for missing translations
WHen using this tool to search for missing translations within your app simply open a terminal window inside the current project directory and type the following:

```bash  
C:\Users\...\android-project> java -jar language-diff-tool.jar -les  
```  

The -l option is where you specify the language to diff against.  
Once the program executes a `missing-es-translations.xml` file will be generated listing any strings in english that need to be translated to the respective language.

### Passing a CSV file
This will allow you to automate updating your project with any translations that may need to be updated or added within your resource file. 
To pass a csv file to the tool simply type the following in the terminal within your project:

```bash 
C:\Users\...\android-project> java -jar language-diff-tool.jar -les -csv path/to/file.csv
```

It's important to note that the csv file path is relative to the `source` directory provided to the tool which can be set with the `-s` command which is also used to find the string files within your project. So be sure to include the csv file within the project directory you are using this tool in.

Once started the csv file will be scanned for any matching english strings with the values provided. 
After the tool has run a diff report will be provided which generates a `skipped-english-translations.xml` file for any skipped translations 
that could not be found as well as a `skipped-alt-lang-translations` for any translations that could not be found in the alt res file.
Please note that the tool is not case sensative when processing a simple 2 column csv file and allows for minor grammatical errors when searching for matching english strings.


##### Expected Simple CSV format
To correctly process the csv file it must be in the following format:

| English     | Spanish |
| ----------- | ----------- |
| Hello       | Hola        |
| Goodbye!    | Adios!      |
| System Error| Error del sistema |

The csv must consist of only 2 columns with `english` on the left and `spanish/german/etc.` on the other column.
This is great for quick imports of translations when you only need to update a few translations in the alternate language.
However, this only updates strings in the alternate resource file, to update both english and spanish resources refer to the second csv format below.

##### Expected Advanced CSV format
To correctly process the csv file it must be in the following format:

| English     |   Spanish   |    ResId     |
| ----------- | ----------- | ------------ |
| Hello       | Hola        | greeting_msg |
| Goodbye!    | Adios!      | exit_title   |
| System Error| Error del sistema | error_result_title |

The csv must consist of only 3 columns with `english`, `alternate-language`, & `string-res-id`. 
Once the tool has finished processing, a diff report will be provided listing any skipped translations during processing.
This format can be generated manually or automatically via csv export as described below. This format is great if 
you want to update both english strings & the alternate language at the same time when importing the translations.

##### Exporting A CSV File
To export all the strings within your app you can use the following command `-exportcsv`.
This will generate a csv file containing 3 columns `english`, `alternate-resource`, and `string-res-id` which can be updated
and used by this tool to modify the same strings with the res-id's found in the file.

Below is an example of using the export command:
```bash
C:\Users\...\android-project> java -jar language-diff-tool.jar -les -exportcsv path/to/file.csv
```

Once the command has run a file named `string-translations.csv` will be generated.

#### Ignoring strings
In the event if you do not want a string to be considered by this tool to be "not translated" and included in the missing translations file.  
You can add the `translatable="false"` flag to the string resource to have it not considered by the tool.
