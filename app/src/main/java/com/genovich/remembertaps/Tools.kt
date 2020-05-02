package com.genovich.remembertaps

typealias Source<INPUT> = suspend () -> INPUT

suspend fun <STATE : Any, INPUT : Any> simple(
    initial: STATE,
    process: (Pair<STATE, INPUT>) -> STATE,
    show: (STATE) -> Source<INPUT>
): Nothing {
    var state = initial

    while (true) {
        val source = show(state)
        state = process(state to source())
    }
}