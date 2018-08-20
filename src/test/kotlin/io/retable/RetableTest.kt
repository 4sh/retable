package io.retable

import org.junit.jupiter.api.Test
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

        val list = Retable.csv().parse(StringReader(csv)).toList()

        println(list)

        expect(list).containsExactly(
                RetableRecord(1, 2, listOf("Xavier", "Hanin")),
                RetableRecord(2, 3, listOf("Alfred", "Dalton")),
                RetableRecord(3, 4, listOf("Victor", "Hugo"))
        )
    }
}