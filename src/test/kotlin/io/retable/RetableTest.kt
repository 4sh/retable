package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.File

class RetableTest {

    @Test
    fun `should list columns`() {
        val cols = object:RetableColumns() {
            val test1 = StringRetableColumn(0, "test1")
            val test2 = RetableColumn<Int>(1, "test2")
            val somethingElse = "test3"
        }

        expect(cols.list()).containsExactly(cols.test1, cols.test2)
    }


    @Test
    fun `should access data by col name in a row`() {
        val columns = RetableColumns.ofNames(listOf("first_name", "last_name"))

        expect(RetableRecord(columns,1, 2, listOf("Xavier", "Hanin"))) {
            map {it["first_name"] }.isEqualTo("Xavier")
            map {it["last_name"] }.isEqualTo("Hanin")
        }
    }

    @Test
    fun `should parse simple csv with default settings from string`() {
        val csv = """
            first_name,last_name
            Xavier,Hanin
            Alfred,Dalton
            Victor,Hugo
        """.trimIndent()

        val retable = Retable.csv().read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        val columns = RetableColumns.ofNames(listOf("first_name", "last_name"))
        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expect(retable.records).containsExactly(
                RetableRecord(columns,1, 2, listOf("Xavier", "Hanin")),
                RetableRecord(columns,2, 3, listOf("Alfred", "Dalton")),
                RetableRecord(columns,3, 4, listOf("Victor", "Hugo"))
        )
    }

    @Test
    fun `should parse simple csv with predefined columns`() {
        val csv = """
            first_name,last_name
            Xavier,Hanin
            Alfred,Dalton
            Victor,Hugo
        """.trimIndent()

        val columns = object:RetableColumns() {
            val firstName = StringRetableColumn(0, "first_name")
            val lastName = StringRetableColumn(1, "last_name")
        }

        val retable = Retable.csv(columns = columns).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        val records = retable.records.toList()

        expect(records).containsExactly(
                RetableRecord(columns,1, 2, listOf("Xavier", "Hanin")),
                RetableRecord(columns,2, 3, listOf("Alfred", "Dalton")),
                RetableRecord(columns,3, 4, listOf("Victor", "Hugo"))
        )

        val firstRecord = records[0]

        expect(firstRecord[retable.columns.firstName]).isEqualTo("Xavier")
        expect(firstRecord[columns.lastName]).isEqualTo("Hanin")
    }

    @Test
    fun `should read simple xlsx with default settings`() {
        val retable = Retable.excel().read(
                "/simple_data.xlsx".resourceStream())

        val columns = RetableColumns.ofNames(listOf("first_name", "last_name"))
        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expect(retable.records).containsExactly(
                RetableRecord(columns,1, 2, listOf("Xavier", "Hanin")),
                RetableRecord(columns,2, 3, listOf("Alfred", "Dalton")),
                RetableRecord(columns,3, 4, listOf("Victor", "Hugo"))
        )
    }

    @Test
    fun `should basic example work`() {
        // for test only
        val pathToExcelFile = "src/test/resources/simple_data.xlsx"
        val pathToCsvFile = "src/test/resources/simple_data.csv"
        val out = StringBuilder()
        fun println(a:Any?) {
            out.append(a?.toString()?:"null").append("\n")
        }

        // example
        File(pathToExcelFile).inputStream().use {
            // reads data from Excel file
            val retable = Retable.excel().read(it)

            // table data columns are populated from header in file
            println(retable.columns[0]?.name)      // prints `first_name`
            println(retable.columns[1]?.name)      // prints `last_name`

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

        File(pathToCsvFile).inputStream().use {
            // reads data from CSV file
            val retable = Retable.csv().read(it)

            // exact same api than for excel files can be used for CSV
            println(retable.columns[0]?.name)      // prints `first_name`
        }

        File(pathToExcelFile).inputStream().use {
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


        // check example
        expect(out.toString()).isEqualTo("""
            first_name
            last_name
            3
            [Xavier, Hanin]
            Xavier
            1
            2
            first_name
            Xavier
            Hanin
            """.trimIndent() + "\n")
    }


    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}