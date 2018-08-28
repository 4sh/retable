package io.valkee


object Valkee {
    inline operator fun <reified T> invoke():ValkeeBuilder<T> = builder()
    inline fun <reified T> builder():ValkeeBuilder<T> = ValkeeBuilder()
}

class ValkeeBuilder<T> {
    internal inline fun <reified T> builder():ValkeeBuilder<T> = ValkeeBuilder()
}
