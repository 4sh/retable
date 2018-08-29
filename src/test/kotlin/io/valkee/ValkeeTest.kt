package io.valkee

import io.valkee.rules.*
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expect
import strikt.assertions.isEqualTo

class ValkeeTest {
    @Test
    fun `should validate int range`() {
        val rule = Valkee<Int>().inRange(0..10)
        expect(rule.validate(2)) {
            rule().isEqualTo(rule)
            value().isEqualTo(2)
            severity().isEqualTo(ValkeeSeverity.OK)
            message().isEqualTo("2 is between 0 and 10")
        }

        expect(rule.validate(12)) {
            rule().isEqualTo(rule)
            value().isEqualTo(12)
            severity().isEqualTo(ValkeeSeverity.ERROR)
            message().isEqualTo("12 should be between 0 and 10")
        }
    }

    @Test
    fun `should validate string length`() {
        val rule = Valkee<String>().length { inRange(4..10) }
        expect(rule.validate("test")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("test")
            value().isEqualTo(4)
            severity().isEqualTo(ValkeeSeverity.OK)
            message().isEqualTo("\"test\" length 4 is between 4 and 10")
        }

        expect(rule.validate("te")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("te")
            value().isEqualTo(2)
            severity().isEqualTo(ValkeeSeverity.ERROR)
            message().isEqualTo("\"te\" length 2 should be between 4 and 10")
        }
    }

    @Test
    fun `should validate string against pattern`() {
        val rule = Valkee<String>().matches(Regex("[abc]{3}"), "contain 3 characters among abc")
        expect(rule.validate("test")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("test")
            value().isEqualTo("test")
            severity().isEqualTo(ValkeeSeverity.ERROR)
            message().isEqualTo("\"test\" should contain 3 characters among abc")
        }

        expect(rule.validate("cab")) {
            rule().isEqualTo(rule)
            subject().isEqualTo("cab")
            value().isEqualTo("cab")
            severity().isEqualTo(ValkeeSeverity.OK)
            message().isEqualTo("\"cab\"  contain 3 characters among abc")
        }
    }

    @Test
    fun `should validate object with string and int properties`() {
        data class User(val name:String,val age:Int)

        val rule = Valkee<User>().constraints {
            on(User::name) {
                length { inRange(4..10) }
            }
            on(User::age) {
                inRange(0..120)
            }
        }

        val user = User("John", 40)
        val check = rule.validate(user)
        expect(check.subject).isEqualTo(user)
        expect(check.value).isEqualTo(user)
        expect(check.severity).isEqualTo(ValkeeSeverity.OK)

        expect(rule.validate(User("1", 40)).severity).isEqualTo(ValkeeSeverity.ERROR)
        expect(rule.validate(User("John", 140)).severity).isEqualTo(ValkeeSeverity.ERROR)
    }



    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.rule(): Assertion.Builder<ValkeeRule<S, V, E, R>>
        = map(RuleCheck<S, V, E, R>::rule)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.subject(): Assertion.Builder<S>
        = map(RuleCheck<S, V, E, R>::subject)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.value(): Assertion.Builder<V>
        = map(RuleCheck<S, V, E, R>::value)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.severity(): Assertion.Builder<ValkeeSeverity>
        = map(RuleCheck<S, V, E, R>::severity)
    fun <S,V,E,R> Assertion.Builder<RuleCheck<S, V, E, R>>.message(): Assertion.Builder<String>
        = map(RuleCheck<S, V, E, R>::message)
}

