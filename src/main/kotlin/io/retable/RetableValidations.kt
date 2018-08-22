package io.retable


enum class ValidationLevel { OK, WARN, ERROR, FATAL }

abstract class ValidationRule<S,C>(val id:String) {
    companion object {
        fun <S,C> ok(context:C) = always<S,C>(ValidationLevel.OK, context)
        fun <S,C> always(level:ValidationLevel, context:C) = object:ValidationRule<S,C>("always.${level.name}") {
            override fun validate(subject: S?): ValidationResult<S, C> =
                    ValidationResult(level, subject, context, this)
        }
    }
    abstract fun validate(subject:S?):ValidationResult<S,C>



    // helper functions for implementations
    protected fun ok(subject:S?, context:C) = result(ValidationLevel.OK, subject, context)
    protected fun warn(subject:S?, context:C) = result(ValidationLevel.OK, subject, context)
    protected fun error(subject:S?, context:C) = result(ValidationLevel.OK, subject, context)
    protected fun fatal(subject:S?, context:C) = result(ValidationLevel.OK, subject, context)

    protected fun result(level: ValidationLevel, subject: S?, context: C) =
            ValidationResult(level, subject, context, this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValidationRule<*, *>) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class ValidationResult<S,C>(
        val level:ValidationLevel,
        val subject:S?,
        val context:C,
        val rule:ValidationRule<S,C>)

class RetableValidations(
        val header:List<ValidationResult<*,*>>,
        // collect all records having at least one validation result which is not OK
        val records:MutableList<RetableRecord> = mutableListOf()) {


    fun hasHeaderErrors(): Boolean {
        return header
                .filter { it.level.ordinal >= ValidationLevel.ERROR.ordinal }
                .firstOrNull() != null
    }

    companion object {
        val header = HeaderValidations
        val data = DataValidations
    }
}


typealias HeaderValidationResult = ValidationResult<Headers, HeaderValidations.Context>
object HeaderValidations {
    data class Context(val col:RetableColumn<*>, val value:String?)
    abstract class Rule(id:String) : ValidationRule<Headers, Context>(id)

    fun rule(id:String, col:RetableColumn<*>, predicate:(String)->Boolean, level:ValidationLevel = ValidationLevel.ERROR) =
            object : Rule("header." + id) {
                private val missingHeader = missingHeader(col)
                override fun validate(subject: Headers?): ValidationResult<Headers, Context> {
                    val missing = missingHeader.validate(subject)
                    if (missing.level != ValidationLevel.OK) {
                        return missing
                    }
                    val value = subject?.get(col)
                    return if (predicate.invoke(value!!)) {
                        ok(subject, Context(col, value))
                    } else {
                        result(level, subject, Context(col, value))
                    }
                }
            }

    fun missingHeader(col:RetableColumn<*>) = object : Rule("header.missing") {
        override fun validate(subject: Headers?): ValidationResult<Headers, Context> {
            val value = subject?.get(col)
            if (value == null) {
                return fatal(subject, Context(col, value))
            } else {
                return ok(subject, Context(col, value))
            }
        }
    }

    val eq: (RetableColumn<*>) -> Rule
            = { col -> rule("equal", col, { it.equals(col.name) }) }
    val eqIgnoreCase: (RetableColumn<*>) -> Rule
            = { col -> rule("equalIgnoreCase", col, { it.equals(col.name, true) }) }
}

typealias DataValidationResult<T> = ValidationResult<RetableRecord, DataValidations.Context<T>>
object DataValidations {
    data class Context<T>(val col:RetableColumn<T>, val value:T?)
    abstract class Rule<T>(id:String) : ValidationRule<RetableRecord, Context<T>>(id)

    fun <T> none():(RetableColumn<T>) -> Rule<T> = { col -> rule<T>("none", col, { true } )}

    fun <T>rule(id:String, col:RetableColumn<T>, predicate:(T?)->Boolean, level:ValidationLevel = ValidationLevel.ERROR) =
            object : Rule<T>("data." + id) {
                override fun validate(subject: RetableRecord?): ValidationResult<RetableRecord, Context<T>> {
                    val value = subject?.get(col)
                    return if (predicate.invoke(value)) {
                        ok(subject, Context(col, value))
                    } else {
                        result(level, subject, Context(col, value))
                    }
                }
            }
}
