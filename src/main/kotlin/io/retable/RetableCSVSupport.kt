package io.retable

import org.apache.commons.csv.CSVFormat
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class CSVReadOptions(
        val charset:Charset = Charsets.UTF_8,
        val delimiter:Char = ',',
        val escape:Char? = null,
        val quote:Char = '"',
        trimValues:Boolean = true,
        ignoreEmptyLines:Boolean = true,
        firstRecordAsHeader:Boolean = true
)
    : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)


class RetableCSVSupport<T : RetableColumns>(
        val columns: T,
        val options:CSVReadOptions = CSVReadOptions()
) {

    private val format: CSVFormat

    init {
        format = CSVFormat
                .newFormat(options.delimiter)
                .withEscape(options.escape)
                .withQuote(options.quote)
                .withIgnoreEmptyLines(options.ignoreEmptyLines)
                .withTrim(options.trimValues)
                .let { if (options.firstRecordAsHeader) it.withFirstRecordAsHeader() else it }
    }

    /**
     * Parses the input from the reader as a CSV file.
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun read(input: InputStream): Retable<T> {
        val parse = format.parse(InputStreamReader(input, options.charset))
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