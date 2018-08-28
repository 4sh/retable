package io.valkee

import io.valkee.Validations.Numbers.inRange
import io.valkee.Validations.Strings.length
import io.valkee.Validations.Strings.matches
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.isEqualTo

class ValkeeTest {
    @Test
    fun `should validate int range`() {
        val rule = Validations.Numbers.inRange(IntRange(0, 10))
        expect(rule.validate(2)) {
            rule().isEqualTo(rule)
            value().isEqualTo(2)
            severity().isEqualTo(ValidationSeverity.OK)
            message().isEqualTo("2 is between 0 and 10")
        }

        expect(rule.validate(12)) {
            rule().isEqualTo(rule)
            value().isEqualTo(12)
            severity().isEqualTo(ValidationSeverity.ERROR)
            message().isEqualTo("12 should be between 0 and 10")
        }
    }

    @Test
    fun `should validate string length`() {
        val rule = length(inRange(4..10))
        expect(rule.validate("test")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("test")
            value().isEqualTo(4)
            severity().isEqualTo(ValidationSeverity.OK)
            message().isEqualTo("\"test\" length 4 is between 4 and 10")
        }

        expect(rule.validate("te")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("te")
            value().isEqualTo(2)
            severity().isEqualTo(ValidationSeverity.ERROR)
            message().isEqualTo("\"te\" length 2 should be between 4 and 10")
        }
    }

    @Test
    fun `should validate string against pattern`() {
        val rule = matches(Regex("[abc]{3}"), "contain 3 characters among abc")
        expect(rule.validate("test")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("test")
            value().isEqualTo("test")
            severity().isEqualTo(ValidationSeverity.ERROR)
            message().isEqualTo("\"test\" should contain 3 characters among abc")
        }

        expect(rule.validate("cab")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("cab")
            value().isEqualTo("cab")
            severity().isEqualTo(ValidationSeverity.OK)
            message().isEqualTo("\"cab\"  contain 3 characters among abc")
        }
    }



    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.rule(): Assertion.Builder<ValidationRule<S, V, E, R>>
        = map(RuleCheck<S, V, E, R>::rule)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.subject(): Assertion.Builder<S>
        = map(RuleCheck<S, V, E, R>::subject)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.value(): Assertion.Builder<V>
        = map(RuleCheck<S, V, E, R>::value)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.severity(): Assertion.Builder<ValidationSeverity>
        = map(RuleCheck<S, V, E, R>::severity)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.message(): Assertion.Builder<String>
        = map(RuleCheck<S, V, E, R>::message)
}

