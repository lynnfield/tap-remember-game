package com.genovich.remembertaps

import android.animation.ValueAnimator
import android.util.Log
import android.view.View
import arrow.core.*
import arrow.fx.IO
import arrow.fx.extensions.io.concurrent.raceN
import arrow.fx.extensions.io.functor.tupleLeft
import arrow.fx.handleErrorWith
import arrow.fx.typeclasses.Duration
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.selectUnbiased
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

suspend fun <T> oneOf(vararg tasks: Deferred<T>): T = selectUnbiased {
    tasks.forEach { task ->
        task.onAwait {
            tasks.filter { it !== task }.forEach { it.cancel() }
            it
        }
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

fun animation(duration: Duration): Flow<Int> = callbackFlow {
    val timeout = duration.timeUnit.toMillis(duration.amount)
    ValueAnimator.ofInt(timeout.toInt(), 0).apply {
        this.duration = timeout
        addUpdateListener {
            if (!isClosedForSend) offer(it.animatedValue as Int)
        }
        start()
        awaitClose {
            Log.d("Cancel", "Cancel")
            cancel()
        }
    }
}
