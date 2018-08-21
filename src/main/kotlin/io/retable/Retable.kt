package io.retable

import org.apache.commons.csv.CSVFormat
import org.apache.poi.ss.usermodel.Row
import java.io.InputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStreamReader
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


class Retable<T : RetableColumns>(val columns:T,
                val records:Sequence<RetableRecord>) {
    companion object {
        fun csv() = csv(RetableColumns.auto)
        fun <T : RetableColumns> csv(columns:T) = RetableCSVSupport(columns)
        fun excel() = excel(RetableColumns.auto)
        fun <T : RetableColumns> excel(columns:T) = RetableExcelSupport(columns)
    }
}

class RetableExcelSupport<T : RetableColumns>(val columns: T) {
    fun read(input:InputStream):Retable<T> {
        val workbook = WorkbookFactory.create(input)

        val sheet = workbook.getSheetAt(0)

        val rowIterator = sheet.rowIterator()

        var lineNumber:Long = 0
        val header = rowIterator.next()

        val columns = if (this.columns is ListRetableColumns && this.columns.list().size == 0) {
            RetableColumns.ofNames(
                    header.cellIterator().asSequence()
                            .map { it.stringCellValue }
                            .toList())
        } else {
            this.columns
        }

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

        return Retable(columns as T, records)
    }
}

class RetableCSVSupport<T : RetableColumns>(val columns: T) {
    private val format:CSVFormat = CSVFormat.DEFAULT.withFirstRecordAsHeader()

    /**
     * Parses the input from the reader as a CSV file.
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun read(input:InputStream):Retable<T> {
        val parse = format.parse(InputStreamReader(input, Charsets.UTF_8))
        val iterator = parse.iterator()

        val headers = parse.headerMap
        val columns = if (this.columns is ListRetableColumns && this.columns.list().size == 0) {
            RetableColumns.ofNames(
                    (0..headers.size - 1)
                            .map { index -> headers.entries.find { it.value == index } }
                            .map { it?.key?:"" }
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

abstract class RetableColumns {
    companion object {
        fun ofNames(names:List<String>) = ofCols(names.mapIndexed { index, s -> StringRetableColumn(index + 1, s) }.toList())
        fun ofCols(cols:List<RetableColumn<*>>) = ListRetableColumns(cols)
        val auto = ListRetableColumns(listOf())
    }

    protected var c = 1

    open fun list():List<RetableColumn<*>> = this::class.memberProperties
            .filter { it.returnType.jvmErasure.isSubclassOf(RetableColumn::class) }
            .map { it.call(this) }
            .filterIsInstance(RetableColumn::class.java)
            .toList()

    operator fun get(index:Int) = list().find { it.index == index } ?: throw ArrayIndexOutOfBoundsException(index)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RetableColumns) return false
        return other.list() == list()
    }

    override fun hashCode(): Int {
        return list().hashCode()
    }
}

class ListRetableColumns(private val cols:List<RetableColumn<*>>):RetableColumns() {
    override fun list(): List<RetableColumn<*>> = cols
}

abstract class RetableColumn<T>(val index:Int, val name:String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RetableColumn<*>) return false

        if (index != other.index) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "RetableColumn(index=$index, name='$name')"
    }

    abstract fun getFromRaw(raw:String):T

}

class StringRetableColumn(index:Int, name:String) : RetableColumn<String>(index, name) {
    override fun getFromRaw(raw: String): String = raw
}
class IntRetableColumn(index:Int, name:String) : RetableColumn<Int>(index, name) {
    override fun getFromRaw(raw: String): Int = raw.toInt()
}

data class RetableRecord(val columns: RetableColumns,
                         val recordNumber: Long,
                         val lineNumber: Long,
                         val rawData: List<String>
                         ) {
    operator fun get(c:String):String? {
        return columns.list()
                .find { it.name == c }
                ?.let { rawData.get(it.index - 1) }
    }

    operator fun <T> get(c:RetableColumn<T>):T? {
        return c.getFromRaw(rawData.get(c.index - 1))
    }
}
