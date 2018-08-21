package io.retable

import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


class Retable<T : RetableColumns>(
        val columns:T,
        val records:Sequence<RetableRecord>,
        val validations: RetableValidations
) {
    companion object {
        fun csv(options:CSVReadOptions = CSVReadOptions()) = csv(RetableColumns.auto, options)
        fun <T : RetableColumns> csv(columns:T, options:CSVReadOptions
                = CSVReadOptions()) = RetableCSVSupport(columns, options)

        fun excel() = excel(RetableColumns.auto)
        fun <T : RetableColumns> excel(columns:T) = RetableExcelSupport(columns)
    }
}


abstract class ReadOptions(
        val trimValues:Boolean,
        val ignoreEmptyLines:Boolean,
        val firstRecordAsHeader:Boolean
)





abstract class RetableColumns {
    companion object {
        fun ofNames(names:List<String>,
                    headerRule: (String) -> ValidationRule<String, *> = RetableValidations.header.eq)
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
                                headerRule: (String) -> ValidationRule<String,*>,
                                val dataValidation: ValidationRule<T,*>) {
    val headerValidation:ValidationRule<String,*>

    init {
        headerValidation = headerRule.invoke(name)
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
                          headerRule: (String) -> ValidationRule<String,*> = RetableValidations.header.eq,
                          dataValidation: ValidationRule<String,*> = ValidationRule.ok("NO VALIDATION"))
    : RetableColumn<String>(index, name, headerRule, dataValidation) {
    override fun getFromRaw(raw: String): String = raw
}
class IntRetableColumn(index:Int, name:String,
                       headerRule: (String) -> ValidationRule<String,*> = RetableValidations.header.eq,
                       dataValidation: ValidationRule<Int,*> = ValidationRule.ok("NO VALIDATION"))
    : RetableColumn<Int>(index, name, headerRule, dataValidation) {
    override fun getFromRaw(raw: String): Int = raw.toInt()
}




data class RetableRecord(val columns: RetableColumns,
                         val recordNumber: Long,
                         val lineNumber: Long,
                         val rawData: List<String>,
                         val validations:List<ValidationResult<RetableRecord,*>> = listOf()
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



