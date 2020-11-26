package io.retable

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.time.LocalDate

class RetableExcelTest {


    @Test
    fun `should read xlsx with default settings`() {
        val retable = Retable.excel().read(
                "/many_data.xlsx".resourceStream())

        val columns = RetableColumns.ofNames(
                listOf("First name", "Last Name", "Age", "Date", "Time",
                        "Link", "Math", "Sum", "Money", "Percent", "NumberAsString"))
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expectThat(retable.records.toList()).contains(
                RetableRecord(columns,2, 4,
                        listOf("Alfred", "Dalton", "12", "2006-11-06", "12:34:00",
                                "http://example.com/dalton", "2.5", "14.5", "24", "0.05", "1234567890"))
        )
    }

    @Test
    fun `should read xlsx with defined cols`() {
        val retable = Retable.excel(
                object : RetableColumns() {
                    val firstName = string("First name")
                    val lastName = string("Last Name")
                    val age = int("Age")
                    val date = localDate("Date")
                    val numberAsString = string("NumberAsString", index = 11)
                }
        ).read(
                "/many_data.xlsx".resourceStream())

        val records = retable.records.toList()
        expectThat(records).hasSize(3)
        expectThat(records[1][retable.columns.firstName]).isEqualTo("Alfred")
        expectThat(records[1][retable.columns.date]).isEqualTo(LocalDate.parse("2006-11-06"))
        expectThat(records[1][retable.columns.age]).isEqualTo(12)
        expectThat(records[1][retable.columns.numberAsString]).isEqualTo("1234567890")
    }

    @Test
    fun `should read xlsx with empty cells`() {
        val retable = Retable.excel(
                object : RetableColumns() {
                    val firstName = string("First name")
                    val lastName = string("Last Name")
                    val age = int("Age")
                    val date = localDate("Date")
                    val numberAsString = string("NumberAsString", index = 11)
                }
        ).read(
                "/empty_cells.xlsx".resourceStream())

        val records = retable.records.toList()
        expectThat(records).hasSize(3)
        expectThat(records[1][retable.columns.firstName]).isEqualTo("Alfred")
        expectThat(records[1][retable.columns.age]).isEqualTo(12)
        expectThat(records[1][retable.columns.numberAsString]).isEqualTo("1234567890")

        expectThat(records[2][retable.columns.lastName]).isEqualTo("Hugo")
    }

    @Test
    fun `should read xlsx on selected sheet by index`() {
        val retable = Retable.excel(options = ExcelReadOptions(sheetIndex = 2)).read(
                "/worksheets.xlsx".resourceStream())

        val records = retable.records.toList()
        println(records)
        expectThat(records)
                .hasSize(2)
                .contains(
                        RetableRecord(retable.columns,1, 2,
                                listOf("Xavier", "Hanin"))
        )
    }

    @Test
    fun `should read xlsx on selected sheet by name`() {
        val retable = Retable.excel(options = ExcelReadOptions(sheetName = "Last")).read(
                "/worksheets.xlsx".resourceStream())

        val records = retable.records.toList()
        expectThat(records)
                .hasSize(2)
                .contains(
                        RetableRecord(retable.columns,1, 2,
                                listOf("Alfred", "Dalton"))
        )
    }


    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
}
