package io.valkee

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

data class ValidationProperty<in S, out V>(
        val name:String,
        val accessor: (S) -> V
)

data class ValidationRule<S, V, E, R>(
        val id:String,
        val name:String,
        val severity: ValidationSeverity = ValidationSeverity.ERROR,
        val property: ValidationProperty<S, V>,
        val expectation: E,
        val predicate: (V, E) -> Pair<Boolean, R>,
        val validMessage: RuleCheckMessageTemplate<S, V, E, R>,
        val invalidMessage: RuleCheckMessageTemplate<S, V, E, R>,
        val i18nResolver: (String) -> String
) {
    fun validate(subject:S): RuleCheck<S, V, E, R> {
        val value = property.accessor(subject)
        val result = predicate(value, expectation)
        return if (result.first) {
            RuleCheck(this, subject, value, result.second, ValidationSeverity.OK, validMessage)
        } else {
            RuleCheck(this, subject, value, result.second, severity, invalidMessage)
        }
    }
}

data class RuleCheck<S, V, E, R>(val rule: ValidationRule<S, V, E, R>,
                                 val subject:S,
                                 val value:V,
                                 val result:R,
                                 val severity: ValidationSeverity,
                                 val messageTemplate: RuleCheckMessageTemplate<S, V, E, R>
) {
    fun message() = messageTemplate.buildDefaultMessage(this)
    fun i18nMessage() = messageTemplate.buildI18nMessage(rule.i18nResolver, this)
    fun isValid() = when (severity) { ValidationSeverity.OK, ValidationSeverity.WARN -> true else -> false }

    // used to return this check as result for a predicate
    fun toPair(): Pair<Boolean, RuleCheck<S, V, E, R>> = isValid() to this
}


data class RuleCheckMessageTemplate<S, V, E, R>(
        val i18nKey:String,
        val defaultMessage:String,
        val context:(Context<S, V, E, R>) -> Map<String,*>) {
    fun buildDefaultMessage(ruleCheck: RuleCheck<S, V, E, R>): String = buildMessage(defaultMessage, ruleCheck)

    fun buildI18nMessage(i18nResolver: (String) -> String, ruleCheck: RuleCheck<S, V, E, R>): String =
            buildMessage(   i18nResolver(
                                if (i18nKey.startsWith(".")) { ruleCheck.rule.id + i18nKey } else { i18nKey }),
                            ruleCheck)

    private fun buildMessage(template:String, ruleCheck: RuleCheck<S, V, E, R>): String =
            template.interpolate(context(Context(ruleCheck)))

    data class Context<S, V, E, R>(
            val check: RuleCheck<S, V, E, R>,
            val rule: ValidationRule<S, V, E, R> = check.rule,
            val subject: S = check.subject,
            val value:V = check.value,
            val expectation:E = check.rule.expectation,
            val result:R = check.result,
            val valid:Boolean = check.isValid(),
            val severity: ValidationSeverity = check.severity
    )
}


fun String.interpolate(context: Map<String, *>): String {
    // very naive templating mechanism
    var msg = this
    context.forEach {
        msg = msg.replace("{${it.key}}", it.value?.toString()?:"")
    }
    return msg.trim()
}

/**
 * Validations provides a bunch of useful ValidationRule
 */
object Validations {
    object Numbers {
        fun <S : Number?> equals(expected:S) = selfRule<S, S>(
                id = "validations.numbers.equals", expectation = expected, predicate = { v, e -> v == e },
                message = MsgTpl("equal to"))
        fun <S : Number?> greaterThan(expected:S) = selfRule<S, S>(
                id = "validations.numbers.greaterThan", expectation = expected,
                predicate = { v, e -> v != null && v.toDouble() < e?.toDouble() ?: 0.0 },
                message = MsgTpl("greater than"))
        fun <S : Number?> lowerThan(expected:S) = selfRule<S, S>(
                id = "validations.numbers.lowerThan", expectation = expected,
                predicate = { v, e -> v != null && v.toDouble() < e?.toDouble() ?: 0.0 },
                message = MsgTpl("lower than"))
        fun inRange(expected:IntRange) = selfRule<Int?, IntRange>(
                id = "validations.numbers.inRange", expectation = expected,
                predicate = { v, e -> v != null && e.contains(v) },
                message = MsgTpl(rule = "between",
                        context = context({
                            mapOf(
                                    "expectation" to "${expectation.first} and ${expectation.last}")
                        })))
        fun inRange(expected:LongRange) = selfRule<Long?, LongRange>(
                id = "validations.numbers.inRange", expectation = expected,
                predicate = { v, e -> v != null && e.contains(v) },
                message = MsgTpl(rule = "between",
                        context = context({
                            mapOf(
                                    "expectation" to "${expectation.first} and ${expectation.last}")
                        })))
    }

