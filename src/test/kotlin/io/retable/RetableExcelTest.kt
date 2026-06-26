package io.retable

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RetableExcelTest {
    fun pathTo(s: String) = "src/test/resources/examples/$s"

    @Test
    fun `should read xlsx with default settings`() {
        val retable = Retable.excel().read(
            "/many_data.xlsx".resourceStream()
        )

        val columns = RetableColumns.ofNames(
            listOf(
                "First name", "Last Name", "Age", "Date", "Time",
                "Link", "Math", "Sum", "Money", "Percent", "NumberAsString"
            )
        )
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expectThat(retable.records.toList()).hasSize(3).and {
            get(1).and {
                get { columns }.isEqualTo(columns)
                get { recordNumber }.isEqualTo(2)
                get { lineNumber }.isEqualTo(4)
                get { rawData }.hasSize(11).and {
                    get(0).isEqualTo("Alfred")
                    get(1).isEqualTo("Dalton")
                    get(2).isEqualTo("12")
                    get(3).isEqualTo("2006-11-06")
                    get(4).isEqualTo("12:34:00")
                    get(5).isEqualTo("http://example.com/dalton")
                    get(6).isEqualTo("2.5")
                    get(7).isEqualTo("14.5")
                    get(8).isEqualTo("24")
                    get(9).isEqualTo("0.05")
                    get(10).isEqualTo("1234567890")
                }
            }
        }
    }

    @Test
    fun `should read xlsx with date format`() {
        val options = ExcelReadOptions(defaultDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val retable = Retable.excel(options).read(
            "/many_data.xlsx".resourceStream()
        )

        val columns = RetableColumns.ofNames(
            listOf(
                "First name", "Last Name", "Age", "Date", "Time",
                "Link", "Math", "Sum", "Money", "Percent", "NumberAsString"
            )
        )
        expectThat(retable.columns.list()).containsExactly(*columns.list().toTypedArray())

        expectThat(retable.records.toList()).hasSize(3).and {
            get(1).and {
                get { columns }.isEqualTo(columns)
                get { recordNumber }.isEqualTo(2)
                get { lineNumber }.isEqualTo(4)
                get { rawData }.hasSize(11).and {
                    get(0).isEqualTo("Alfred")
                    get(1).isEqualTo("Dalton")
                    get(2).isEqualTo("12")
                    get(3).isEqualTo("06/11/2006")
                    get(4).isEqualTo("12:34:00")
                    get(5).isEqualTo("http://example.com/dalton")
                    get(6).isEqualTo("2.5")
                    get(7).isEqualTo("14.5")
                    get(8).isEqualTo("24")
                    get(9).isEqualTo("0.05")
                    get(10).isEqualTo("1234567890")
                }
            }
        }
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
            "/many_data.xlsx".resourceStream()
        )

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
            "/empty_cells.xlsx".resourceStream()
        )

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
            "/worksheets.xlsx".resourceStream()
        )

        val records = retable.records.toList()
        println(records)
        expectThat(records)
            .hasSize(2)
            .contains(
                RetableRecord(
                    retable.columns,
                    1,
                    2,
                    listOf("Xavier", "Hanin")
                )
            )
    }

    @Test
    fun `should read xlsx on selected sheet by name`() {
        val retable = Retable.excel(options = ExcelReadOptions(sheetName = "Last")).read(
            "/worksheets.xlsx".resourceStream()
        )

        val records = retable.records.toList()
        expectThat(records)
            .hasSize(2)
            .contains(
                RetableRecord(
                    retable.columns,
                    1,
                    2,
                    listOf("Alfred", "Dalton")
                )
            )
    }

    @Test
    fun `should export with hyperlink`() {
        val resultFilePath = pathTo("export_with_hyperlink.xlsx")

        Retable(
            RetableColumns.ofNames(listOf("first_name", "last_name", "url"))
        )
            .data(
                listOf(
                    listOf("John", "Doe", "https://google.fr"),
                    listOf("Jenny", "Boe", "http://4sh.fr")
                )
            )
            .write(Retable.excel() to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get { exists() }.isTrue()
        }
    }

    @Test
    fun `should export with hyperlink as raw string`() {
        val resultFilePath = pathTo("export_with_raw_hyperlink.xlsx")
        val columns = object : RetableColumns() {
            val FIRST_NAME = string("first_name", index = 2)
            val LAST_NAME = string("last_name", index = 1)
            val URL = string("url", index = 3, writeUrlAsHyperlink = false)
        }
        Retable(columns)
            .data(
                listOf(
                    listOf("John", "Doe", "https://google.fr"),
                    listOf("Jenny", "Boe", "http://4sh.fr")
                )
            )
            .write(Retable.excel(columns) to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get { exists() }.isTrue()
        }
    }

    @Test
    fun `should export with defined std value constraints`() {
        val resultFilePath = pathTo("export_with_std_value_constraints.xlsx")
        val columns = object : RetableColumns() {
            val COMPANY = string("company_name", index = 1, allowedValues = listOf("Google", "4sh"))
            val ENABLED = string("enabled", index = 2, allowedValues = listOf("true", "false"))
        }
        Retable(columns)
            .data(
                listOf(
                    listOf("Google", "true"),
                    listOf("4sh", "true")
                )
            )
            .write(Retable.excel(columns) to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get { exists() }.isTrue()
        }
    }

    @Test
    fun `should export with defined sheet based value constraints`() {
        val resultFilePath = pathTo("export_with_sheet_value_constraints.xlsx")
        val columns = object : RetableColumns() {
            val TRANSIT_TYPE = string("Type de trafic", allowedValues = listOf("IMPORT", "EXPORT"))
            val SLOTTER = string("Affréteur", allowedValues = (1..500).map { "Slotter $it" })
            val STOP_OVER = string("N° Escale IMPORT")
            val BL = string("N° BL")
            val FORWARDER = string("Transitaire / Réclamateur", allowedValues = (1..500).map { "Forwarder $it" })
            val VALIDITY = string("Date fin de validité")
        }
        Retable(columns)
            .data(
                listOf(
                    listOf("IMPORT", "Slotter 1", "", "", "Forwarder 1", "2021-01-01"),
                    listOf("IMPORT", "Slotter 2", "", "", "Forwarder 2", "2021-01-01"),
                    listOf("IMPORT", "Slotter 3", "", "", "Forwarder 3", "2021-01-01"),
                    listOf("IMPORT", "Slotter 4", "", "", "Forwarder 4", "2021-01-01"),
                    listOf("IMPORT", "Slotter 5", "", "", "Forwarder 5", "2021-01-01"),
                    listOf("IMPORT", "Slotter 6", "", "", "Forwarder 6", "2021-01-01")
                )
            )
            .write(Retable.excel(columns) to File(resultFilePath).outputStream())

        expectThat(File(resultFilePath)) {
            get { exists() }.isTrue()
        }
    }

    @Test
    fun `should list sheet names`() {
        val names = Retable.excel().sheetNames("/worksheets.xlsx".resourceStream())
        expectThat(names).isNotEmpty()
        expectThat(names).contains("Last")
    }

    @Test
    fun `should read all sheets`() {
        val sheets = Retable.excel().readAll("/worksheets.xlsx".resourceStream())
        expectThat(sheets).isNotEmpty()
        val lastSheet = sheets["Last"]!!
        expectThat(lastSheet.records.toList())
            .hasSize(2)
            .contains(RetableRecord(lastSheet.columns, 1, 2, listOf("Alfred", "Dalton")))
    }

    @Test
    fun `should write workbook with multiple sheets`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val sheet1 = Retable(cols).data(listOf(listOf("Xavier", "Hanin")))
        val sheet2 = Retable(cols).data(listOf(listOf("Alfred", "Dalton")))

        val resultPath = pathTo("multi_sheet_export.xlsx")
        Retable.excel().writeWorkbook(
            listOf("Feuille1" to sheet1, "Feuille2" to sheet2),
            File(resultPath).outputStream()
        )

        val readBack = Retable.excel().readAll(File(resultPath).inputStream())
        expectThat(readBack.keys.toList()).containsExactly("Feuille1", "Feuille2")
        expectThat(readBack["Feuille1"]!!.records.toList()).hasSize(1)
        expectThat(readBack["Feuille2"]!!.records.toList()).hasSize(1)
    }

    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
}
