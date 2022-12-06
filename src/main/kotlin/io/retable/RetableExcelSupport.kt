package io.retable

import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToLong

class ExcelReadOptions(
    val sheetName: String? = null, // sheet name (used in priority over sheet index, if provided)
    val sheetIndex: Int? = null, // sheet index (one based)
    trimValues: Boolean = true,
    ignoreEmptyLines: Boolean = true,
    firstRecordAsHeader: Boolean = true,
    val defaultDateTimeFormatter: DateTimeFormatter? = null
) : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)

class RetableExcelSupport<T : RetableColumns>(
    columns: T,
    options: ExcelReadOptions = ExcelReadOptions()
) : BaseSupport<T, ExcelReadOptions>(columns, options) {
    override fun iterator(input: InputStream): Iterator<List<String>> {
        val workbook = WorkbookFactory.create(input)
        val sheet = if (options.sheetName != null) workbook.getSheet(options.sheetName)
        else workbook.getSheetAt((options.sheetIndex ?: 1) - 1)
        val rowIterator = sheet?.rowIterator()
            ?: throw IllegalStateException("worksheet `${options.sheetName ?: options.sheetIndex ?: 1}` not found")

        return object : Iterator<List<String>> {
            override fun hasNext(): Boolean {
                return rowIterator.hasNext()
            }

            override fun next(): List<String> {
                val row = rowIterator.next()
                val upperRange = if (columns.maxIndex == 0) row.lastCellNum.toInt() else columns.maxIndex - 1
                return (0..upperRange)
                    .map { row.getCell(it) }
                    .map { it?.asStringValue(options.defaultDateTimeFormatter) ?: "" }
                    .map {
                        if (options.trimValues) {
                            it.trim()
                        } else {
                            it
                        }
                    }
                    .toList()
                    .removeLastEmpty()
            }
        }
    }

    override fun write(columns: T, records: Sequence<RetableRecord>, outputStream: OutputStream) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet()

        val header = sheet.createRow(0)
        columns.list().forEach {
            val cell = header.createCell(it.index - 1)
            cell.setCellValue(it.name)
        }
        val styleText = workbook.createCellStyle()
        styleText.wrapText = true
        val styleDate = workbook.createCellStyle()
        styleDate.dataFormat = workbook.creationHelper.createDataFormat().getFormat("m/d/yy")

        val hyperlinkStyle = workbook.createCellStyle().also { style ->
            style.setFont(
                workbook.createFont().also {
                    it.underline = XSSFFont.U_SINGLE
                    it.color = IndexedColors.BLUE.index
                }
            )
        }

        records.forEachIndexed { index, record ->
            val row = sheet.createRow(record.lineNumber.toInt() - 1)
            columns.list().forEach { col ->
                record[col]?.let { value ->
                    val cell = row.createCell(col.index - 1)
                    when (value) {
                        is Number -> {
                            cell.setCellType(CellType.NUMERIC)
                            cell.setCellValue(value.toDouble())
                        }
                        is LocalDate -> writeLocalDateCell(workbook, cell, styleDate, value)
                        is Instant -> cell.setCellValue(Date(value.toEpochMilli()))
                        else -> {
                            val stringValue = value.toString()
                            cell.setCellValue(stringValue)
                            if (col.writeUrlAsHyperlink && stringValue.isUrl()) {
                                cell.hyperlink = workbook.creationHelper.createHyperlink(HyperlinkType.URL)
                                    .also { it.address = stringValue }
                                cell.cellStyle = hyperlinkStyle
                            } else {
                                cell.cellStyle = styleText
                            }
                        }
                    }
                }
            }
        }

        for (index in 0..columns.maxIndex - 1) {
            sheet.autoSizeColumn(index)
        }
        workbook.write(outputStream)
        workbook.close()
    }

    private fun writeLocalDateCell(workbook: XSSFWorkbook, cell: XSSFCell, style: XSSFCellStyle, value: LocalDate) {
        cell.cellStyle = style
        val calendar = Calendar.getInstance()
        calendar.set(value.year, value.month.value, value.dayOfMonth, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        cell.setCellValue(DateUtil.getExcelDate(calendar, false))
    }

    fun Cell.asStringValue(dateTimeFormatter: DateTimeFormatter?): String {
        return when (this.cellType) {
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(this)) {
                val d = SimpleDateFormat("yyyy-MM-dd").format(this.dateCellValue)
                return if (d == "1899-12-31") {
                    SimpleDateFormat("HH:mm:SS").format(this.dateCellValue)
                } else {
                    dateTimeFormatter?.format(DateTimeFormatter.ISO_LOCAL_DATE.parse(d)) ?: d
                }
            } else if (this.numericCellValue.isInteger()) {
                this.numericCellValue.roundToLong().toString()
            } else {
                this.numericCellValue.toString().replace(Regex("\\.0$"), "")
            }

            CellType.FORMULA -> try {
                this.numericCellValue.toString()
            } catch (e: Exception) {
                this.stringCellValue
            }
            else -> this.stringCellValue
        }
    }

    fun Double.isInteger(): Boolean = this.roundToLong().toDouble() == this

    private fun <E> List<E>.removeLastEmpty(): List<E> {
        // excel returns all cells which are not empty, it only depends on which cells have been created
        // at one point in time - even if clearer after. This behaviour can't be understood by end user who only see
        // empty cells - therefore we remove trailing empty cells in the list of rawData, and when accessing cells
        // exceeding index is handled anyway
        return this.subList(
            0,
            this.size - kotlin.math.max(
                0,
                this.reversed().indexOfFirst { !it?.toString().isNullOrBlank() }
            )
        )
    }

    private fun String.isUrl(): Boolean =
        take(7) == "http://" || take(8) == "https://"
}
