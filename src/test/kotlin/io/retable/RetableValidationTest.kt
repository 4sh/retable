package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.*
import java.io.ByteArrayInputStream
import java.io.File

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

        expect(retable.validations.hasHeaderErrors()).isTrue()
        expect(retable.validations.header).hasSize(2)
        expect(retable.validations.header[0])
                .isEqualTo(ValidationResult(ValidationLevel.ERROR, "first",
                        StringEqualValidationRule("FIRST", ValidationLevel.ERROR),
                        "FIRST"))
        expect(retable.validations.header[1])
                .isEqualTo(ValidationResult(ValidationLevel.ERROR, "second",
                        StringEqualValidationRule("SECOND", ValidationLevel.ERROR),
                        "SECOND"))

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

        val columns = RetableColumns.ofNames(listOf("FIRST", "SECOND"), RetableValidations.header.eqIgnoreCase)

        val retable = Retable.csv(columns = columns)
                .read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expect(retable.validations.hasHeaderErrors()).isFalse()
        expect(retable.validations.header).hasSize(2)
        expect(retable.validations.header[0])
                .isEqualTo(ValidationResult(ValidationLevel.OK, "first",
                        StringEqualIgnoreCaseValidationRule("FIRST", ValidationLevel.ERROR),
                        "FIRST"))
    }

    // helper extensions
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}