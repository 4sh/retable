package io.retable

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MixQuoteCharBugTest {

    @Test
    fun `should write and read with specific escape char`() {
        val options = CSVReadOptions(
            charset = Charsets.UTF_8,
            delimiter = ',',
            quote = '"',
            escape = '\\',
            firstRecordAsHeader = true
        )
        val byteOutputStream = ByteArrayOutputStream()
        val record = listOf("\\\"réf externe", "plain")

        Retable(RetableColumns.ofNames(listOf("col1", "col2")))
            .data(listOf(record))
            .write(Retable.csv(options) to byteOutputStream)

        val written = ByteArrayInputStream(byteOutputStream.toByteArray()).reader().readText()

        Retable.csv(options).read(ByteArrayInputStream(written.toByteArray())).records.forEach {
            expectThat(it.rawData).isEqualTo(record)
        }
    }
}
