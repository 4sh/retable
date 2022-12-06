package io.retable

import io.valkee.RuleCheck
import io.valkee.Validations
import io.valkee.Validations.MsgTpl
import io.valkee.Validations.rule
import io.valkee.ValkeeProperty
import io.valkee.ValkeeRule

typealias HeaderValueConstraint = ValkeeRule<String?, String?, String, Unit>
typealias HeaderValueCheck = RuleCheck<String?, String?, String, Unit>
typealias HeaderConstraint = ValkeeRule<Headers, String?, RetableColumn<*>, HeaderValueCheck>
typealias HeaderRuleCheck = RuleCheck<Headers, String?, RetableColumn<*>, HeaderValueCheck>

typealias DataValueConstraint<T, E> = ValkeeRule<T, *, E, *>
typealias DataValueCheck<T, E> = RuleCheck<T, *, E, *>
typealias DataConstraint = ValkeeRule<RetableRecord, String?, DataValueConstraint<*, *>, DataValueCheck<*, *>>
typealias DataConstraintCheck = RuleCheck<RetableRecord, String?, DataValueConstraint<*, *>, DataValueCheck<*, *>>

class RetableViolations(
    // list of all header checks performed
    val header: List<HeaderRuleCheck> = listOf(),
    // collect all records having at least one invalid check
    val records: MutableList<RetableRecord> = mutableListOf()
) {

    fun hasHeaderErrors(): Boolean {
        return header
            .filter { !it.isValid() }
            .firstOrNull() != null
    }
}

object HeaderConstraints {
    val missingHeaderRule = Validations.selfRule<String?, String>(
        id = "validations.header.missing",
        expectation = "not null",
        predicate = { v, _ -> v != null }
    )
    fun constraint(column: RetableColumn<*>, rule: HeaderValueConstraint) =
        rule<Headers, String?, RetableColumn<*>, HeaderValueCheck>(
            id = "validations.retable.header.${rule.name}",
            property = ValkeeProperty("[${column.index}] header", { it.get(column) }),
            expectation = column,
            predicate = { header, _ ->
                val check = if (header == null) {
                    missingHeaderRule.validate(header)
                } else {
                    rule.validate(header)
                }
                return@rule check.isValid() to check
            },
            message = MsgTpl(message = "column {property} {result}")
        )

    val eq: (RetableColumn<*>) -> HeaderConstraint =
        { col -> constraint(col, Validations.Strings.isEquals(col.name)) }
    val eqIgnoreCase: (RetableColumn<*>) -> HeaderConstraint =
        { col -> constraint(col, Validations.Strings.equalsIgnoreCase(col.name)) }
}

object DataConstraints {
    private fun <T> rawColProperty(col: RetableColumn<T>) =
        ValkeeProperty<RetableRecord, String?>(col.name, { it.rawGet(col) })

    @Suppress("UNCHECKED_CAST")
    fun rawColConstraint(col: RetableColumn<*>, valueConstraint: DataValueConstraint<String?, *>) =
        rule(
            id = "validations.data.${valueConstraint.name}",
            property = rawColProperty(col),
            expectation = valueConstraint,
            predicate = { v, e -> e.validate(v).toPair() },
            message = MsgTpl(message = "{property} {result}")
        ) as DataConstraint

    @Suppress("UNCHECKED_CAST")
    fun <T> colConstraint(col: RetableColumn<T>, valueConstraint: DataValueConstraint<T?, *>) =
        rule(
            id = "validations.data.${valueConstraint.name}",
            property = rawColProperty(col),
            expectation = valueConstraint,
            predicate = { v, e -> e.validate(v?.let { col.getFromRaw(it) }).toPair() },
            message = MsgTpl(message = "{property} {result}")
        ) as DataConstraint

    fun <T> none(): DataValueConstraint<T?, *> =
        Validations.selfRule(id = "validations.data.value.none", expectation = Unit, predicate = { _, _ -> true })
}
