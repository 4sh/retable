package io.retable.validation

/**
 * A validation rule is responsible for validating property values on subjects against an expectation.
 *
 * It has:
 * - an id (used to identify the rule
 * - a name (used in messages)
 * - a severity (it can be used to distinguish the behaviour to have when a rule is violated)
 * - a subject type
 * - a subject property, with
 *    - a name (used in messages)
 *    - a value accessor, allowing to obtain the value of the property on a subject
 * - an expectation: the value used to check the rule
 * - a predicate: comparing the expectation and the property value on the subject under validation
 * - 2 rule check message templates (one for valid, one for invalid), each composed of:
 *     - an i18n key (that is used to build an internationalized message with i18n resolver)
 *     - a default message template (used to easily build a message with i18n)
 *     - a function to build a message context (that can be used in default and i18n templates)
 * - an i18n resolver (for the messages)
 *
 * When requested to validate a subject, it uses the property accessor to obtain the property value, and then uses
 * the predicate and evaluates it against the expectation and the property value.
 *
 * It provides the result as a RuleCheck, with as context:
 * - the rule
 * - the subject
 * - the property value
 * - a boolean to indicate if the check is valid or not
 * - the severity of the violation (if invalid)
 * - a message, in two versions:
 *    - default
 *    - i18n
 *
 * Validation rules can be composed with the expectation:
 * for instance if you want to check that the length of the name of a user is in the range [3,8],
 * you can use the following chain:
 *
 * property rule (subject = user, property = name, expectation = string length rule, predicate = expectation passes)
 * string length rule (subject = string, property = length, expectation = range rule, predicate = expectation passes)
 * range rule (subject = number, property = self, expectation = [3,8], predicate = value is in range)
 *
 * Validation rules can also be grouped using AND and OR:
 * for instance
 * and rule (subject = user, property = self, expectation = [property rule 1, property rule 2],
 *           predicate = all expectation passes)
 * or rule (subject = user, property = self, expectation = [property rule 1, property rule 2],
 *           predicate = any expectation passes)
 */

enum class ValidationSeverity {
    OK, WARN, ERROR, FATAL
}

data class ValidationProperty<S,V>(
        val name:String,
        val accessor: (S) -> V
)

data class ValidationRule<S, V, E, R>(
        val id:String,
        val name:String,
        val severity: ValidationSeverity = ValidationSeverity.ERROR,
        val property: ValidationProperty<S,V>,
        val expectation: E,
        val predicate: (V, E) -> Pair<Boolean, R>,
        val validMessage: RuleCheckMessageTemplate<S, V, E, R>,
        val invalidMessage: RuleCheckMessageTemplate<S, V, E, R>,
        val i18nResolver: (String) -> String
) {
    fun validate(subject:S):RuleCheck<S, V, E, R> {
        val value = property.accessor(subject)
        val result = predicate(value, expectation)
        return if (result.first) {
            RuleCheck(this, subject, value, result.second, ValidationSeverity.OK, validMessage)
        } else {
            RuleCheck(this, subject, value, result.second, severity, invalidMessage)
        }
    }
}

data class RuleCheck<S, V, E, R>(val rule:ValidationRule<S, V, E, R>,
                              val subject:S,
                              val value:V,
                              val result:R,
                              val severity:ValidationSeverity,
                              val messageTemplate:RuleCheckMessageTemplate<S, V, E, R>
) {
    fun message() = messageTemplate.buildDefaultMessage(this)
    fun i18nMessage() = messageTemplate.buildI18nMessage(rule.i18nResolver, this)
    fun isValid() = when (severity) { ValidationSeverity.OK, ValidationSeverity.WARN -> true else -> false }
}


data class RuleCheckMessageTemplate<S, V, E, R>(
        val i18nKey:String,
        val defaultMessage:String,
        val context:(RuleCheck<S, V, E, R>) -> Map<String,*>) {
    fun buildDefaultMessage(ruleCheck: RuleCheck<S, V, E, R>): String = buildMessage(defaultMessage, ruleCheck)

    fun buildI18nMessage(i18nResolver: (String) -> String, ruleCheck: RuleCheck<S, V, E, R>): String =
            buildMessage(   i18nResolver(
                                if (i18nKey.startsWith(".")) { ruleCheck.rule.id + i18nKey } else { i18nKey }),
                            ruleCheck)

    private fun buildMessage(template:String, ruleCheck: RuleCheck<S, V, E, R>): String =
            interpolate(template, context(ruleCheck))

    private fun interpolate(template: String, context: Map<String, *>): String {
        // very naive templating mechanism
        var msg = template
        context.forEach {
            msg = msg.replace("{${it.key}}", it.value?.toString()?:"")
        }
        return msg
    }
}


/**
 * Validations provides a bunch of useful ValidationRule
 */
