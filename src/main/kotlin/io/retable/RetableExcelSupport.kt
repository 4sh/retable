package io.retable

import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.text.SimpleDateFormat


class ExcelReadOptions(trimValues:Boolean = true,
                       ignoreEmptyLines:Boolean = true,
                       firstRecordAsHeader:Boolean = true)
    : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)

class RetableExcelSupport<T : RetableColumns>(
        columns: T, options:ExcelReadOptions = ExcelReadOptions())
    : BaseSupport<T, ExcelReadOptions>(columns, options) {
    override fun iterator(input: InputStream): Iterator<List<String>> {
        val workbook = WorkbookFactory.create(input)
        val sheet = workbook.getSheetAt(0)
        val rowIterator = sheet.rowIterator()

        return object : Iterator<List<String>> {
            override fun hasNext(): Boolean {
                return rowIterator.hasNext()
            }

            override fun next(): List<String> {
                return rowIterator.next().cellIterator().asSequence()
                        .map { it.asStringValue() }
                        .map { if (options.trimValues) { it.trim() } else { it }}
                        .toList()
            }
        }
    }

    fun Cell.asStringValue():String {
        return when (this.cellTypeEnum) {
            CellType.NUMERIC    ->  if (DateUtil.isCellDateFormatted(this))
                                        {
                                            val  d = SimpleDateFormat("yyyy-MM-dd").format(this.dateCellValue)
                                            if (d == "1899-12-31") {
                                                return SimpleDateFormat("HH:mm:SS").format(this.dateCellValue)
                                            } else {
                                                return d
                                            }
                                        }
                                    else
                                        {
                                            this.numericCellValue.toString().replace(Regex("\\.0$"), "")
                                        }

            CellType.FORMULA    -> try { this.numericCellValue.toString() } catch (e:Exception) { this.stringCellValue }
            else                -> this.stringCellValue
        }
    }
}