package io.retable

import io.retable.validation.ValidationSeverity
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.*
import java.io.ByteArrayInputStream

class RetableValidationTest {

    @Test
    fun `should validate header`() {
        val csv = """
            first,second
            Joe,Dalton
        """.trimIndent()

        val columns = RetableColumns.ofNames(listOf("FIRST", "SECOND"))

        val retable = Retable.csv(columns = columns)
                .read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expect(retable.violations.hasHeaderErrors()).isTrue()
        expect(retable.violations.header).hasSize(2)
        expect(retable.violations.header[0].severity).isEqualTo(ValidationSeverity.ERROR)
        expect(retable.violations.header[0].message()).isEqualTo("column [1] header \"first\" should be equals \"FIRST\"")

        expect(retable.violations.header[1].severity).isEqualTo(ValidationSeverity.ERROR)
        expect(retable.violations.header[1].message()).isEqualTo("column [2] header \"second\" should be equals \"SECOND\"")

        // the columns shouldn't be changed by what has been actually found
        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        // records should have parsed
        expect(retable.records).containsExactly(
                RetableRecord(retable.columns,1, 2, listOf("Joe", "Dalton"))
        )
    }

    @Test
    fun `should validate header ignore case`() {
        val csv = """
            first,second
            Joe,Dalton
        """.trimIndent()

        val columns = RetableColumns.ofNames(listOf("FIRST", "SECOND"), HeaderConstraints.eqIgnoreCase)

        val retable = Retable.csv(columns = columns)
                .read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expect(retable.violations.hasHeaderErrors()).isFalse()
        expect(retable.violations.header).hasSize(2)
        expect(retable.violations.header[0].severity).isEqualTo(ValidationSeverity.OK)
        expect(retable.violations.header[0].message()).isEqualTo("column [1] header \"first\" is equalsIgnoreCase \"FIRST\"")
    }

    // helper extensions
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}