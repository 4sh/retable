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

class RetableGSheetWriteTest {

    @Test
    fun `should write gSheet`() {
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
                        spreadsheetId = OUTPUT_SHEET,
                        sheetName = "toto"
                )) to ByteArrayOutputStream())

    }

    @Test
    fun `should export indexed columns`() {
        val columns = object:RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME  = string("last_name", index = 1)
            val AGE        = int("age", index = 3)
        }
        Retable(columns)
                .data(
                        listOf(
                                Person("John", "Doe", 23),
                                Person("Jenny", "Boe", 25)
                        )
                ) {
                    mapOf(
                            FIRST_NAME to it.firstName,
                            LAST_NAME to it.lastName,
                            AGE to it.age
                    )
                }
                .write(Retable.gsheet(columns, GSheetReadOptions(spreadsheetId = OUTPUT_SHEET)) to
                        ByteArrayOutputStream())

    }

    @Test
    fun `should export columns with local date`() {
        val columns = object:RetableColumns() {
            val NAME = string("name")
            val START_DATE   = localDate("start_date")
            val END_DATE   = localDate("end_date")
        }
        Retable(columns)
                .data(
                        listOf(
                                Event("So Good Fest",
                                        LocalDate.parse("2020-06-05"),
                                        LocalDate.parse("2020-06-06")
                                ),
                                Event("Les Fous-Cavés",
                                        LocalDate.parse("2019-07-19"),
                                        LocalDate.parse("2020-07-21")
                                )
                        )
                ) {
                    mapOf(
                            NAME to it.name,
                            START_DATE to it.startDate,
                            END_DATE to it.endDate
                    )
                }
                .write(Retable.gsheet(columns, GSheetReadOptions(spreadsheetId = OUTPUT_SHEET)) to
                        ByteArrayOutputStream())
    }

    companion object {
        const val OUTPUT_SHEET = "1BPABkryT9bPCiAewxxUD-iKjG3J3Ztje2g0xjht_r04"
        const val MANY_DATA = "1w1oMMSlHtay6F3wzSHTh3SPlxnA4ekhmmpy-y4aYm94"
    }
}
