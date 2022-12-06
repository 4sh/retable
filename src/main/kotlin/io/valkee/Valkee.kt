package io.valkee

import io.valkee.Validations.rule
import io.valkee.Validations.self
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object Valkee {
    inline operator fun <reified T : Any> invoke(): ValkeeBuilder<T> = builder()
    inline fun <reified T : Any> builder(): ValkeeBuilder<T> = ValkeeBuilder(T::class)
}

class ValkeeBuilder<T : Any>(val kClass: KClass<T>) {
    val rules: MutableList<ValkeeRule<T?, *, *, *>> = mutableListOf()

    inline fun <reified V : Any> builder(): ValkeeBuilder<V> = ValkeeBuilder(V::class)

    fun constraints(function: ValkeeBuilder<T>.() -> Unit): ValkeeRule<T?, T?, *, *> {
        function(this)

        return Validations.rule(
            id = "validations.${kClass.simpleName}",
            name = kClass.simpleName ?: "",
            property = self<T?>(),
            expectation = rules.toList(),
            predicate = { v, e -> e.map { it.validate(v) }.let { it.all { it.isValid() } to it } }
        )
    }

    inline fun <reified V : Any> on(
        p: KProperty<V>,
        function: ValkeeBuilder<V>.() -> ValkeeRule<V?, *, *, *>
    ): ValkeeRule<T?, V?, *, *> {
        val r = rule<T?, V?, ValkeeRule<V?, *, *, *>, RuleCheck<V?, *, *, *>>(
            id = "validations.${kClass.simpleName}.${p.name}",
            name = p.name,
            property = ValkeeProperty(p.name, { p.call(it) }),
            expectation = function.invoke(builder()),
            predicate = { v, e -> e.validate(v).toPair() }
        )
        rules.add(r)

        return r
    }
}
