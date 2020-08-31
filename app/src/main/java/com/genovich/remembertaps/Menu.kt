package com.genovich.remembertaps

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import arrow.core.Tuple2
import arrow.fx.IO
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class Menu(ui: (State) -> IO<Action>) : SimpleFeature<Menu.State, Menu.Action>(ui) {
    sealed class State {
        object Menu : State() {
            override fun toString() = this::class.qualifiedName!!
        }
    }

    sealed class Action {
        object Start : Action() {
            override fun toString() = this::class.qualifiedName!!
        }
    }

    override fun simpleProcess(input: Tuple2<State, Action>): State = input.a

    class View(context: Context) : FrameLayout(context), Widget<State, Action> {

        private val start = MaterialButton(context).apply {
            setText(R.string.menu_start)
        }

        init {
            addView(start, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
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