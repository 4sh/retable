package io.retable

import org.apache.poi.util.DefaultTempFileCreationStrategy
import org.apache.poi.util.TempFile
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.io.OutputStream
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.nio.file.Files
import java.time.LocalDate

/**
 * Run with `./gradlew perfTest` (256 MB heap, excluded from the default `test` task).
 * Nothing is written to disk except POI temporary files, the output is only counted.
 */
@Tag("perf")
class RetableStreamingPerformanceTest {

    class Cols : RetableColumns() {
        val FIRST = string("first", index = 1)
        val LAST = string("last", index = 2)
        val AGE = int("age", index = 3)
        val DATE = localDate("date", index = 4)
        val COMMENT = string("comment", index = 5)
        val URL = string("url", index = 6)
    }

    private class CountingOutputStream : OutputStream() {
        var bytes = 0L
        override fun write(b: Int) {
            bytes++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            bytes += len
        }
    }

    // lazily generated: the input data does not use heap
    private fun records(columns: Cols, count: Int, withUrl: Boolean): Sequence<RetableRecord> =
        (1..count).asSequence().map { i ->
            RetableRecord(
                columns,
                i.toLong(),
                i.toLong() + 1,
                listOf(
                    "First name $i",
                    "Last name $i",
                    "${i % 90}",
                    LocalDate.of(2000, 1, 1).plusDays((i % 3650).toLong()).toString(),
                    "Some free text comment for line $i",
                    if (withUrl) "https://example.org/doc/$i" else "doc $i"
                )
            )
        }

    private fun measure(label: String, block: () -> Unit): Long {
        System.gc()
        val heapPools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
        heapPools.forEach { it.resetPeakUsage() }
        val start = System.nanoTime()
        block()
        val ms = (System.nanoTime() - start) / 1_000_000
        val peakMb = heapPools.sumOf { it.peakUsage.used } / 1024 / 1024
        println("$label: $ms ms, heap peak ~$peakMb MB")
        return ms
    }

    private fun export(rows: Int, withUrl: Boolean, options: ExcelReadOptions): Long {
        val columns = Cols()
        val out = CountingOutputStream()
        Retable(columns, records(columns, rows, withUrl)).write(Retable.excel(columns, options) to out)
        return out.bytes
    }

    @Test
    fun `streaming export of 200,000 rows fits in a 256 MB heap`() {
        var bytes = 0L
        val ms = measure("streaming, 200,000 rows, faster autosize, no url") {
            bytes = export(
                200_000,
                withUrl = false,
                options = ExcelReadOptions(useStreamingWorkbook = true, useExperimentalFasterAutoSizeColumn = true)
            )
        }
        expectThat(bytes).isGreaterThan(0L)
        expectThat(ms).isLessThan(60_000L)
    }

    @Test
    fun `streaming export of 50,000 rows with POI autosize`() {
        var bytes = 0L
        val ms = measure("streaming, 50,000 rows, POI autosize, no url") {
            bytes = export(50_000, withUrl = false, options = ExcelReadOptions(useStreamingWorkbook = true))
        }
        expectThat(bytes).isGreaterThan(0L)
        expectThat(ms).isLessThan(120_000L)
    }

    // POI keeps every hyperlink in memory until the workbook is written (not streamed): 200,000 rows with a URL
    // column do not fit in a 256 MB heap, this test guards against a further increase of that memory cost
    @Test
    fun `streaming export of 50,000 rows with hyperlinks`() {
        var bytes = 0L
        val ms = measure("streaming, 50,000 rows, faster autosize, with hyperlinks") {
            bytes = export(
                50_000,
                withUrl = true,
                options = ExcelReadOptions(useStreamingWorkbook = true, useExperimentalFasterAutoSizeColumn = true)
            )
        }
        expectThat(bytes).isGreaterThan(0L)
        expectThat(ms).isLessThan(60_000L)
    }

    @Test
    fun `streaming should not be slower than classic export`() {
        val classicOptions = ExcelReadOptions(useExperimentalFasterAutoSizeColumn = true)
        val streamingOptions = ExcelReadOptions(useStreamingWorkbook = true, useExperimentalFasterAutoSizeColumn = true)
        // warm-up, so that JIT and class loading do not distort the measures
        export(2_000, withUrl = false, options = classicOptions)
        export(2_000, withUrl = false, options = streamingOptions)

        val classicMs = measure("classic, 20,000 rows, faster autosize, no url") {
            export(20_000, withUrl = false, options = classicOptions)
        }
        val streamingMs = measure("streaming, 20,000 rows, faster autosize, no url") {
            export(20_000, withUrl = false, options = streamingOptions)
        }
        expectThat(streamingMs).isLessThan(classicMs * 2 + 500)
    }

    @Test
    fun `compressed temp files should use less disk than uncompressed ones`() {
        val rows = 50_000

        fun measureTempBytes(compress: Boolean): Long {
            val tempDir = Files.createTempDirectory("retable-perf-temp").toFile()
            TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy(tempDir))
            try {
                var tempBytes = 0L
                val columns = Cols()
                val options = ExcelReadOptions(
                    useStreamingWorkbook = true,
                    streamingRowAccessWindowSize = 100,
                    useExperimentalFasterAutoSizeColumn = true,
                    compressStreamingTempFiles = compress
                )
                val ms = measure("temp files compress=$compress, $rows rows, window 100") {
                    val sampled = records(columns, rows, withUrl = false).onEach {
                        if (it.recordNumber == rows.toLong()) {
                            tempBytes = tempDir.listFiles()!!.sumOf { f -> f.length() }
                        }
                    }
                    Retable(columns, sampled).write(Retable.excel(columns, options) to CountingOutputStream())
                }
                println("temp files compress=$compress: $tempBytes bytes on disk, written in $ms ms")
                return tempBytes
            } finally {
                TempFile.setTempFileCreationStrategy(DefaultTempFileCreationStrategy())
                tempDir.deleteRecursively()
            }
        }

        val uncompressedBytes = measureTempBytes(compress = false)
        val compressedBytes = measureTempBytes(compress = true)
        println("temp files on disk, uncompressed=$uncompressedBytes bytes, compressed=$compressedBytes bytes")
        expectThat(compressedBytes).isGreaterThan(0L)
        expectThat(compressedBytes).isLessThan(uncompressedBytes / 2)
    }
}
