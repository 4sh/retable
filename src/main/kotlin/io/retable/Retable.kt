package io.retable

import org.apache.commons.csv.CSVFormat
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import java.io.Reader
import java.io.File
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.Workbook




class Retable {
    companion object {
        fun csv() = RetableCSVParser()
        fun excel() = RetableExcelReader()
    }
}

class RetableExcelReader {
    fun read(input:InputStream):Sequence<RetableRecord> {
        val workbook = WorkbookFactory.create(input)

        val sheet = workbook.getSheetAt(0)

        val rowIterator = sheet.rowIterator()

        var lineNumber:Long = 0
        val header = rowIterator.next()
        lineNumber++

        return object: Iterator<RetableRecord> {
            private var rowNumber:Long = 0
            private var row: Row? = null

            override fun hasNext(): Boolean {
                while (rowIterator.hasNext()) {
                    row = rowIterator.next()
                    lineNumber++
                    if (! row!!.getCell(0).stringCellValue.trim().isEmpty()) {
                        rowNumber++
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
                return RetableRecord(rowNumber, lineNumber,
                        row!!.cellIterator().asSequence().map { it.stringCellValue }.toList())
            }
        }.asSequence()
    }
}

class RetableCSVParser {
    private val format:CSVFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()

    /**
     * Parses the input from the reader as a CSV file.
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun parse(reader:Reader):Sequence<RetableRecord> {
        val parse = format.parse(reader)
        val iterator = parse.iterator()

        return object: Iterator<RetableRecord> {
            var lineNumber:Long = 0

            override fun hasNext(): Boolean {
                // we have to store the line number here, because calling hasNext reads the input
                lineNumber = parse.currentLineNumber
                return iterator.hasNext()
            }

            override fun next(): RetableRecord {
                val next = iterator.next()

                return RetableRecord(next.recordNumber, lineNumber + 1, next.toList())
            }
        }.asSequence()
    }
}

data class RetableRecord(val rowNumber: Long,
                         val lineNumber: Long,
                         val rawData: List<String>
                         ) {

}
