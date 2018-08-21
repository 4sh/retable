package io.retable


enum class ValidationLevel { OK, WARN, ERROR, FATAL }

interface ValidationRule<S,D> {
    companion object {
        fun <S,D> ok(details:D) = object:ValidationRule<S,D> {
            override fun validate(subject: S?): ValidationResult<S, D> =
                    ValidationResult(ValidationLevel.OK, subject, this, details)
        }
    }
    fun validate(subject:S?):ValidationResult<S,D>
}

data class ValidationResult<S,D>(val level:ValidationLevel,
                            val subject:S?,
                            val rule:ValidationRule<S,D>,
                            val details:D)

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
    }
}

object HeaderValidations {
    val eq: (String) -> ValidationRule<String, *>
            = { StringEqualValidationRule(it, ValidationLevel.ERROR) }
    val eqIgnoreCase: (String) -> ValidationRule<String, *>
            = { StringEqualIgnoreCaseValidationRule(it, ValidationLevel.ERROR) }
}

// rules

// primitive rules
data class StringEqualValidationRule(val expect:String, val level:ValidationLevel):ValidationRule<String,String> {
    override fun validate(subject: String?): ValidationResult<String, String> {
        return if (expect == subject)
            ValidationResult(ValidationLevel.OK, subject, this, expect)
        else ValidationResult(level, subject, this, expect)
    }
}

data class StringEqualIgnoreCaseValidationRule(val expect:String, val level:ValidationLevel):ValidationRule<String,String> {
    override fun validate(subject: String?): ValidationResult<String, String> {
        return if (expect.equals(subject, true))
            ValidationResult(ValidationLevel.OK, subject, this, expect)
        else ValidationResult(level, subject, this, expect)
    }
}

// data validation rule is used to validate a particular column
data class DataValidationResult<S,D>(val col:RetableColumn<S>, val result:ValidationResult<S,D>)

class DataValidationRule<S,D>(val expect: ValidationRule<S,D>, val col:RetableColumn<S>)
    :ValidationRule<RetableRecord,DataValidationResult<S,D>> {
    override fun validate(subject: RetableRecord?): ValidationResult<RetableRecord, DataValidationResult<S, D>> {
        return expect
                .validate(subject?.let {it[col]})
                .let { ValidationResult(it.level, subject, this, DataValidationResult(col, it)) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataValidationRule<*, *>) return false

        if (expect != other.expect) return false
        if (col != other.col) return false

        return true
    }

    override fun hashCode(): Int {
        var result = expect.hashCode()
        result = 31 * result + col.hashCode()
        return result
    }

    override fun toString(): String {
        return "DataValidationRule(expect=$expect, col=$col)"
    }
}

//

class MissingHeaderRule(val col:RetableColumn<*>):ValidationRule<String,RetableColumn<*>> {
    override fun validate(subject: String?): ValidationResult<String, RetableColumn<*>> =
        ValidationResult(ValidationLevel.FATAL, subject, this, col)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MissingHeaderRule) return false

        if (col != other.col) return false

        return true
    }

    override fun hashCode(): Int {
        return col.hashCode()
    }

    override fun toString(): String {
        return "MissingHeaderRule(col=$col)"
    }
}