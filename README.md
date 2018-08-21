# retable

retable is a Kotlin library to work with tabular data files (csv, excel, open document)

Under the hood it uses Apache commons csv for CSV and Apache POI for Excel / OpenDocument, and expose them with a common and simple API.

retable is still very young, and currently has a very limited set of feature, but more are coming.

## Example

_see `src/test/resources` for data files_

```kotlin
File("path/to/excel.xslx").inputStream().use {
    // reads data from Excel file
    val retable = Retable.excel().read(it)

    // table data columns are populated from header in file
    println(retable.columns[0].name)      // prints `first_name`
    println(retable.columns[1].name)      // prints `last_name`

    // records (rows) are available in a sequence, we convert it to a list for the example
    val records = retable.records.toList()

    println(records.size)                 // prints `3`
    println(records[0].rawData)           // prints `[Xavier, Hanin]`
    println(records[0].get("first_name")) // prints `Xavier`
    println(records[0].recordNumber)      // prints `1`
    // (record numbers are one based, they are intended for humans

    println(records[0].lineNumber)        // prints `2`
    // line numbers are one based, and count all lines in file
    // (header, empty lines, comments, ...)
}

File("path/to/data.csv").inputStream().use {
    // reads data from CSV file
    val retable = Retable.csv().read(it)

    // exact same api than for excel files can be used for CSV
    println(retable.columns[0].name)      // prints `first_name`
}

File("path/to/excel.xslx").inputStream().use {
    // access data with type safe columns
    val retable = Retable.excel(columns = object:RetableColumns(){
        val firstName = StringRetableColumn(0, "first_name")
        val lastName = StringRetableColumn(1, "last_name")
    }).read(it)

    // records (rows) are available in a sequence, we convert it to a list for the example
    val records = retable.records.toList()

    println(records[0][retable.columns.firstName]) // prints `Xavier`
    println(records[0][retable.columns.lastName]) // prints `Hanin`
}

```


## Installation

retable has not been released yet

