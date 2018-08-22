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
        columns: T,
        options:CSVReadOptions = CSVReadOptions()
) : BaseSupport<T, CSVReadOptions>(columns, options) {

    private val format: CSVFormat

    init {
        format = CSVFormat
                .newFormat(options.delimiter)
                .withEscape(options.escape)
                .withQuote(options.quote)
                .withIgnoreEmptyLines(options.ignoreEmptyLines)
                .withTrim(options.trimValues)
    }

    override fun iterator(input: InputStream, cols:()->RetableColumns): Iterator<RetableRecord> {
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