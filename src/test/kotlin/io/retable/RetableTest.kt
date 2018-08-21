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
            val test1 = StringRetableColumn(c++, "test1")
            val test2 = IntRetableColumn(c++, "test2")
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
            first_name,last_name,age
            Xavier,Hanin,41
            Alfred,Dalton,12
            Victor,Hugo,88
        """.trimIndent()

        val columns = object:RetableColumns() {
            val firstName = StringRetableColumn(c++, "first_name")
            val lastName = StringRetableColumn(c++, "last_name")
            val age = IntRetableColumn(c++, "age")
        }

        val retable = Retable.csv(columns = columns).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        val records = retable.records.toList()

        expect(records).containsExactly(
                RetableRecord(columns,1, 2, listOf("Xavier", "Hanin", "41")),
                RetableRecord(columns,2, 3, listOf("Alfred", "Dalton", "12")),
                RetableRecord(columns,3, 4, listOf("Victor", "Hugo", "88"))
        )

        val firstRecord = records[0]

        expect(firstRecord[retable.columns.firstName]).isEqualTo("Xavier")
        expect(firstRecord[columns.lastName]).isEqualTo("Hanin")
        expect(firstRecord[columns.age]).isEqualTo(41)
    }

    @Test
    fun `should apply columns`() {
        val csv = """
            first_name,last_name,age
            Xavier,Hanin,41
            Alfred,Dalton,12
        """.trimIndent()

        val retable = Retable.csv(columns = object:RetableColumns() {
            val firstName = StringRetableColumn(c++, "first_name")
            val lastName = StringRetableColumn(c++, "last_name")
            val age = IntRetableColumn(c++, "age")
        }).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        retable.columns.apply {
            expect(
                    retable.records
                    .filter { it[age]?:0 > 18 }
                    .map { "Hello ${it[firstName]} ${it[lastName]}" }
                    .joinToString()
            ).isEqualTo("Hello Xavier Hanin")
        }
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

    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}