package com.genovich.remembertaps

import arrow.core.Tuple2
import arrow.core.left
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.extensions.io.apply.tupled
import arrow.fx.handleErrorWith
import kotlin.coroutines.suspendCoroutine

interface Feature<STATE : Any, INPUT : Any> {
    fun process(input: Tuple2<STATE, INPUT>): STATE
}

interface Widget<STATE : Any, INPUT : Any> {
    fun show(state: STATE, callback: (INPUT) -> Unit)
}

suspend fun <STATE : Any, INPUT : Any> Widget<STATE, INPUT>.show(state: STATE): INPUT =
    suspendCoroutine { continuation ->
        show(state) { continuation.resumeWith(Result.success(it)) }
    }

fun <STATE : Any, INPUT : Any> simpleLogic(
    ui: (STATE) -> IO<INPUT>,
    logic: (Tuple2<STATE, INPUT>) -> STATE
): (Tuple2<STATE, INPUT>) -> Tuple2<IO<STATE>, IO<INPUT>> = { input ->
    logic(input).let { IO.just(it) toT ui(it) }
}

fun <STATE : Any, INPUT : Any> simpleInitial(
    ui: (STATE) -> IO<INPUT>,
    initial: STATE
): IO<Tuple2<STATE, INPUT>> = tupled(IO.just(initial), ui(initial))

// todo looks creepy
// todo exit condition?
fun <STATE : Any, INPUT : Any> execute(
    logic: (Tuple2<STATE, INPUT>) -> Tuple2<IO<STATE>, IO<INPUT>>,
    fallback: (Throwable) -> IO<Tuple2<STATE, INPUT>>,
    initial: IO<Tuple2<STATE, INPUT>>
): IO<STATE> = IO.tailRecM(initial) { newIo ->
    newIo                           // IO<Tuple<State, Input>>
        .handleErrorWith(fallback)  // IO<Tuple<State, Input>>
        .map(logic)                 // IO<Tuple<IO<State>, IO<Input>>
        .map { (stateIo, inputIo) -> tupled(stateIo, inputIo) } // IO<IO<Tuple<State, Input>>>
        .map { it.left() }          // IO<Either<IO<Tuple<State, Input>>, Nothing>>
}
