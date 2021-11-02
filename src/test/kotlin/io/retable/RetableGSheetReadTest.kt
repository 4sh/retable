package io.retable

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

class RetableGSheetReadTest {

    @Test
    fun `should read gSheet with empty cells`() {
        val retable = Retable.gsheet(
                object : RetableColumns() {
                    val firstName = string("First name")
                    val lastName = string("Last Name")
                    val age = int("Age")
                    val date = localDate("Date", format = "d/M/yyyy")
                    val numberAsString = string("NumberAsString", index = 11)
                },
                options = GSheetReadOptions(
                        spreadsheetId = EMPTY_CELLS
                )).read(ByteArrayInputStream(byteArrayOf()))

        val records = retable.records.toList()
        expectThat(records).hasSize(3)
        expectThat(records[1][retable.columns.firstName]).isEqualTo("Alfred")
        expectThat(records[1][retable.columns.age]).isEqualTo(12)
        expectThat(records[1][retable.columns.numberAsString]).isEqualTo("1234567890")

        expectThat(records[2][retable.columns.lastName]).isEqualTo("Hugo")

        records.forEach { println(it) }
    }

    @Test
    fun `should read gSheet with defined cols`() {
        val retable = Retable.gsheet(
                object : RetableColumns() {
                    val firstName = string("First name")
                    val lastName = string("Last Name")
                    val age = int("Age")
                    val date = localDate("Date", format = "M/d/yyyy")
                    val numberAsString = string("NumberAsString", index = 11)
                },
                options = GSheetReadOptions(
                        spreadsheetId = MANY_DATA
                )).read(ByteArrayInputStream(byteArrayOf()))

        val records = retable.records.toList()
        expectThat(records).hasSize(3)
        expectThat(records[1][retable.columns.firstName]).isEqualTo("Alfred")
        expectThat(records[1][retable.columns.date]).isEqualTo(LocalDate.parse("2006-11-06"))
        expectThat(records[1][retable.columns.age]).isEqualTo(12)
        expectThat(records[1][retable.columns.numberAsString]).isEqualTo("1234567890")
    }

    @Test
    fun `should read gSheet on selected sheet by index`() {
        val retable = Retable.gsheet(options = GSheetReadOptions(
                sheetIndex = 2,
                spreadsheetId = WORKSHEET
        )).read(ByteArrayInputStream(byteArrayOf()))

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
    fun `should read gSheet on selected sheet by name`() {
        val retable = Retable.gsheet(
                options = GSheetReadOptions(
                        sheetName = "Last",
                        spreadsheetId = WORKSHEET))
                .read(ByteArrayInputStream(byteArrayOf()))

        val records = retable.records.toList()
        expectThat(records)
                .hasSize(2)
                .contains(
                        RetableRecord(retable.columns,1, 2,
                                listOf("Alfred", "Dalton"))
                )
    }


    @Test
    fun `should write GSheet`() {
        ByteArrayOutputStream()
        Retable(
                RetableColumns.ofNames(listOf("first_name", "last_name", "age"))
        )
                .data(
                        listOf(
                                listOf("John",  "Doe", 23),
                                listOf("Jenny", "Boe", 25)
                        )
                )
                .write(Retable.gsheet(GSheetReadOptions(
                        spreadsheetId = "1BPABkryT9bPCiAewxxUD-iKjG3J3Ztje2g0xjht_r04",
                        sheetName = "toto"
                )) to ByteArrayOutputStream())

    }

    companion object {
        const val WORKSHEET = "1aF4y8ywI99H08IHXHHYt-hg4FX984KoNhGWvn7id02o"
        const val EMPTY_CELLS = "12Td_oKZIJz-BQLRfQz8stpGQ5mAf-HWEVMnFpX-RNoQ"
        const val MANY_DATA = "1w1oMMSlHtay6F3wzSHTh3SPlxnA4ekhmmpy-y4aYm94"
    }
}
