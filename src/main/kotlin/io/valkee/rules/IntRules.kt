package io.valkee.rules

import io.valkee.Validations
import io.valkee.ValkeeBuilder

fun ValkeeBuilder<Int>.inRange(range: IntRange) =
    Validations.Numbers.inRange(range)
fun ValkeeBuilder<Int>.equals(expected: Int) =
    Validations.Numbers.equals(expected)
fun ValkeeBuilder<Int>.gt(expected: Int) =
    Validations.Numbers.greaterThan(expected)
fun ValkeeBuilder<Int>.lt(expected: Int) =
    Validations.Numbers.lowerThan(expected)
