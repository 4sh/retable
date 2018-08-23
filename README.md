# retable

retable is a Kotlin library to work with tabular data files (csv, excel, open document)

Under the hood it uses Apache commons csv for CSV and Apache POI for Excel / OpenDocument, and expose them with a common and simple API.

retable is still very young, and currently has a very limited set of feature, but more are coming.

## Usage (Read)

_Hint: see `src/test/resources/examples` for data files_


### Basic Usage (CSV)

Reading a CSV file is very straightforward
```kotlin
// opens and use an input stream with std kotlin
File(pathTo("simple_data.csv")).inputStream().use {
    val hello =
        Retable.csv().read(it) // read as csv with default settings
            .records       // access the records sequence
            .map { it["first_name"] + " " + it["last_name"] } // access data by column name (headers in file)
            .first()       // sequence is consumed only on call, so getting first record in a large file is fast
    println(hello)  // prints `Xavier Hanin`
}
```

### Basic Usage (Excel)

Reading an Excel file is equally simple, and except some configuration options (see later) the API is exactly the same as with CSV!
```kotlin
File(pathTo("simple_data.xlsx")).inputStream().use {
    val hello =
            Retable.excel().read(it) // read as excel with default settings
                    .records       // access the records sequence
                    .map { it["first_name"] + " " + it["last_name"] } // access data by column name (headers in file)
                    .first()       // sequence is consumed only on call, so getting first record in a large file is fast
    println(hello)  // prints `Xavier Hanin`
}
```

### Accessing column names

By default retable reads column information from file header, and you can access to these columns
```kotlin
File(pathTo("simple_data.csv")).inputStream().use {
    val retable = Retable.csv().read(it)

    // you can access to the columns
    val colNames = retable.columns.list().map { it.name }.joinToString()

    println(colNames)      // prints `first_name, last_name, age`
}
```

### Accessing information about the record

On each record you have access to some meta information and to the raw data (list of string) of the record
```kotlin
File(pathTo("simple_data.csv")).inputStream().use {
    val record = Retable.csv().read(it)
            .records.first() // get first record

    record.apply {
        // on the record you have access to:
        // - the line number, i.e. the line (in the file) on which the record was found (1 based)
        // - the record number, i.e. the index of this record among all records (1 based)
        // - rawData, the list (as string) of each "cell"
        println("$recordNumber $lineNumber $rawData") // prints `1 2 [Xavier, Hanin, 41]`
    }
}
```

### Defining columns

Much more powerful usage can be obtained by defining the expected columns. Providing their index (1 based), their names, and their type, you can then access to the data of each column on each record in a type safe way.
```kotlin
File(pathTo("simple_data.csv")).inputStream().use {
    val retable = Retable
            .csv(
            // we can also define the expected columns
            object:RetableColumns() {
                // each column is defined as a property on an object
                val FIRST_NAME = string("first_name")
                val LAST_NAME  = string("last_name")
                // note that the column is typed - here the age is expected to be an Int
                val AGE        = int("age")
            })
            .read(it)

    // we will now be able to access data using the predefined columns, we `apply` them for ease of use
    retable.columns.apply {
        val hello = retable.records
                // now we can access the column value on a record by its column
                // note that both the column (AGE) and its type (Int) are known by the compiler
                // the direct access to the column with `AGE` is made possible thanks to the `apply`
                .filter { it[AGE]?:0 > 18 }
                // see how easy it is to access the fields
                .map { "Hello ${it[FIRST_NAME]} ${it[LAST_NAME]}" }
                .joinToString()
        println(hello)          // prints `Hello Xavier Hanin, Hello Victor Hugo`
    }
}
```

### CSV Options

#### Set charset

```kotlin
val retable = Retable.csv(options = CSVReadOptions(charset = Charsets.ISO_8859_1)).read(it)
```

#### Other options

```kotlin
val retable = Retable.csv(
                columns = RetableColumns.ofNames(listOf("Prénom", "Nom", "Oeuvre")),
                options = CSVReadOptions(
                            delimiter = ';',
                            quote = '`',
                            trimValues = true,
                            firstRecordAsHeader = false
        )).read(it)
```

### Excel Options

TO BE IMPLEMENTED


## Installation

retable has not been released yet

