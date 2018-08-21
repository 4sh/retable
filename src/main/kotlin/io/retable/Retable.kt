package io.retable

import org.apache.commons.csv.CSVFormat
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStreamReader


class Retable(val columns:RetableColumns,
                val records:Sequence<RetableRecord>) {
    companion object {
        fun csv() = RetableCSVSupport()
        fun excel() = RetableExcelSupport()
    }
}

class RetableExcelSupport {
    fun read(input:InputStream):Retable {
        val workbook = WorkbookFactory.create(input)

        val sheet = workbook.getSheetAt(0)

        val rowIterator = sheet.rowIterator()

        var lineNumber:Long = 0
        val header = rowIterator.next()

        val columns = header.cellIterator().asSequence()
                .map { it.stringCellValue }
                .map { RetableColumn(it) }
                .toList()

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

        return Retable(columns, records)
    }
}

class RetableCSVSupport {
    private val format:CSVFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()

    /**
     * Parses the input from the reader as a CSV file.
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun read(input:InputStream):Retable {
        val parse = format.parse(InputStreamReader(input, Charsets.UTF_8))
        val iterator = parse.iterator()

        val headers = parse.headerMap
        val columns = (0 .. headers.size - 1)
                .map { index -> headers.entries.find { it.value == index } }
                .filterNotNull()
                .map { it.key }
                .map { RetableColumn(it) }
                .toList()


        val records = object : Iterator<RetableRecord> {
            var lineNumber: Long = 0

            override fun hasNext(): Boolean {
                // we have to store the line number here, because calling hasNext reads the input
                lineNumber = parse.currentLineNumber
                return iterator.hasNext()
            }

            override fun next(): RetableRecord {
                val next = iterator.next()

                return RetableRecord(columns, next.recordNumber, lineNumber + 1, next.toList())
            }
        }.asSequence()

        return Retable(columns, records)
    }
}

typealias RetableColumns = List<RetableColumn>

data class RetableColumn(val name:String) {

}

data class RetableRecord(val columns: RetableColumns,
                         val recordNumber: Long,
                         val lineNumber: Long,
                         val rawData: List<String>
                         ) {
    operator fun get(c:String):String? {
        return columns.mapIndexed({index, column -> RetableData(column, rawData[index])})
                .find { it.column.name == c }
                ?.let { it.value }
    }
}

data class RetableData(val column: RetableColumn, val value:String)
