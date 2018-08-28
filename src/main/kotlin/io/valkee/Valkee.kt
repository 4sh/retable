package io.valkee


/**
 * Validations provides a bunch of useful ValkeeRule
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
                message = MsgTpl(rule = "between"),
                contextDataResolver = context({
                    mapOf(
                            "expectation" to "${expectation.first} and ${expectation.last}")
                }))
        fun inRange(expected:LongRange) = selfRule<Long?, LongRange>(
                id = "validations.numbers.inRange", expectation = expected,
                predicate = { v, e -> v != null && e.contains(v) },
                message = MsgTpl(rule = "between"),
                        contextDataResolver = context({
                            mapOf(
                                    "expectation" to "${expectation.first} and ${expectation.last}")
                        }))
    }

    object Strings {
        fun <E> length(expected: ValkeeRule<Int?, Int?, E, Unit>) = rule(
                id = "validations.string.length",
                expectation = expected,
                property = ValkeeProperty<String?, Int>("length", { it?.length ?: 0 }),
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
                          property: ValkeeProperty<S, V>,
                          severity: ValkeeSeverity = ValkeeSeverity.ERROR,
                          expectation: E,
                          predicate: (V, E) -> Pair<Boolean, R>,
                          message: MsgTpl = MsgTpl(name),
                          contextDataResolver: RuleCheckMessageTemplate.Context<S, V, E, R>.() -> Map<String,*> = context(),
                          templateInterpolator: String.(Map<String, *>) -> String = Validations.templateInterpolate,
                          templateResolver: String.(RuleCheckMessageTemplate.Context<S, V, E, R>) -> String =
                                  { ctx -> templateInterpolator.invoke(this, contextDataResolver.invoke(ctx)) },
                          i18nResolver: (String) -> String = Validations.i18nResolver,
                          messageBuilder: ValkeeRuleMessageBuilder<S,V,E,R> = ValkeeRuleMessageBuilder(
                                  validMessage = message.okMessage(),
                                  invalidMessage = message.nokMessage(),
                                  templateResolver = templateResolver,
                                  i18nResolver = i18nResolver
                          )
    ): ValkeeRule<S, V, E, R> = ValkeeRule(
            id = id, name = name,
            property = property, severity = severity,
            expectation = expectation,
            predicate = predicate,
            messageBuilder = messageBuilder
    )
    fun <S, E> selfRule(id:String,
                        name:String = id.substringAfterLast('.'),
                        severity: ValkeeSeverity = ValkeeSeverity.ERROR,
                        expectation: E,
                        predicate: (S, E) -> Boolean,
                        message: MsgTpl = MsgTpl(name),
                        contextDataResolver: RuleCheckMessageTemplate.Context<S, S, E, Unit>.() -> Map<String,*> = context(),
                        templateInterpolator: String.(Map<String, *>) -> String = Validations.templateInterpolate,
                        templateResolver: String.(RuleCheckMessageTemplate.Context<S, S, E, Unit>) -> String =
                                { ctx -> templateInterpolator.invoke(this, contextDataResolver.invoke(ctx)) },
                        i18nResolver: (String) -> String = Validations.i18nResolver,
                        messageBuilder: ValkeeRuleMessageBuilder<S, S, E, Unit> = ValkeeRuleMessageBuilder(
                                validMessage = message.okMessage(),
                                invalidMessage = message.nokMessage(),
                                templateResolver = templateResolver,
                                i18nResolver = i18nResolver
                        )
    ) = ValkeeRule(
            id = id, name = name,
            property = self(), severity = severity,
            expectation = expectation,
            predicate = { v, e -> return@ValkeeRule predicate(v, e) to Unit },
            messageBuilder = messageBuilder
    )

    class MsgTpl(
            rule:String = "",
            message:String = "{subject} {property} {value} {verb} {rule} {expectation}",
            okVerb:String = "is",
            nokVerb:String = "should be",
            private val okMessage:String = message.interpolate(mapOf("verb" to okVerb, "rule" to rule)),
            private val nokMessage:String = message.interpolate(mapOf("verb" to nokVerb, "rule" to rule))
    ) {
        fun okMessage() = I18nMessage(".ok", okMessage)
        fun nokMessage() = I18nMessage(".nok", nokMessage)
    }

    fun <S> self(): ValkeeProperty<S, S> = ValkeeProperty("self", { it })

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
    var templateInterpolate:(String,Map<String, *>) -> String = { tpl, context ->
        // very naive templating mechanism
        var msg = tpl
        context.forEach {
            msg = msg.replace("{${it.key}}", it.value?.toString()?:"")
        }

        msg.trim()
    }

    fun String.interpolate(context: Map<String, *>):String = templateInterpolate(this,context)
}
