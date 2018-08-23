package io.retable

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.contains
import strikt.assertions.containsExactly

class RetableExcelTest {


    @Test
    fun `should read simple xlsx with default settings`() {
        val retable = Retable.excel().read(
                "/many_data.xlsx".resourceStream())

        val columns = RetableColumns.ofNames(
                listOf("First name", "Last Name", "Age", "Date", "Time", "Link", "Math", "Sum", "Money", "Percent"))
        expect(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expect(retable.records.toList()).contains(
                RetableRecord(columns,2, 4,
                        listOf("Alfred", "Dalton", "12",
                                "2006-11-06", "12:34:00", "http://example.com/dalton", "2.5", "14.5", "24", "0.05"))
        )
    }

    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
}