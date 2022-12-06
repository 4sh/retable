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

enum class ValkeeSeverity {
    OK, WARN, ERROR, FATAL
}

data class ValkeeProperty<in S, out V>(
    val name: String,
    val accessor: (S) -> V
)

data class ValkeeRule<S, V, E, R>(
    val id: String,
    val name: String,
    val severity: ValkeeSeverity = ValkeeSeverity.ERROR,
    val property: ValkeeProperty<S, V>,
    val expectation: E,
    val predicate: (V, E) -> Pair<Boolean, R>,
    val messageBuilder: ValkeeRuleMessageBuilder<S, V, E, R>
) {
    fun validate(subject: S): RuleCheck<S, V, E, R> {
        val value = property.accessor(subject)
        val (valid, result) = predicate(value, expectation)

        return RuleCheck(
            this,
            subject,
            value,
            result,
            if (valid) { ValkeeSeverity.OK } else { severity },
            messageBuilder.buildMessage(valid)
        )
    }
}

typealias ValkeeComposedRule<S, V, VV, VE, VR> = ValkeeRule<S, V, ValkeeRule<V, VV, VE, VR>, RuleCheck<V, VV, VE, VR>>

data class ValkeeRuleMessageBuilder<S, V, E, R>(
    private val validMessage: I18nMessage,
    private val invalidMessage: I18nMessage,
    private val templateResolver: (String, RuleCheckMessageTemplate.Context<S, V, E, R>) -> String,
    private val i18nResolver: (String) -> String
) {
    fun buildMessage(valid: Boolean): RuleCheckMessageTemplate<S, V, E, R> =
        RuleCheckMessageTemplate(if (valid) { validMessage } else { invalidMessage }, templateResolver, i18nResolver)
}

data class I18nMessage(val i18nKey: String, val defaultMessage: String)

data class RuleCheck<S, V, E, R>(
    val rule: ValkeeRule<S, V, E, R>,
    val subject: S,
    val value: V,
    val result: R,
    val severity: ValkeeSeverity,
    val messageTemplate: RuleCheckMessageTemplate<S, V, E, R>
) {
    fun message() = messageTemplate.buildDefaultMessage(this)
    fun i18nMessage() = messageTemplate.buildI18nMessage(this)
    fun isValid() = when (severity) { ValkeeSeverity.OK, ValkeeSeverity.WARN -> true else -> false }

    // used to return this check as result for a predicate
    fun toPair(): Pair<Boolean, RuleCheck<S, V, E, R>> = isValid() to this
}

data class RuleCheckMessageTemplate<S, V, E, R>(
    val message: I18nMessage,
    private val templateResolver: (String, RuleCheckMessageTemplate.Context<S, V, E, R>) -> String,
    private val i18nResolver: (String) -> String
) {
    fun buildDefaultMessage(ruleCheck: RuleCheck<S, V, E, R>): String = buildMessage(message.defaultMessage, ruleCheck)

    fun buildI18nMessage(ruleCheck: RuleCheck<S, V, E, R>): String =
        buildMessage(
            i18nResolver(
                if (message.i18nKey.startsWith(".")) { ruleCheck.rule.id + message.i18nKey } else { message.i18nKey }
            ),
            ruleCheck
        )

    private fun buildMessage(template: String, ruleCheck: RuleCheck<S, V, E, R>): String =
        templateResolver(template, Context(ruleCheck))

    data class Context<S, V, E, R>(
        val check: RuleCheck<S, V, E, R>,
        val rule: ValkeeRule<S, V, E, R> = check.rule,
        val subject: S = check.subject,
        val value: V = check.value,
        val expectation: E = check.rule.expectation,
        val result: R = check.result,
        val valid: Boolean = check.isValid(),
        val severity: ValkeeSeverity = check.severity
    )
}
