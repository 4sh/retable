package io.retable.validation

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.isEqualTo

class ValidationsTest {
    @Test
    fun `should validate int range`() {
        val rule = Validations.Numbers.inRange(IntRange(0, 10))
        expect(rule.validate(2)) {
            rule().isEqualTo(rule)
            value().isEqualTo(2)
            severity().isEqualTo(ValidationSeverity.OK)
            message().isEqualTo("2 is inRange 0..10")
        }

        expect(rule.validate(12)) {
            rule().isEqualTo(rule)
            value().isEqualTo(12)
            severity().isEqualTo(ValidationSeverity.ERROR)
            message().isEqualTo("12 should be inRange 0..10")
        }
    }

    @Test
    fun `should validate string length`() {
        val rule = Validations.Strings.length(Validations.Numbers.inRange(IntRange(4, 10)))
        expect(rule.validate("test")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("test")
            value().isEqualTo(4)
            severity().isEqualTo(ValidationSeverity.OK)
            message().isEqualTo("`test` length 4 is inRange 4..10")
        }

        expect(rule.validate("te")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("te")
            value().isEqualTo(2)
            severity().isEqualTo(ValidationSeverity.ERROR)
            message().isEqualTo("`te` length 2 should be inRange 4..10")
        }
    }



    fun <S,V,E,R> Assertion.Builder<RuleCheck<S,V,E,R>>.rule(): Assertion.Builder<ValidationRule<S,V,E,R>>
        = map(RuleCheck<S,V,E,R>::rule)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S,V,E,R>>.subject(): Assertion.Builder<S>
        = map(RuleCheck<S,V,E,R>::subject)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S,V,E,R>>.value(): Assertion.Builder<V>
        = map(RuleCheck<S,V,E,R>::value)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S,V,E,R>>.severity(): Assertion.Builder<ValidationSeverity>
        = map(RuleCheck<S,V,E,R>::severity)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S,V,E,R>>.message(): Assertion.Builder<String>
        = map(RuleCheck<S,V,E,R>::message)
}

