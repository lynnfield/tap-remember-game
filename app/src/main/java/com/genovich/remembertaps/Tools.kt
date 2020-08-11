package com.genovich.remembertaps

import android.view.View
import arrow.core.*
import arrow.fx.IO
import arrow.fx.extensions.io.concurrent.raceN
import arrow.fx.extensions.io.functor.tupleLeft
import arrow.fx.handleErrorWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

interface Feature<STATE : Any, INPUT : Any> {
    fun process(input: Tuple2<STATE, INPUT>): Tuple2<STATE, NonEmptyList<IO<INPUT>>>
}

abstract class SimpleFeature<STATE : Any, INPUT : Any>(
    val ui: (STATE) -> IO<INPUT>
) : Feature<STATE, INPUT> {

    abstract fun simpleProcess(input: Tuple2<STATE, INPUT>): STATE

    override fun process(input: Tuple2<STATE, INPUT>): Tuple2<STATE, NonEmptyList<IO<INPUT>>> =
        simpleProcess(input).let { it toT Nel(ui(it)) }
}

interface Widget<STATE : Any, INPUT : Any> {
    suspend fun show(state: STATE): INPUT
}

fun <STATE : Any, INPUT : Any> execute(
    logic: (Tuple2<STATE, INPUT>) -> Tuple2<STATE, NonEmptyList<IO<INPUT>>>,
    fallback: (Throwable) -> IO<Tuple2<STATE, INPUT>>, // todo bad
    initial: Tuple2<STATE, NonEmptyList<IO<INPUT>>>,
    parallelDispatcher: CoroutineContext
): Flow<STATE> = flow {
    var value = initial

    while (coroutineContext.isActive) {
        emit(value.a)

        value = value.b.firstOfAll(coroutineContext + parallelDispatcher).tupleLeft(value.a)
            .handleErrorWith(fallback)
            .map(logic)
            .suspended()
    }
}

fun <T> NonEmptyList<IO<T>>.firstOfAll(coroutineContext: CoroutineContext): IO<T> =
    tail.fold(head) { acc, io ->
        coroutineContext.raceN(acc, io).map {
            it.fold(::identity, ::identity)
        }
    }

suspend fun View.awaitClick(): Unit = suspendCancellableCoroutine { continuation ->
    setOnClickListener {
        setOnClickListener(null)
        continuation.resume(Unit)
    }
    continuation.invokeOnCancellation {
        setOnClickListener(null)
    }
}
