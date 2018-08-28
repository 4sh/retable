package io.retable

import io.valkee.Validations
import io.valkee.Validations.Numbers.inRange
import io.valkee.Validations.Strings.length
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isEqualTo
import java.io.File

class RetableExamplesTest {
    val out = StringBuilder()
    fun println(a:Any?) {
        out.append(a?.toString()?:"null").append("\n")
    }
    private fun expectOut(s: String) {
        expect(out.toString()).isEqualTo(s.trimIndent() + "\n")
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

        expectOut("Hello Xavier Hanin, Hello Victor Hugo")
    }

    @Test
    fun `should validation example work`() {
        File(pathTo("invalid_data.csv")).inputStream().use {
            val retable = Retable
                    .csv(
                            // we can set constraints on the columns
                            object:RetableColumns() {
                                val FIRST_NAME = string("first_name",
                                                    constraint = length(inRange(3..20)))
                                val LAST_NAME  = string("last_name")
                                // an int column will automatically check the value is an int
                                val AGE        = int("age",
                                        constraint = inRange(0..120))
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
}