package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.File

class RetableCSVTest {

    @Test
    fun `should parse csv with custom charset from string`() {
        val csv = """
            Prénom,Nom
            Ñino,Dalton
        """.trimIndent()

        val retable = Retable.csv(options = CSVReadOptions(
                charset = Charsets.ISO_8859_1
        )).read(ByteArrayInputStream(csv.toByteArray(Charsets.ISO_8859_1)))

        val columns = RetableColumns.ofNames(listOf("Prénom", "Nom"))
        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expect(retable.records).containsExactly(
                RetableRecord(columns,1, 2, listOf("Ñino", "Dalton"))
        )
    }

    @Test
    fun `should parse csv with custom settings`() {
        val csv = """
            `Arthur `;` Rimbaud `;`Le dormeur du val ; 1870`
            `Victor `;` Hugo `;`Demain, dès l'aube… ; 1856`
            """.trimIndent()

        val retable = Retable.csv(
                columns = RetableColumns.ofNames(listOf("Prénom", "Nom", "Oeuvre")),
                options = CSVReadOptions(
                            delimiter = ';',
                            quote = '`',
                            trimValues = true,
                            firstRecordAsHeader = false
        )).read(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))

        expect(retable.records).containsExactly(
                RetableRecord(retable.columns,1, 1,
                        listOf("Arthur", "Rimbaud", "Le dormeur du val ; 1870")),
                RetableRecord(retable.columns,2, 2,
                        listOf("Victor", "Hugo", "Demain, dès l'aube… ; 1856"))
        )
    }

    // helper extensions
    fun <T : Sequence<E>, E> Assertion.Builder<T>.containsExactly(vararg elements: E): Assertion.Builder<List<E>>
     = this.map { it.toList() }.containsExactly(*elements)
}