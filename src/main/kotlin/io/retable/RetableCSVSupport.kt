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
    }

    /**
     * Parses the input from the reader as a CSV file.
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun read(input: InputStream): Retable<T> {
        var columns:RetableColumns? = null
        val records = iterator(input, { columns?:this.columns })

        if (!options.firstRecordAsHeader && columns == RetableColumns.auto) {
            throw IllegalStateException("columns are mandatory when not using first record as header")
        }

        val validations:RetableValidations
        if (options.firstRecordAsHeader) {
            val header = if (records.hasNext()) { records.next() } else { null }
            if (header == null) {
                throw IllegalStateException("empty file not allowed when first record is expected to be the header")
            }
            val headers = Headers(header.rawData)

            if (this.columns == RetableColumns.auto) {
                columns = RetableColumns.ofNames(headers.headers)
                validations = RetableValidations(listOf())
            } else {
                columns = this.columns
                validations = RetableValidations(
                        columns.list().map { col -> col.headerValidation.validate(headers) }
                )
            }
        } else {
            columns = this.columns
            validations = RetableValidations(listOf())
        }

        return Retable(columns as T, records.asSequence(), validations)
    }

    fun iterator(input: InputStream, cols:()->RetableColumns): Iterator<RetableRecord> {
        val parse = format.parse(InputStreamReader(input, options.charset))
        val iterator = parse.iterator()
        val records = object : Iterator<RetableRecord> {
            var lineNumber: Long = 0

            override fun hasNext(): Boolean {
                // we have to store the line number here, because calling hasNext reads the input
                lineNumber = parse.currentLineNumber
                return iterator.hasNext()
            }

            override fun next(): RetableRecord {
                val next = iterator.next()

                return RetableRecord(cols.invoke(),
                        if (options.firstRecordAsHeader) next.recordNumber - 1 else next.recordNumber,
                        lineNumber + 1, next.toList())
            }
        }
        return records
    }
}