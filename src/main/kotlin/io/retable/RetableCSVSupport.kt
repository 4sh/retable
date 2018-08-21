package io.retable

import org.apache.commons.csv.CSVFormat
import java.io.InputStream
import java.io.InputStreamReader

class RetableCSVSupport<T : RetableColumns>(val columns: T) {
    private val format: CSVFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()

    /**
     * Parses the input from the reader as a CSV file.
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun read(input: InputStream): Retable<T> {
        val parse = format.parse(InputStreamReader(input, Charsets.UTF_8))
        val iterator = parse.iterator()

        val headers = parse.headerMap
        val columns = if (this.columns is ListRetableColumns && this.columns.list().size == 0) {
            RetableColumns.ofNames(
                    (0..headers.size - 1)
                            .map { index -> headers.entries.find { it.value == index } }
                            .map { it?.key ?: "" }
                            .toList())
        } else {
            this.columns
        }

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

        return Retable(columns as T, records)
    }
}