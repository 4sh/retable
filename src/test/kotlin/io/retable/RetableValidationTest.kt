package io.retable

import io.valkee.Validations.Numbers.inRange
import io.valkee.ValkeeSeverity
import io.valkee.rules.length
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
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

        expectThat(retable.violations.hasHeaderErrors()).isTrue()
        expectThat(retable.violations.header).hasSize(2)
        expectThat(retable.violations.header[0].severity).isEqualTo(ValkeeSeverity.ERROR)
        expectThat(retable.violations.header[0].message())
            .isEqualTo("column [1] header \"first\" should be equal to \"FIRST\"")

        expectThat(retable.violations.header[1].severity).isEqualTo(ValkeeSeverity.ERROR)
        expectThat(retable.violations.header[1].message())
            .isEqualTo("column [2] header \"second\" should be equal to \"SECOND\"")

        // the columns shouldn't be changed by what has been actually found
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        // records should have parsed
        expectThat(retable.records).containsExactly(
            RetableRecord(retable.columns, 1, 2, listOf("Joe", "Dalton"))
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

        expectThat(retable.violations.hasHeaderErrors()).isFalse()
        expectThat(retable.violations.header).hasSize(2)
        expectThat(retable.violations.header[0].severity).isEqualTo(ValkeeSeverity.OK)
        expectThat(retable.violations.header[0].message()).isEqualTo(
            "column [1] header \"first\" is equal ignoring case to \"FIRST\""
        )
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
            val first = string("first") { length { inRange(2..20) } }
            val second = string("second")
            val age = int("age", constraint = { inRange(0..120) })
        }

        val retable = Retable.csv(columns = columns)
            .read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        // no violations collected before we read the file
        expectThat(retable.violations.records.isEmpty())

        // should be able to filter valid records
        val validRecords = retable.records.filter { it.isValid() }.toList()

        expectThat(validRecords).hasSize(1)
        expectThat(validRecords[0][columns.first]).isEqualTo("Xavier")

        expectThat(retable.violations.records).hasSize(2)
        expectThat(retable.violations.records[0].violations).hasSize(1)
        expectThat(retable.violations.records[0].violations[0].message())
            .isEqualTo("age \"TWELV\" should be an integer")

        expectThat(retable.violations.records[1].violations).hasSize(2)
        expectThat(retable.violations.records[1].violations[0].message())
            .isEqualTo("first \"V\" length 1 should be between 2 and 20")
        expectThat(retable.violations.records[1].violations[1].message())
            .isEqualTo("age 124 should be between 0 and 120")
    }

    // helper extensions
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>> =
        this.get { toList() }.containsExactly(*elements)
}
