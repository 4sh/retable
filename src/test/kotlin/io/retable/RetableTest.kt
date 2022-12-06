package io.retable

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.io.ByteArrayInputStream

class RetableTest {

    @Test
    fun `should list columns`() {
        val cols = object : RetableColumns() {
            val test1 = string("test1")
            val test2 = int("test2")
            val notAColumn = "test3"
        }

        expectThat(cols.list()).containsExactly(cols.test1, cols.test2)
    }

    @Test
    fun `should access data by col name in a row`() {
        val columns = RetableColumns.ofNames(listOf("first_name", "last_name"))

        expectThat(RetableRecord(columns, 1, 2, listOf("Xavier", "Hanin"))) {
            get { this["first_name"] }.isEqualTo("Xavier")
            get { this["last_name"] }.isEqualTo("Hanin")
        }
    }

    @Test
    fun `should allow missing values in a row`() {
        val columns = RetableColumns.ofNames(listOf("first_name", "last_name"))

        expectThat(RetableRecord(columns, 1, 2, listOf("Xavier"))) {
            get { this["last_name"] }.isNull()
            get { this.rawGet(columns[2]) }.isNull()
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
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expectThat(retable.records).containsExactly(
            RetableRecord(columns, 1, 2, listOf("Xavier", "Hanin")),
            RetableRecord(columns, 2, 3, listOf("Alfred", "Dalton")),
            RetableRecord(columns, 3, 4, listOf("Victor", "Hugo"))
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

        val columns = object : RetableColumns() {
            val firstName = string("first_name")
            val lastName = string("last_name")
            val age = int("age")
        }

        val retable = Retable.csv(columns = columns).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        val records = retable.records.toList()

        expectThat(records).containsExactly(
            RetableRecord(columns, 1, 2, listOf("Xavier", "Hanin", "41")),
            RetableRecord(columns, 2, 3, listOf("Alfred", "Dalton", "12")),
            RetableRecord(columns, 3, 4, listOf("Victor", "Hugo", "88"))
        )

        val firstRecord = records[0]

        expectThat(firstRecord[retable.columns.firstName]).isEqualTo("Xavier")
        expectThat(firstRecord[columns.lastName]).isEqualTo("Hanin")
        expectThat(firstRecord[columns.age]).isEqualTo(41)
    }

    @Test
    fun `should apply columns`() {
        val csv = """
            first_name,last_name,age
            Xavier,Hanin,41
            Alfred,Dalton,12
        """.trimIndent()

        val retable = Retable.csv(
            columns = object : RetableColumns() {
                val firstName = string("first_name")
                val lastName = string("last_name")
                val age = int("age")
            }
        ).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        retable.columns.apply {
            expectThat(
                retable.records
                    .filter { it[age] ?: 0 > 18 }
                    .map { "Hello ${it[firstName]} ${it[lastName]}" }
                    .joinToString()
            ).isEqualTo("Hello Xavier Hanin")
        }
    }

    @Test
    fun `should read simple xlsx with default settings`() {
        val retable = Retable.excel().read(
            "/simple_data.xlsx".resourceStream()
        )

        val columns = RetableColumns.ofNames(listOf("first_name", "last_name"))
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expectThat(retable.records).containsExactly(
            RetableRecord(columns, 1, 2, listOf("Xavier", "Hanin")),
            RetableRecord(columns, 2, 3, listOf("Alfred", "Dalton")),
            RetableRecord(columns, 3, 4, listOf("Victor", "Hugo"))
        )
    }

    @ParameterizedTest(name = "\"{0}\" should be {1}")
    @CsvSource(
        "A,   1",
        "B,   2 ",
        "j,   10",
        "Z,   26",
        "AA,  27",
        "AZ,  52",
        "BA,  53"
    )
    fun `should convert column indexes`(c: String, index: Int) {
        expectThat(col(c)).isEqualTo(index)
    }

    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>> =
        this.get { toList() }.containsExactly(*elements)
}
