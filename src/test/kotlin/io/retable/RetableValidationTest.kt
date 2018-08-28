package io.retable

import io.valkee.ValidationSeverity
import io.valkee.Validations.Numbers.inRange
import io.valkee.Validations.Strings.length
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
        expect(retable.violations.header[0].message()).isEqualTo("column [1] header \"first\" should be equal to \"FIRST\"")

        expect(retable.violations.header[1].severity).isEqualTo(ValidationSeverity.ERROR)
        expect(retable.violations.header[1].message()).isEqualTo("column [2] header \"second\" should be equal to \"SECOND\"")

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
        expect(retable.violations.header[0].message()).isEqualTo(
                "column [1] header \"first\" is equal ignoring case to \"FIRST\"")
    }

    @Test
    fun `should validate data`() {
        val csv = """
            first,second,age
            Joe,Dalton,TWELV
            V,Hugo,124
            Xavier,Hanin,41
        """.trimIndent()

        val columns = object : RetableColumns() {
            val first = string("first", constraint = length(inRange(2..20)))
            val second = string("second")
            val age = int("age", constraint = inRange(0..120))
        }

        val retable = Retable.csv(columns = columns)
                .read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        // no violations collected before we read the file
        expect(retable.violations.records.isEmpty())

        // should be able to filter valid records
        val validRecords = retable.records.filter { it.isValid() }.toList()

        expect(validRecords).hasSize(1)
        expect(validRecords[0][columns.first]).isEqualTo("Xavier")

        expect(retable.violations.records).hasSize(2)
        expect(retable.violations.records[0].violations).hasSize(1)
        expect(retable.violations.records[0].violations[0].message()).isEqualTo("age \"TWELV\" should be an integer")

        expect(retable.violations.records[1].violations).hasSize(2)
        expect(retable.violations.records[1].violations[0].message()).isEqualTo("first \"V\" length 1 should be between 2 and 20")
        expect(retable.violations.records[1].violations[1].message()).isEqualTo("age 124 should be between 0 and 120")
    }


    // helper extensions
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}