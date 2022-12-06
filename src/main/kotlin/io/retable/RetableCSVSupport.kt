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
    val removeFirstFormulaChars: Boolean = false,
    trimValues: Boolean = true,
    ignoreEmptyLines: Boolean = true,
    firstRecordAsHeader: Boolean = true
) : ReadOptions(trimValues, ignoreEmptyLines, firstRecordAsHeader)

class RetableCSVSupport<T : RetableColumns>(
    columns: T,
    override val options: CSVReadOptions = CSVReadOptions()
) : BaseSupport<T, CSVReadOptions>(columns, options) {

    private val format: CSVFormat = CSVFormat.Builder.create()
        .setDelimiter(options.delimiter)
        .setEscape(options.escape)
        .setQuote(options.quote)
        .setIgnoreEmptyLines(false) // this is handled by base support
        .setTrim(false) // this is handled by base support
        .setRecordSeparator(options.lineSeparator)
        .build()

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
        val csvPrinter = CSVPrinter(
            OutputStreamWriter(outputStream),
            CSVFormat.Builder.create(format)
                .setHeader(
                    *sortedColumns
                        .map { it.name }
                        .toTypedArray()
                )
                .build()
        )

        records.forEach { record ->
            var cleanedRecordRawData = record.rawData
            if (options.removeFirstFormulaChars) {
                cleanedRecordRawData = record.rawData.map {
                    it.replace("^(=|\\+|-|@|0x09|0x0D)*".toRegex(), "")
                }
            }
            csvPrinter.printRecord(cleanedRecordRawData)
        }

        csvPrinter.flush()
        csvPrinter.close()
    }
}
