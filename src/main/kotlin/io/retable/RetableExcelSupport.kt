package io.retable

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream


class ExcelReadOptions(trimValues:Boolean = true,
                       ignoreEmptyLines:Boolean = true,
                       firstRecordAsHeader:Boolean = true)
    : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)

class RetableExcelSupport<T : RetableColumns>(
        columns: T, options:ExcelReadOptions = ExcelReadOptions())
    : BaseSupport<T, ExcelReadOptions>(columns, options) {
    override fun iterator(input: InputStream, cols: () -> RetableColumns): Iterator<RetableRecord> {
        val workbook = WorkbookFactory.create(input)

        val sheet = workbook.getSheetAt(0)

        val rowIterator = sheet.rowIterator()

        return object : Iterator<RetableRecord> {
            private var lineNumber:Long = 0
            private var recordNumber: Long = 0
            private var row: Row? = null
            private var headerLoaded: Boolean = false

            override fun hasNext(): Boolean {

                while (rowIterator.hasNext()) {
                    row = rowIterator.next()
                    lineNumber++

                    if (options.ignoreEmptyLines && row!!.getCell(0).stringCellValue.trim().isEmpty()) {
                        row = null // don't keep current empty row in our state
                    } else {
                        if (options.firstRecordAsHeader && !headerLoaded) {
                            headerLoaded = true
                        } else {
                            recordNumber++
                        }

                        return true
                    }
                }

                return false
            }

            override fun next(): RetableRecord {
                if (row == null) {
                    if (!hasNext()) {
                        throw IllegalStateException("no more rows")
                    }
                }
                return RetableRecord(cols.invoke(), recordNumber, lineNumber,
                        row!!.cellIterator().asSequence()
                                .map { it.stringCellValue }
                                .map { if (options.trimValues) { it.trim() } else { it }}
                                .toList())
            }
        }
    }
}