package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.io.StringReader

class RetableTest {

    @Test
    internal fun `should access data by col name in a row`() {
        val columns = listOf(
                RetableColumn("first_name"),
                RetableColumn("last_name"))

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

        val retable = Retable.csv().read(StringReader(csv))
        val columns = listOf(
                RetableColumn("first_name"),
                RetableColumn("last_name"))

        expect(retable) {
            map(Retable::columns).containsExactly(*columns.toTypedArray())

            map(Retable::records).containsExactly(
                    RetableRecord(columns,1, 2, listOf("Xavier", "Hanin")),
                    RetableRecord(columns,2, 3, listOf("Alfred", "Dalton")),
                    RetableRecord(columns,3, 4, listOf("Victor", "Hugo"))
            )
        }
    }

    @Test
    fun `should read simple xlsx with default settings`() {
        val retable = Retable.excel().read(
                "/simple_data.xlsx".resourceStream())

        val columns = listOf(
                RetableColumn("first_name"),
                RetableColumn("last_name"))

        expect(retable) {
            map(Retable::columns).containsExactly(*columns.toTypedArray())

            map(Retable::records).containsExactly(
                    RetableRecord(columns,1, 2, listOf("Xavier", "Hanin")),
                    RetableRecord(columns,2, 3, listOf("Alfred", "Dalton")),
                    RetableRecord(columns,3, 4, listOf("Victor", "Hugo"))
            )
        }
    }


    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}