package io.retable

import org.apache.commons.csv.CSVFormat
import java.io.Reader


class Retable {
    companion object {
        fun csv() = RetableCSVParser()
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
