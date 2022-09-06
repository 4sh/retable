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

### Validation

By default retable validates the format for typed columns (such as int columns). You can also set up additional constraints that are checked by retable while reading the records. 

```kotlin
File(pathTo("invalid_data.csv")).inputStream().use {
    val retable = Retable
            .csv(
                    // we can set constraints on the columns
                    object:RetableColumns() {
                        // here we set a constraint on the length is in a given range
                        // the constraint is defined in a block
                        val FIRST_NAME = string("first_name") { length { inRange(3..20) } }
                        // different code style, we set the constraint with a named parameter
                        val LAST_NAME  = string("last_name",
                                constraint = { matches(Regex("[A-Za-z ]+"),
                                                "should only contain alpha and spaces")})
                        // an int column will automatically check the value is an int
                        val AGE        = int("age",
                                // and we can add other constraint too
                                constraint =  { inRange(0..120) })
                    })
            .read(it)

    retable.columns.apply {
        val hello = retable.records
                .filter { it.isValid() } // we can check if a record is valid, and filter it out if we want
                .map { "Hello ${it[FIRST_NAME]} ${it[LAST_NAME]}" }
                .joinToString()
        println(hello)          // prints `Hello Victor Hugo`

        // once the sequence of records has been consumed, the invalid records
        // are available in a list
        val invalid = retable.violations.records
                .map {
                    "line ${it.lineNumber} - ${it[FIRST_NAME]} ${it[LAST_NAME]}\n" +
                    "violations:\n" +
                    it.violations
                             .map { "${it.severity.name} - ${it.message()}" }
                             .joinToString("\n")
                }
                .joinToString("\n")
        println(invalid)    // prints:
                            // ---
                            // line 2 - Xavier Hanin
                            // violations:
                            // ERROR - age 241 should be between 0 and 120
                            // line 3 - A Dalton
                            // violations:
                            // ERROR - first_name "A" length 1 should be between 3 and 20
                            // ERROR - age "TWELVE" should be an integer
                            // ---
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
                            ignoreEmptyLines = true,
                            trimValues = true,
                            firstRecordAsHeader = false
        )).read(it)
```

### Excel Options

#### Select Sheet by name

```kotlin
val retable = Retable.excel(options = ExcelReadOptions(sheetName = "my sheet")).read(it)
```

#### Select Sheet by index

```kotlin
val retable = Retable.excel(options = ExcelReadOptions(sheetIndex = 2)).read(it)
```

#### Common options

```kotlin
val retable = Retable.excel(options = ExcelReadOptions(
                ignoreEmptyLines = true,
                trimValues = true,
                firstRecordAsHeader = false
         )).read(it)
```

## Usage (Write)

Note: Only basic Excel write is supported yet

### Basic

```kotlin
        Retable(
                // we define the column names
                RetableColumns.ofNames(listOf("first_name", "last_name", "age"))
            )
            .data(
                // we provide the data to write as a list
                // of List<Any> (the list of values of each row)
                listOf(
                        listOf("John",  "Doe", 23),
                        listOf("Jenny", "Boe", 25)
                )
            )
            // then we can just ask to write data to outputstream
            // in the format we want
            .write(Retable.excel() to File(pathTo("export_data.xlsx")).outputStream())

            /* produces an excel file like this:
                +------------+-----------+-----+
                | first_name | last_name | age |
                +------------+-----------+-----+
                | John       | Doe       |  23 |
                | Jenny      | Boe       |  25 |
                +------------+-----------+-----+
             */

```

### Typed Columns

```kotlin
// we can also define typed columns with arbitrary indexes (or not)
        val columns = object:RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME  = string("last_name", index = 1)
            val AGE        = int("age", index = 3)
        }
        Retable(columns)
            .data(
                    // we provide the data to write as a list
                    // of any kind
                    listOf(
                            Person("John", "Doe", 23),
                            Person("Jenny", "Boe", 25)
                    )
            ) {     // with the mapper function to transform them to map <column -> value>
                    mapOf(
                            // columns are easily accessible in this context
                            // (the receiver is the RetableColumns object defined above)
                            FIRST_NAME to it.firstName,
                            LAST_NAME to it.lastName,
                            AGE to it.age
                    )
            }
            // then we can just ask to write data
            .write(Retable.excel(columns) to File(pathTo("export_data_cols.xlsx")).outputStream())

            /* produces an excel file like this:
                +-----------+------------+-----+
                | last_name | first_name | age |
                +-----------+------------+-----+
                | Doe       | John       |  23 |
                | Boe       | Jenny      |  25 |
                +-----------+------------+-----+
             */
```

## G-Sheet support
### Configuration
To be allowed to access to your Google Drive and read/write on Google spreadsheet, you must generate and OAuth 2.0 
Client Ids from the Google cloud console in section [APIs & services > Credentials](https://console.cloud.google.com/apis/credentials)

Then, download your credential as json file and give it to your `GSheetReadOptions.credentialFilePath` (by default 
Retable take the file `credentials.json` in the `resources` directory)

## Installation

retable has not been released yet, but you can use jitpack to obtain it:
https://jitpack.io/#4sh/retable/-SNAPSHOT


