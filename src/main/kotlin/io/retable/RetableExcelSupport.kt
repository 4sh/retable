package io.retable

import org.apache.poi.ss.usermodel.*
import java.io.InputStream
import java.text.SimpleDateFormat
import kotlin.math.roundToLong


class ExcelReadOptions(
        val sheetName:String? = null, // sheet name (used in priority over sheet index, if provided)
        val sheetIndex:Int? = null, // sheet index (one based)
        trimValues:Boolean = true,
        ignoreEmptyLines:Boolean = true,
        firstRecordAsHeader:Boolean = true)
    : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)

class RetableExcelSupport<T : RetableColumns>(
        columns: T, options:ExcelReadOptions = ExcelReadOptions())
    : BaseSupport<T, ExcelReadOptions>(columns, options) {
    override fun iterator(input: InputStream): Iterator<List<String>> {
        val workbook = WorkbookFactory.create(input)
        val sheet = if (options.sheetName != null) workbook.getSheet(options.sheetName)
                        else workbook.getSheetAt((options.sheetIndex?:1) - 1)
        val rowIterator = sheet?.rowIterator()?:
                throw IllegalStateException("worksheet `${options.sheetName?:options.sheetIndex?:1}` not found")

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
                                    else if (this.numericCellValue.isInteger())
                                        {
                                            this.numericCellValue.roundToLong().toString()
                                        }
                                    else
                                        {
                                            this.numericCellValue.toString().replace(Regex("\\.0$"), "")
                                        }

            CellType.FORMULA    -> try { this.numericCellValue.toString() } catch (e:Exception) { this.stringCellValue }
            else                -> this.stringCellValue
        }
    }

    fun Double.isInteger(): Boolean = this.roundToLong().toDouble() == this
}