object Validations {
    object Numbers {
        fun <S : Number?> equals(expected:S) = selfRule<S,S>(
                id = "validations.numbers.equals", expectation = expected, predicate = { v, e -> v == e })
        fun <S : Number?> greaterThan(expected:S) = selfRule<S,S>(
                id = "validations.numbers.greaterThan", expectation = expected,
                predicate = { v, e -> v != null && v.toDouble() < e?.toDouble()?:0.0 })
        fun <S : Number?> lowerThan(expected:S) = selfRule<S,S>(
                id = "validations.numbers.lowerThan", expectation = expected,
                predicate = { v, e -> v != null && v.toDouble() < e?.toDouble()?:0.0 })
        fun inRange(expected:IntRange) = selfRule<Int?,IntRange>(
                id = "validations.numbers.inRange", expectation = expected,
                predicate = { v, e -> v != null && e.contains(v) })
        fun inRange(expected:LongRange) = selfRule<Long?,LongRange>(
                id = "validations.numbers.inRange", expectation = expected,
                predicate = { v, e -> v != null && e.contains(v) })
    }

    object Strings {
        fun <E> length(expected:ValidationRule<Int?, Int?, E, Unit>) = rule(
                id = "validations.string.length",
                expectation = expected,
                property = ValidationProperty<String?,Int>("length", { it?.length?:0 }),
                okMessage = "{subject} {property} {result}",
                nokMessage = "{subject} {property} {result}",
                predicate = { v, e ->
                    val result = e.validate(v)
                    return@rule result.isValid() to result
                }
        )
        fun equals(expected:String) = selfRule<String?, String>(
                id = "validations.string.equals", expectation = expected, predicate = { v, e -> v == e }
        )
        fun equalsIgnoreCase(expected:String) = selfRule<String?, String>(
                id = "validations.string.equalsIgnoreCase", expectation = expected,
                predicate = { v, e -> e.equals(v, true) }
        )
    }



    // helpers
    fun <S, V, E, R> rule(id:String,
                       name:String = id.substringAfterLast('.'),
                       property: ValidationProperty<S,V>,
                       severity: ValidationSeverity = ValidationSeverity.ERROR,
                       expectation: E,
                       predicate: (V, E) -> Pair<Boolean, R>,
                       okMessage:String = "{subject} {property} {value} is ${name} {expectation}",
                       nokMessage:String = "{subject} {property} {value} should be ${name} {expectation}",
                       validMessage: RuleCheckMessageTemplate<S, V, E, R> = Validations.okMessage(okMessage),
                       invalidMessage: RuleCheckMessageTemplate<S, V, E, R> = Validations.nokMessage(nokMessage),
                       i18nResolver: (String) -> String = Validations.i18nResolver
    ):ValidationRule<S, V, E, R> = ValidationRule(
            id = id, name = name,
            property = property, severity = severity,
            expectation     = expectation,
            predicate       = predicate,
            i18nResolver    = i18nResolver,
            validMessage    = validMessage,
            invalidMessage  = invalidMessage
    )
    fun <S, E> selfRule(id:String,
                     name:String = id.substringAfterLast('.'),
                     severity: ValidationSeverity = ValidationSeverity.ERROR,
                     expectation: E,
                     predicate: (S, E) -> Boolean,
                     okMessage:String = "{value} is ${name} {expectation}",
                     nokMessage:String = "{value} should be ${name} {expectation}",
                     validMessage: RuleCheckMessageTemplate<S, S, E, Unit> = Validations.okMessage(okMessage),
                     invalidMessage: RuleCheckMessageTemplate<S, S, E, Unit> = Validations.nokMessage(nokMessage),
                     i18nResolver: (String) -> String = Validations.i18nResolver
    ) = ValidationRule(
            id = id, name = name,
            property = self(), severity = severity,
            expectation     = expectation,
            predicate       = { v,e -> return@ValidationRule predicate(v,e) to Unit },
            i18nResolver    = i18nResolver,
            validMessage    = validMessage,
            invalidMessage  = invalidMessage
    )

    fun <S, V, E, R> okMessage(msg:String) = RuleCheckMessageTemplate(".ok", msg, context<S, V, E, R>())
    fun <S, V, E, R> nokMessage(msg:String) = RuleCheckMessageTemplate(".nok", msg, context<S, V, E, R>())

    fun <S> self(): ValidationProperty<S, S> = ValidationProperty("self", { it })

    fun <S, V, E, R> context():(RuleCheck<S, V, E, R>) -> Map<String,*> =
            {
                mapOf(
                    "subject" to display(it.subject),
                    "property" to it.rule.property.name,
                    "value" to display(it.value),
                    "result" to (if (it.result is RuleCheck<*,*,*,*>) { it.result.message() } else { display(it) }),
                    "expectation" to display(it.rule.expectation)
                )
            }
    private fun <T> display(o:T):String {
        return when (o) {
            is String -> "\"$o\""
            else -> o?.toString()?:"NULL"
        }
    }

    var i18nResolver:(String) -> String = { throw UnsupportedOperationException("i18n not implemented by default") }
}
