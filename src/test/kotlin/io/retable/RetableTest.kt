package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.containsExactly
import java.io.StringReader

class RetableTest {
    @Test
    fun `should parse simple csv with default settings from string`() {
        val csv = """
            first_name,last_name
            Xavier,Hanin
            Alfred,Dalton
            Victor,Hugo
        """.trimIndent()

        val retable = Retable.csv().parse(StringReader(csv))

        expect(retable).map(Retable::records).containsExactly(
                RetableRecord(1, 2, listOf("Xavier", "Hanin")),
                RetableRecord(2, 3, listOf("Alfred", "Dalton")),
                RetableRecord(3, 4, listOf("Victor", "Hugo"))
        )
    }

    @Test
    fun `should read simple xlsx with default settings`() {
        val retable = Retable.excel().read(
                "/simple_data.xlsx".resourceStream())
        expect(retable).map(Retable::records).containsExactly(
                RetableRecord(1, 2, listOf("Xavier", "Hanin")),
                RetableRecord(2, 3, listOf("Alfred", "Dalton")),
                RetableRecord(3, 4, listOf("Victor", "Hugo"))
        )
    }


    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}