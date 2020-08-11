package com.genovich.remembertaps

import android.content.Context
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import arrow.core.Tuple2
import arrow.fx.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class Menu(ui: (State) -> IO<Action>) : SimpleFeature<Menu.State, Menu.Action>(ui) {
    sealed class State {
        object Menu : State()
    }

    sealed class Action {
        object Start : Action()
    }

    override fun simpleProcess(input: Tuple2<State, Action>): State = input.a

    class View(context: Context) : FrameLayout(context), Widget<State, Action> {

        private val start = Button(context).apply {
            setText(R.string.menu_start)
        }

        init {
            addView(start, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }

        override suspend fun show(state: State): Action =
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    start.setOnClickListener(null)
                }
                start.setOnClickListener {
                    it.setOnClickListener(null)
                    continuation.resume(Action.Start)
                }
            }
    }
}