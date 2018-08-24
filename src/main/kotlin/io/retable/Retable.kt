package io.retable

import java.io.InputStream
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


class Retable<T : RetableColumns>(
        val columns:T,
        val records:Sequence<RetableRecord>,
        val violations: RetableViolations
) {
    companion object {
        fun csv(options:CSVReadOptions = CSVReadOptions()) = csv(RetableColumns.auto, options)
        fun <T : RetableColumns> csv(columns:T, options:CSVReadOptions
                = CSVReadOptions()) = RetableCSVSupport(columns, options)

        fun excel() = excel(RetableColumns.auto)
        fun <T : RetableColumns> excel(columns:T) = RetableExcelSupport(columns)
    }
}

abstract class BaseSupport<T : RetableColumns, O : ReadOptions>(val columns: RetableColumns, val options:O) {
    abstract fun iterator(input: InputStream): Iterator<List<String>>
    /**
     * Parses the input
     *
     * Note that input is consumed when sequence is consumed, if the end is not reached the reader
     * should be closed.
     */
    fun read(input: InputStream): Retable<T> {
        val rawData = object : Iterator<List<String>> {
            val raw = iterator(input)
            var next:List<String>? = null
            var lineNumber:Long = 0

            override fun hasNext(): Boolean {
                lineNumber++
                next = if (raw.hasNext()) { raw.next() } else { null }
                while (next != null && ignoreLine(next)) {
                    lineNumber++
                    next = if (raw.hasNext()) { raw.next() } else { null }
                }
                return next != null
            }

            private fun ignoreLine(list: List<String>?): Boolean =
                    options.ignoreEmptyLines && isEmptyLine(list)

            private fun isEmptyLine(list: List<String>?): Boolean =
                    list == null || list.isEmpty() || list.filter { !it.trim().isEmpty() }.isEmpty()

            override fun next(): List<String> {
                if (next == null) hasNext() // make sure hasNext has been called so that next is fetched
                return (next ?: throw IllegalStateException("no more records"))
                            .map { if (options.trimValues) it.trim() else it }
            }
        }

        if (!options.firstRecordAsHeader && columns == RetableColumns.auto) {
            throw IllegalStateException("columns are mandatory when not using first record as header")
        }

        val violations:RetableViolations
        val columns:RetableColumns
        if (options.firstRecordAsHeader) {
            val header = if (rawData.hasNext()) { rawData.next() } else { null }
            if (header == null) {
                throw IllegalStateException("empty file not allowed when first record is expected to be the header")
            }
            val headers = Headers(header)

            if (this.columns == RetableColumns.auto) {
                columns = RetableColumns.ofNames(headers.headers)
                violations = RetableViolations(listOf())
            } else {
                columns = this.columns
                violations = RetableViolations(
                        columns.list().map { col -> col.headerConstraint.validate(headers) }
                )
            }
        } else {
            columns = this.columns
            violations = RetableViolations(listOf())
        }

        return Retable(
                columns as T,
                rawData.asSequence()
                        .mapIndexed {index, raw ->
                            RetableRecord(columns, index + 1L, rawData.lineNumber, raw)},
                violations)
    }

}

abstract class ReadOptions(
        val trimValues:Boolean,
        val ignoreEmptyLines:Boolean,
        val firstRecordAsHeader:Boolean
)



class Headers(val headers:List<String>) {
    operator fun get(col:RetableColumn<*>):String? =
            try { headers[col.index - 1] } catch (e:ArrayIndexOutOfBoundsException) { null }
}


abstract class RetableColumns {
    companion object {
        fun ofNames(names:List<String>,
                    headerRule: (RetableColumn<*>) -> HeaderConstraint = HeaderConstraints.eq)
                = ofCols(names.mapIndexed { index, s -> StringRetableColumn(index + 1, s, headerRule) }.toList())
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

    fun string(name:String,
               headerConstraint: (RetableColumn<*>) ->  HeaderConstraint = HeaderConstraints.eq) =
            StringRetableColumn(c++, name, headerConstraint)
    fun int(name:String,
            headerConstraint: (RetableColumn<*>) ->  HeaderConstraint = HeaderConstraints.eq) =
            IntRetableColumn(c++, name, headerConstraint)

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

abstract class RetableColumn<T>(val index:Int, val name:String,
                                headerRule: (RetableColumn<*>) -> HeaderConstraint) {
    val headerConstraint:HeaderConstraint

    init {
        headerConstraint = headerRule.invoke(this)
    }

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

class StringRetableColumn(index:Int, name:String,
                          headerConstraint: (RetableColumn<*>) ->  HeaderConstraint)
    : RetableColumn<String>(index, name, headerConstraint) {
    override fun getFromRaw(raw: String): String = raw
}
class IntRetableColumn(index:Int, name:String,
                       headerConstraint: (RetableColumn<*>) ->  HeaderConstraint)
    : RetableColumn<Int>(index, name, headerConstraint) {
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