    object Strings {
        fun <E> length(expected: ValidationRule<Int?, Int?, E, Unit>) = rule(
                id = "validations.string.length",
                expectation = expected,
                property = ValidationProperty<String?, Int>("length", { it?.length ?: 0 }),
                message = MsgTpl(message = "{subject} {property} {result}"),
                predicate = { v, e ->
                    val result = e.validate(v)
                    return@rule result.isValid() to result
                }
        )
        fun equals(expected:String) = selfRule<String?, String>(
                id = "validations.string.equals", expectation = expected, predicate = { v, e -> v == e },
                message = MsgTpl("equal to")
        )
        fun equalsIgnoreCase(expected:String) = selfRule<String?, String>(
                id = "validations.string.equalsIgnoreCase", expectation = expected,
                predicate = { v, e -> e.equals(v, true) },
                message = MsgTpl("equal ignoring case to")
        )
        fun isInteger() = selfRule<String?, Unit>(
                id = "validations.string.isInteger", expectation = Unit, predicate =
        { v, e -> v?.asSequence()?.filter { !it.isDigit() }?.firstOrNull() == null },
                message = MsgTpl("an integer")
        )
        fun matches(regex:Regex, message:String = "match {expectation}") = selfRule<String?, Regex>(
                id = "validations.string.equals", expectation = regex,
                predicate = { v, e -> v?.let { e.matches(it) } ?: false },
                message = MsgTpl(message = "{subject} {property} {value} {verb} ${message}",
                        okVerb = "", nokVerb = "should")
        )
    }



    // helpers
    fun <S, V, E, R> rule(id:String,
                          name:String = id.substringAfterLast('.'),
                          property: ValidationProperty<S, V>,
                          severity: ValidationSeverity = ValidationSeverity.ERROR,
                          expectation: E,
                          predicate: (V, E) -> Pair<Boolean, R>,
                          message: MsgTpl<S, V, E, R> = MsgTpl(name),
                          validMessage: RuleCheckMessageTemplate<S, V, E, R> = message.okMessage(),
                          invalidMessage: RuleCheckMessageTemplate<S, V, E, R> = message.nokMessage(),
                          i18nResolver: (String) -> String = Validations.i18nResolver
    ): ValidationRule<S, V, E, R> = ValidationRule(
            id = id, name = name,
            property = property, severity = severity,
            expectation = expectation,
            predicate = predicate,
            i18nResolver = i18nResolver,
            validMessage = validMessage,
            invalidMessage = invalidMessage
    )
    fun <S, E> selfRule(id:String,
                        name:String = id.substringAfterLast('.'),
                        severity: ValidationSeverity = ValidationSeverity.ERROR,
                        expectation: E,
                        predicate: (S, E) -> Boolean,
                        message: MsgTpl<S, S, E, Unit> = MsgTpl(name),
                        validMessage: RuleCheckMessageTemplate<S, S, E, Unit> = message.okMessage(),
                        invalidMessage: RuleCheckMessageTemplate<S, S, E, Unit> = message.nokMessage(),
                        i18nResolver: (String) -> String = Validations.i18nResolver
    ) = ValidationRule(
            id = id, name = name,
            property = self(), severity = severity,
            expectation = expectation,
            predicate = { v, e -> return@ValidationRule predicate(v, e) to Unit },
            i18nResolver = i18nResolver,
            validMessage = validMessage,
            invalidMessage = invalidMessage
    )

    class MsgTpl<S, V, E, R>(
            rule:String = "",
            message:String = "{subject} {property} {value} {verb} {rule} {expectation}",
            okVerb:String = "is",
            nokVerb:String = "should be",
            private val context: (RuleCheckMessageTemplate.Context<S, V, E, R>) -> Map<String,*> = context(),
            private val okMessage:String = message.interpolate(mapOf("verb" to okVerb, "rule" to rule)),
            private val nokMessage:String = message.interpolate(mapOf("verb" to nokVerb, "rule" to rule))
            ) {
        fun okMessage() = RuleCheckMessageTemplate(".ok", okMessage, context)
        fun nokMessage() = RuleCheckMessageTemplate(".nok", nokMessage, context)
    }

    fun <S> self(): ValidationProperty<S, S> = ValidationProperty("self", { it })

    fun <S, V, E, R> context(
            custom: RuleCheckMessageTemplate.Context<S, V, E, R>.() -> Map<String,*> = { mapOf<String,Any>() }
    ):(RuleCheckMessageTemplate.Context<S, V, E, R>) -> Map<String,*> =
            {
                val self = it.rule.property.name == "self"
                mapOf(
                    "subject" to if (self) "" else display(it.subject),
                    "property" to if (self) "" else it.rule.property.name,
                    "value" to display(it.value),
                    "result" to (if (it.result is RuleCheck<*, *, *, *>) { it.result.message() } else {
                        display(it)
                    }),
                    "expectation" to display(it.expectation)
                ).plus(custom.invoke(it))
            }
    private fun <T> display(o:T):String {
        return when (o) {
            is String -> "\"$o\""
            is Unit -> ""
            else -> o?.toString()?:"NULL"
        }
    }

    var i18nResolver:(String) -> String = { throw UnsupportedOperationException("i18n not implemented by default") }
}
