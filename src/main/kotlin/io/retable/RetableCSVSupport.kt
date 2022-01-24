package io.retable

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset

class CSVReadOptions(
    val charset: Charset = Charsets.UTF_8,
    val delimiter: Char = ',',
    val escape: Char? = null,
    val quote: Char? = '"',
    val lineSeparator: String = "\r\n",
    trimValues: Boolean = true,
    ignoreEmptyLines: Boolean = true,
    firstRecordAsHeader: Boolean = true
) : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)


class RetableCSVSupport<T : RetableColumns>(
    columns: T,
    options: CSVReadOptions = CSVReadOptions()
) : BaseSupport<T, CSVReadOptions>(columns, options) {

    private val format: CSVFormat = CSVFormat
        .newFormat(options.delimiter)
        .withEscape(options.escape)
        .withQuote(options.quote)
        .withIgnoreEmptyLines(false) // this is handled by base support
        .withTrim(false) // this is handled by base support
        .withRecordSeparator(options.lineSeparator)

    override fun iterator(input: InputStream): Iterator<List<String>> {
        val parse = format.parse(InputStreamReader(input, options.charset))
        val iterator = parse.iterator()
        val records = object : Iterator<List<String>> {
            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            override fun next(): List<String> {
                val next = iterator.next()
                return next.toList()
            }
        }
        return records
    }

    override fun write(columns: T, records: Sequence<RetableRecord>, outputStream: OutputStream) {
        val sortedColumns = columns.list()
            .sortedBy { it.index }
        val csvPrinter = CSVPrinter(OutputStreamWriter(outputStream), format
            .withHeader(*sortedColumns
                .map { it.name }
                .toTypedArray()))

        records.forEach { record ->
            csvPrinter.printRecord(record.rawData)
        }

        csvPrinter.flush()
        csvPrinter.close()
    }
}
