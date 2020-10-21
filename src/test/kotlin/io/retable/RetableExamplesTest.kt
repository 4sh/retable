package io.retable

import io.valkee.rules.*
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File
import java.time.LocalDate

class RetableExamplesTest {
    val out = StringBuilder()
    fun println(a:Any?) {
        out.append(a?.toString()?:"null").append("\n")
    }
    private fun expectOut(s: String) {
        expectThat(out.toString()).isEqualTo(s.trimIndent() + "\n")
    }

    fun pathTo(s:String) = "src/test/resources/examples/$s"

    @Test
    fun `should introduction example work`() {
        // opens and use an input stream with std kotlin
        File(pathTo("simple_data.csv")).inputStream().use {
            val hello =
                Retable.csv().read(it) // read as csv with default settings
                    .records       // access the records sequence
                    .map { it["first_name"] + " " + it["last_name"] } // access data by column name (headers in file)
                    .first()       // sequence is consumed only on call, so getting first record in a large file is fast
            println(hello)  // prints `Xavier Hanin`
        }

        expectOut("Xavier Hanin")
    }

    @Test
    fun `should introduction excel example work`() {
        // opens and use an input stream with std kotlin
        File(pathTo("simple_data.xlsx")).inputStream().use {
            val hello =
                    Retable.excel().read(it) // read as excel with default settings
                            .records       // access the records sequence
                            .map { it["first_name"] + " " + it["last_name"] } // access data by column name (headers in file)
                            .first()       // sequence is consumed only on call, so getting first record in a large file is fast
            println(hello)  // prints `Xavier Hanin`
        }

        expectOut("Xavier Hanin")
    }


    @Test
    fun `should access column names work`() {
        File(pathTo("simple_data.csv")).inputStream().use {
            val retable = Retable.csv().read(it)

            // you can access to the columns
            val colNames = retable.columns.list().map { it.name }.joinToString()

            println(colNames)      // prints `first_name, last_name, age`
        }

        expectOut("first_name, last_name, age")
    }

    @Test
    fun `should access record info work`() {
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

        expectOut("1 2 [Xavier, Hanin, 41]")
    }

    @Test
    fun `should columns example work`() {
        File(pathTo("simple_data.csv")).inputStream().use {
            val retable = Retable
                    .csv(
                    // we can also define the expected columns
                    object:RetableColumns() {
                        // each column is defined as a property on an object
                        val FIRST_NAME = string("first_name")
                        val LAST_NAME  = string("last_name")
                        // note that the column is typed - here the age is expectThated to be an Int
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

        expectOut("Hello Xavier Hanin, Hello Victor Hugo")
    }

    @Test
    fun `should validation example work`() {
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

        expectOut("""
            Hello Victor Hugo
            line 2 - Xavier Hanin
            violations:
            ERROR - age 241 should be between 0 and 120
            line 3 - A Dalton
            violations:
            ERROR - first_name "A" length 1 should be between 3 and 20
            ERROR - age "TWELVE" should be an integer
            """)
    }

    @Test
    fun `should simple export example work`() {
        Retable(
                // we define the column names
                RetableColumns.ofNames(listOf("first_name", "last_name", "age"))
            )
            .data(
                // we provide the data to write as either a list or a sequence
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

        expectThat(File(pathTo("export_data.xlsx"))) {
            get {exists()}.isTrue()
        }
    }

    @Test
    fun `should export columns example work`() {
        // we can also define typed columns with arbitrary indexes (or not)
        val columns = object:RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME  = string("last_name", index = 1)
            val AGE        = int("age", index = 3)
        }
        Retable(columns)
            .data(
                    // we provide the data to write as either a list or a sequence
                    // of any kind
                    listOf(
                            Person("John", "Doe", 23),
                            Person("Jenny", "Boe", 25)
                    )
            ) {     // with the mapper function to transform then to map <column -> value>
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
    }

    @Test
    fun `should export columns with local date example work`() {
        // we can also define typed columns with arbitrary indexes (or not)
        val columns = object:RetableColumns() {
            val NAME = string("name")
            val START_DATE   = localDate("start_date")
            val END_DATE   = localDate("end_date")
        }
        Retable(columns)
            .data(
                    // we provide the data to write as either a list or a sequence
                    // of any kind
                    listOf(
                            Event("So Good Fest",
                                    LocalDate.parse("2020-06-05"),
                                    LocalDate.parse("2020-06-06")
                            ),
                            Event("Les Fous-Cavés",
                                    LocalDate.parse("2019-07-19"),
                                    LocalDate.parse("2020-07-21")
                            )
                    )
            ) {     // with the mapper function to transform then to map <column -> value>
                    mapOf(
                            // columns are easily accessible in this context
                            // (the receiver is the RetableColumns object defined above)
                            NAME to it.name,
                            START_DATE to it.startDate,
                            END_DATE to it.endDate
                    )
            }
            // then we can just ask to write data
            .write(Retable.excel(columns) to File(pathTo("export_data_cols_date.xlsx")).outputStream())

            /* produces an excel file like this:
                +----------------+------------+------------+
                | name           | start_date | end_date   |
                +----------------+------------+------------+
                | So Good Fest   | 05/07/2020 | 06/07/2020 |
                | Les Fous-Cavés | 19/08/2019 | 21/08/2020 |
                +----------------+------------+------------+
             */
    }

//    @Test
//    fun `should export after import example work`() {
//        File(pathTo("simple_data.xlsx")).inputStream().use {
//            Retable
//                    .excel(
//                            object:RetableColumns() {
//                                val FIRST_NAME = string("first_name")
//                                val LAST_NAME  = string("last_name")
//                                val AGE        = int("age")
//                            })
//                    .read(it)
//                    .filter { it[AGE]?:0 > 18 }
//                    .update { mapOf(AGE to it[AGE] + 5) }
//                    .write()
//        }
//    }

//    @Test
//    fun `should export after import with data class example work`() {
//        File(pathTo("simple_data.xlsx")).inputStream().use {
//            Retable
//                    .excel(
//                            object:RetableColumns() {
//                                val FIRST_NAME = string("first_name")
//                                val LAST_NAME  = string("last_name")
//                                val AGE        = int("age")
//                            })
//                    .read(it)
//                    .map { Person(it[FIRST_NAME], it[LAST_NAME], it[AGE]) }
//                    .filter { it.age > 18 }
//                    .update { it.copy(age = it.age + 5) }
//                    .map { mapOf(
//                            // columns are easily accessible in this context
//                            // (the `this` is the RetableColumns object defined above)
//                            FIRST_NAME to it.firstName,
//                            LAST_NAME to it.lastName,
//                            AGE to it.age
//                        )
//                    }
//                    .write()
//        }
//    }


}

data class Person(val firstName:String, val lastName: String, val age:Int)

data class Event(val name: String, val startDate: LocalDate, val endDate: LocalDate)
