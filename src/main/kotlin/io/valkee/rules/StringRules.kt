package io.valkee.rules

import io.valkee.Validations
import io.valkee.ValkeeBuilder
import io.valkee.ValkeeRule

fun <E, R> ValkeeBuilder<String>.length(function: ValkeeBuilder<Int>.() -> ValkeeRule<Int?, Int?, E, R>) =
    Validations.Strings.length(function.invoke(builder()))

fun ValkeeBuilder<String>.equals(expected: String) =
    Validations.Strings.equals(expected)
fun ValkeeBuilder<String>.equalsIgnoreCase(expected: String) =
    Validations.Strings.equalsIgnoreCase(expected)
fun ValkeeBuilder<String>.isInteger() =
    Validations.Strings.isInteger()
fun ValkeeBuilder<String>.matches(regex: Regex, message: String = "match {expectation}") =
    Validations.Strings.matches(regex, message)
