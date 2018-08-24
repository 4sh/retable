package io.retable

import io.retable.validation.*

typealias HeaderValueConstraint = ValidationRule<String?, String?, String, Unit>
typealias HeaderValueCheck = RuleCheck<String?, String?, String, Unit>
typealias HeaderConstraint = ValidationRule<Headers, String?, RetableColumn<*>, HeaderValueCheck>
typealias HeaderRuleCheck = RuleCheck<Headers, String?, RetableColumn<*>, HeaderValueCheck>

class RetableViolations(
    // list of all header checks performed
    val header:List<HeaderRuleCheck>,
    // collect all records having at least one invalid check
    val records:MutableList<RetableRecord> = mutableListOf()) {

    fun hasHeaderErrors(): Boolean {
        return header
                .filter { !it.isValid() }
                .firstOrNull() != null
    }
}


object HeaderConstraints {
    val missingHeaderRule = Validations.selfRule<String?, String>(
            id = "validations.header.missing",
            expectation = "not null", predicate = { v,e -> v != null })
    fun constraint(column: RetableColumn<*>, rule: HeaderValueConstraint) =
            Validations.rule<Headers, String?, RetableColumn<*>, HeaderValueCheck>(
                    id = "validations.retable.header.${rule.name}",
                    property = ValidationProperty("[${column.index}] header", { it.get(column) }),
                    expectation = column,
                    predicate = { header, col ->
                        val check = if (header == null) {
                            missingHeaderRule.validate(header)
                        } else {
                            rule.validate(header)
                        }
                        return@rule check.isValid() to check
                    },
                    okMessage = "column {property} {result}",
                    nokMessage = "column {property} {result}"
            )

    val eq: (RetableColumn<*>) -> HeaderConstraint
            = { col -> constraint(col, Validations.Strings.equals(col.name)) }
    val eqIgnoreCase: (RetableColumn<*>) -> HeaderConstraint
            = { col -> constraint(col, Validations.Strings.equalsIgnoreCase(col.name)) }
}

