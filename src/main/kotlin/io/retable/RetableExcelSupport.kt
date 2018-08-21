package io.retable

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class RetableExcelSupport<T : RetableColumns>(val columns: T) {
    fun read(input: InputStream): Retable<T> {
        val workbook = WorkbookFactory.create(input)

        val sheet = workbook.getSheetAt(0)

        val rowIterator = sheet.rowIterator()

        var lineNumber:Long = 0
        val header = rowIterator.next()

        val columns = if (this.columns is ListRetableColumns && this.columns.list().size == 0) {
            RetableColumns.ofNames(
                    header.cellIterator().asSequence()
                            .map { it.stringCellValue }
                            .toList())
        } else {
            this.columns
        }

        lineNumber++

        val records = object : Iterator<RetableRecord> {
            private var recordNumber: Long = 0
            private var row: Row? = null

            override fun hasNext(): Boolean {
                while (rowIterator.hasNext()) {
                    row = rowIterator.next()
                    lineNumber++
                    if (!row!!.getCell(0).stringCellValue.trim().isEmpty()) {
                        recordNumber++
                        return true
                    } else {
                        row = null // don't keep current empty row in our state
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
                return RetableRecord(columns, recordNumber, lineNumber,
                        row!!.cellIterator().asSequence().map { it.stringCellValue }.toList())
            }
        }.asSequence()

        return Retable(columns as T, records)
    }
}