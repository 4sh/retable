package io.retable

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.util.DefaultTempFileCreationStrategy
import org.apache.poi.util.TempFile
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
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

    @Test
    fun `should read all sheets ignoring empty sheets`() {
        val resultPath = pathTo("multi_sheet_with_empty_sheet_export.xlsx")
        XSSFWorkbook().use { workbook ->
            workbook.createSheet("EmptySheet")
            workbook.createSheet("BlankRowsSheet").also { blankSheet ->
                // rows exist but have no non-blank cell: should be treated as empty too
                blankSheet.createRow(0).createCell(0).setCellValue("")
                blankSheet.createRow(1)
            }
            val dataSheet = workbook.createSheet("Feuille1")
            dataSheet.createRow(0).also { header ->
                header.createCell(0).setCellValue("first_name")
                header.createCell(1).setCellValue("last_name")
            }
            dataSheet.createRow(1).also { row ->
                row.createCell(0).setCellValue("Xavier")
                row.createCell(1).setCellValue("Hanin")
            }
            File(resultPath).outputStream().use { workbook.write(it) }
        }

        val readBack = Retable.excel().readAll(File(resultPath).inputStream())
        expectThat(readBack.keys.toList()).containsExactly("Feuille1")
        expectThat(readBack["Feuille1"]!!.records.toList()).hasSize(1)
    }

    @Test
    fun `should write same content with streaming workbook`() {
        val columns = object : RetableColumns() {
            val NAME = string("name", index = 1)
            val AGE = int("age", index = 2)
            val BIRTH = localDate("birth", index = 3)
            val URL = string("url", index = 4)
        }
        val data = (1..1_000).map { listOf("Name $it", it, LocalDate.of(2000, 1, 1).plusDays(it.toLong()), "https://4sh.fr/$it") }
        fun export(fileName: String, options: ExcelReadOptions): List<List<String>> {
            val resultPath = pathTo(fileName)
            Retable(columns)
                .data(data) { mapOf(NAME to it[0], AGE to it[1], BIRTH to it[2], URL to it[3]) }
                .write(Retable.excel(columns, options) to File(resultPath).outputStream())
            return Retable.excel().read(File(resultPath).inputStream()).records.map { it.rawData }.toList()
        }

        val classic = export("export_classic.xlsx", ExcelReadOptions())
        val streamed = export(
            "export_streaming.xlsx",
            ExcelReadOptions(useStreamingWorkbook = true, streamingRowAccessWindowSize = 10)
        )
        val streamedFasterAutoSize = export(
            "export_streaming_faster_autosize.xlsx",
            ExcelReadOptions(
                useStreamingWorkbook = true,
                streamingRowAccessWindowSize = 10,
                useExperimentalFasterAutoSizeColumn = true
            )
        )

        val expected = classic
        expectThat(expected).hasSize(1_000)
        expectThat(streamed).isEqualTo(expected)
        expectThat(streamedFasterAutoSize).isEqualTo(expected)

        val classicWidths = XSSFWorkbook(File(pathTo("export_classic.xlsx"))).use { workbook ->
            (0..3).map { workbook.getSheetAt(0).getColumnWidth(it) }
        }
        XSSFWorkbook(File(pathTo("export_streaming.xlsx"))).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            expectThat(sheet.getRow(1000).getCell(3).hyperlink?.address).isEqualTo("https://4sh.fr/1000")
            // POI autosize on tracked columns gives the same widths as on a whole in memory sheet,
            // give or take 1/256 of a character (POI rounds differently in SXSSF)
            (0..3).forEach { index ->
                expectThat(sheet.getColumnWidth(index)).isIn(classicWidths[index] - 1..classicWidths[index] + 1)
            }
            expectThat(sheet.getColumnWidth(0)).isNotEqualTo(sheet.defaultColumnWidth * 256)
        }
    }

    @Test
    fun `should export sheet based value constraints with streaming workbook`() {
        val resultFilePath = pathTo("export_with_sheet_value_constraints_streaming.xlsx")
        val columns = object : RetableColumns() {
            val SLOTTER = string("Affréteur", allowedValues = (1..500).map { "Slotter $it" })
            val FORWARDER = string("Transitaire", allowedValues = (1..300).map { "Forwarder $it" })
        }
        Retable(columns)
            .data(listOf(listOf("Slotter 1", "Forwarder 1")))
            .write(
                Retable.excel(columns, ExcelReadOptions(useStreamingWorkbook = true))
                    to File(resultFilePath).outputStream()
            )

        XSSFWorkbook(File(resultFilePath)).use { workbook ->
            expectThat(workbook.getSheetAt(0).dataValidations).hasSize(2)
            val constraintSheet = workbook.getSheet("_constraints_Sheet0")
            expectThat(workbook.isSheetHidden(workbook.getSheetIndex(constraintSheet))).isTrue()
            expectThat(constraintSheet.getRow(499).getCell(0).stringCellValue).isEqualTo("Slotter 500")
            expectThat(constraintSheet.getRow(299).getCell(1).stringCellValue).isEqualTo("Forwarder 300")
            expectThat(constraintSheet.getRow(300).getCell(1)).isNull()
        }
    }

    @Test
    fun `should write workbook with multiple sheets with streaming workbook`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val sheet1 = Retable(cols).data((1..300).map { listOf("Xavier $it", "Hanin") })
        val sheet2 = Retable(cols).data(listOf(listOf("Alfred", "Dalton")))

        val resultPath = pathTo("multi_sheet_streaming_export.xlsx")
        Retable.excel(ExcelReadOptions(useStreamingWorkbook = true)).writeWorkbook(
            listOf("Feuille1" to sheet1, "Feuille2" to sheet2),
            File(resultPath).outputStream()
        )

        val readBack = Retable.excel().readAll(File(resultPath).inputStream())
        expectThat(readBack.keys.toList()).containsExactly("Feuille1", "Feuille2")
        expectThat(readBack["Feuille1"]!!.records.toList()).hasSize(300)
        expectThat(readBack["Feuille2"]!!.records.toList()).hasSize(1)
    }

    @Test
    fun `should write with compressed streaming temp files`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val resultPath = pathTo("export_streaming_compressed.xlsx")
        Retable(cols)
            .data((1..300).map { listOf("Xavier $it", "Hanin") })
            .write(
                Retable.excel(
                    cols,
                    ExcelReadOptions(
                        useStreamingWorkbook = true,
                        streamingRowAccessWindowSize = 10,
                        compressStreamingTempFiles = true
                    )
                ) to File(resultPath).outputStream()
            )

        val records = Retable.excel().read(File(resultPath).inputStream()).records.toList()
        expectThat(records).hasSize(300)
        expectThat(records.last().rawData).containsExactly("Xavier 300", "Hanin")
    }

    @Test
    fun `should delete streaming temp files once written`() {
        val tempDir = Files.createTempDirectory("retable-sxssf").toFile()
        TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy(tempDir))
        try {
            val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
            val records = Retable(cols).data((1..300).map { listOf("Xavier $it", "Hanin") }).records
            var tempFilesWhileWriting = 0
            Retable(cols, records.onEach { if (it.recordNumber == 300L) tempFilesWhileWriting = tempDir.list()!!.size })
                .write(
                    Retable.excel(cols, ExcelReadOptions(useStreamingWorkbook = true, streamingRowAccessWindowSize = 10))
                        to ByteArrayOutputStream()
                )

            expectThat(tempFilesWhileWriting).isGreaterThan(0)
            expectThat(tempDir.list()!!.toList()).isEmpty()
        } finally {
            TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy())
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `should reject out of order records with streaming workbook`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val records = Retable(cols).data(listOf(listOf("Xavier", "Hanin"), listOf("Alfred", "Dalton"))).records
        val unordered = Retable(cols, records.toList().reversed().asSequence())

        // both rows still fit in the row access window: POI alone would not fail here
        expectThrows<IllegalArgumentException> {
            unordered.write(
                Retable.excel(cols, ExcelReadOptions(useStreamingWorkbook = true)) to ByteArrayOutputStream()
            )
        }.message.isNotNull().contains("ascending line order")
    }

    @Test
    fun `should export empty records with faster autosize`() {
        val resultPath = pathTo("export_empty_faster_autosize.xlsx")
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        Retable(cols)
            .data(emptyList<List<String>>())
            .write(
                Retable.excel(cols, ExcelReadOptions(useExperimentalFasterAutoSizeColumn = true))
                    to File(resultPath).outputStream()
            )

        XSSFWorkbook(File(resultPath)).use { workbook ->
            expectThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(0)
        }
    }

    @Test
    fun `streaming export should keep cell types and styles of classic export`() {
        val columns = object : RetableColumns() {
            val NAME = string("name", index = 1)
            val AGE = int("age", index = 2)
            val BIRTH = localDate("birth", index = 3)
            val URL = string("url", index = 5)
            val RATIO = double("ratio", index = 6)
        }
        val retable = Retable(columns).data((1..50).toList()) {
            mapOf(
                NAME to if (it % 7 == 0) "" else "Name $it",
                AGE to it,
                BIRTH to LocalDate.of(2000, 1, 1).plusDays(it.toLong()),
                URL to "https://4sh.fr/$it",
                RATIO to it * 0.25
            )
        }
        val classicPath = pathTo("export_types_classic.xlsx")
        val streamingPath = pathTo("export_types_streaming.xlsx")
        retable.write(Retable.excel(columns) to File(classicPath).outputStream())
        retable.write(
            Retable.excel(columns, ExcelReadOptions(useStreamingWorkbook = true, streamingRowAccessWindowSize = 5))
                to File(streamingPath).outputStream()
        )

        val classic = describeCells(File(classicPath))
        expectThat(classic).hasSize(51 * 6)
        expectThat(describeCells(File(streamingPath))).isEqualTo(classic)
    }

    private fun describeCells(file: File): List<String> =
        XSSFWorkbook(file).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            (0..50).flatMap { rowIndex ->
                val row = sheet.getRow(rowIndex)
                (0..5).map { cellIndex ->
                    val prefix = "row $rowIndex cell $cellIndex"
                    val cell = row?.getCell(cellIndex)
                    if (cell == null) {
                        "$prefix: null"
                    } else {
                        val value = when (cell.cellType) {
                            CellType.STRING -> cell.stringCellValue
                            CellType.NUMERIC -> cell.numericCellValue.toString()
                            else -> cell.toString()
                        }
                        "$prefix: type=${cell.cellType} value=$value " +
                            "format=${cell.cellStyle.dataFormatString} wrap=${cell.cellStyle.wrapText} " +
                            "underline=${workbook.getFontAt(cell.cellStyle.fontIndex).underline} " +
                            "link=${cell.hyperlink?.address}"
                    }
                }
            }
        }

    @Test
    fun `streaming export should write inline value constraints`() {
        val columns = object : RetableColumns() {
            val CODE = string("code", index = 1, allowedValues = listOf("A", "B", "C"))
        }
        val path = pathTo("export_inline_constraints_streaming.xlsx")
        Retable(columns)
            .data(listOf(listOf("A"), listOf("B"), listOf("C")))
            .write(Retable.excel(columns, ExcelReadOptions(useStreamingWorkbook = true)) to File(path).outputStream())

        XSSFWorkbook(File(path)).use { workbook ->
            expectThat(workbook.numberOfSheets).isEqualTo(1)
            val validations = workbook.getSheetAt(0).dataValidations
            expectThat(validations).hasSize(1)
            expectThat(validations[0].validationConstraint.explicitListValues.toList())
                .containsExactly("A", "B", "C")
        }
    }

    @Test
    fun `classic export should write sheet based value constraints of unequal sizes`() {
        val columns = object : RetableColumns() {
            val SLOTTER = string("slotter", index = 1, allowedValues = (1..300).map { "Slotter $it" })
            val FORWARDER = string("forwarder", index = 2, allowedValues = (1..500).map { "Forwarder $it" })
        }
        val path = pathTo("export_sheet_constraints_unequal_classic.xlsx")
        Retable(columns)
            .data(listOf(listOf("Slotter 1", "Forwarder 1")))
            .write(Retable.excel(columns) to File(path).outputStream())

        XSSFWorkbook(File(path)).use { workbook ->
            val constraintSheet = workbook.getSheet("_constraints_Sheet0")
            expectThat(constraintSheet.lastRowNum).isEqualTo(499)
            expectThat(constraintSheet.getRow(299).getCell(0).stringCellValue).isEqualTo("Slotter 300")
            expectThat(constraintSheet.getRow(300).getCell(0)).isNull()
            expectThat(constraintSheet.getRow(499).getCell(1).stringCellValue).isEqualTo("Forwarder 500")
            expectThat(workbook.getName("_constraints_Sheet0_1")!!.refersToFormula)
                .isEqualTo("'_constraints_Sheet0'!\$B\$1:\$B\$500")
            expectThat(workbook.getName("_constraints_Sheet0_0")!!.refersToFormula)
                .isEqualTo("'_constraints_Sheet0'!\$A\$1:\$A\$300")
        }
    }

    @Test
    fun `streaming writeWorkbook should write sheet based value constraints on each sheet`() {
        val columns = object : RetableColumns() {
            val CODE = string("code", index = 1, allowedValues = (1..500).map { "Code $it" })
        }
        val sheetA = Retable(columns).data(listOf(listOf("Code 1")))
        val sheetB = Retable(columns).data(listOf(listOf("Code 2")))
        val path = pathTo("multi_sheet_streaming_constraints.xlsx")
        Retable.excel(ExcelReadOptions(useStreamingWorkbook = true)).writeWorkbook(
            listOf("A" to sheetA, "B" to sheetB),
            File(path).outputStream()
        )

        XSSFWorkbook(File(path)).use { workbook ->
            for (name in listOf("_constraints_A", "_constraints_B")) {
                val index = workbook.getSheetIndex(name)
                expectThat(index).isGreaterThan(-1)
                expectThat(workbook.isSheetHidden(index)).isTrue()
            }
            expectThat(workbook.getSheet("A").dataValidations).hasSize(1)
            expectThat(workbook.getSheet("B").dataValidations).hasSize(1)
        }
    }

    @Test
    fun `streaming export should delete temp files when writing fails`() {
        val tempDir = Files.createTempDirectory("retable-sxssf-failure").toFile()
        TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy(tempDir))
        try {
            val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
            val records = Retable(cols).data((1..300).map { listOf("Xavier $it", "Hanin") }).records
                .map { if (it.recordNumber == 200L) throw IllegalStateException("boom") else it }

            expectThrows<IllegalStateException> {
                Retable(cols, records).write(
                    Retable.excel(cols, ExcelReadOptions(useStreamingWorkbook = true, streamingRowAccessWindowSize = 10))
                        to ByteArrayOutputStream()
                )
            }
            expectThat(tempDir.list()!!.toList()).isEmpty()
        } finally {
            TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy())
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `streaming export should accept gaps in line numbers`() {
        val cols = RetableColumns.ofNames(listOf("first_name"))
        val values = listOf(2L to "Xavier", 5L to "Alfred", 9L to "Hugo", 200L to "Victor")
        val records = values.mapIndexed { index, (line, name) -> RetableRecord(cols, index + 1L, line, listOf(name)) }
        val path = pathTo("export_streaming_line_gaps.xlsx")
        Retable(cols, records.asSequence()).write(
            Retable.excel(cols, ExcelReadOptions(useStreamingWorkbook = true, streamingRowAccessWindowSize = 2))
                to File(path).outputStream()
        )

        XSSFWorkbook(File(path)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            expectThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("Xavier")
            expectThat(sheet.getRow(2)).isNull()
            expectThat(sheet.getRow(4).getCell(0).stringCellValue).isEqualTo("Alfred")
            expectThat(sheet.getRow(8).getCell(0).stringCellValue).isEqualTo("Hugo")
            expectThat(sheet.getRow(199).getCell(0).stringCellValue).isEqualTo("Victor")
        }
    }

    @Test
    fun `streaming export should work with a row access window of 1`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val path = pathTo("export_streaming_window_1.xlsx")
        Retable(cols)
            .data((1..100).map { listOf("Xavier $it", "Hanin") })
            .write(
                Retable.excel(cols, ExcelReadOptions(useStreamingWorkbook = true, streamingRowAccessWindowSize = 1))
                    to File(path).outputStream()
            )

        val records = Retable.excel().read(File(path).inputStream()).records.toList()
        expectThat(records).hasSize(100)
        expectThat(records.last().rawData).containsExactly("Xavier 100", "Hanin")
    }

    @Test
    fun `streaming export of empty records with POI autosize`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val path = pathTo("export_streaming_empty.xlsx")
        Retable(cols)
            .data(emptyList<List<String>>())
            .write(Retable.excel(cols, ExcelReadOptions(useStreamingWorkbook = true)) to File(path).outputStream())

        XSSFWorkbook(File(path)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            expectThat(sheet.lastRowNum).isEqualTo(0)
            expectThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("first_name")
            expectThat(sheet.getRow(0).getCell(1).stringCellValue).isEqualTo("last_name")
        }
    }

    @Test
    fun `classic export should still reorder out of order records`() {
        val cols = RetableColumns.ofNames(listOf("first_name", "last_name"))
        val records = Retable(cols).data(listOf(listOf("Xavier", "Hanin"), listOf("Alfred", "Dalton"))).records
        val path = pathTo("export_classic_out_of_order.xlsx")
        Retable(cols, records.toList().reversed().asSequence())
            .write(Retable.excel(cols) to File(path).outputStream())

        XSSFWorkbook(File(path)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            expectThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("Xavier")
            expectThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("Alfred")
        }
    }

    @Test
    fun `faster autosize should give same widths in streaming and classic`() {
        val columns = RetableColumns.ofNames(listOf("first", "second", "third"))
        val data = (1..200).map {
            listOf("Name ${"x".repeat(it % 37)} $it", "y".repeat(it % 23), "z".repeat(it % 11) + it)
        }
        val classicPath = pathTo("export_faster_autosize_classic.xlsx")
        val streamingPath = pathTo("export_faster_autosize_streaming.xlsx")
        Retable(columns)
            .data(data)
            .write(
                Retable.excel(columns, ExcelReadOptions(useExperimentalFasterAutoSizeColumn = true))
                    to File(classicPath).outputStream()
            )
        Retable(columns)
            .data(data)
            .write(
                Retable.excel(
                    columns,
                    ExcelReadOptions(
                        useStreamingWorkbook = true,
                        streamingRowAccessWindowSize = 10,
                        useExperimentalFasterAutoSizeColumn = true
                    )
                ) to File(streamingPath).outputStream()
            )

        val classicWidths = XSSFWorkbook(File(classicPath)).use { workbook ->
            (0..2).map { workbook.getSheetAt(0).getColumnWidth(it) }
        }
        val streamingWidths = XSSFWorkbook(File(streamingPath)).use { workbook ->
            (0..2).map { workbook.getSheetAt(0).getColumnWidth(it) }
        }
        expectThat(streamingWidths).isEqualTo(classicWidths)
    }

    // helper extensions
    fun String.resourceStream() = RetableTest::class.java.getResourceAsStream(this)
}
