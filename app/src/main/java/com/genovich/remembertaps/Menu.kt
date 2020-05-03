package com.genovich.remembertaps

import android.content.Context
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import kotlin.coroutines.suspendCoroutine

object Menu {
    sealed class State {
        object Menu : State()
    }

    sealed class Action {
        object Start : Action()
    }

    fun process(input: Pair<State, Action>): State = input.first

    class View(context: Context) : FrameLayout(context) {

        private val start = Button(context).apply {
            setText(R.string.menu_start)
        }

        init {
            addView(start, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }

        suspend fun show(): Action = suspendCoroutine { continuation ->
            start.setOnClickListener {
                it.setOnClickListener(null)
                continuation.resumeWith(Result.success(Action.Start))
            }
        }
    }
}