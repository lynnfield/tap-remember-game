package com.genovich.remembertaps

import android.content.Context
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import arrow.core.Tuple2

object Menu : Feature<Menu.State, Menu.Action> {
    sealed class State {
        object Menu : State()
    }

    sealed class Action {
        object Start : Action()
    }

    override fun process(input: Tuple2<State, Action>): State = input.a

    class View(context: Context) : FrameLayout(context), Widget<State, Action> {

        private val start = Button(context).apply {
            setText(R.string.menu_start)
        }

        init {
            addView(start, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }

        override fun show(state: State, callback: (Action) -> Unit) {
            start.setOnClickListener {
                it.setOnClickListener(null)
                callback(Action.Start)
            }
        }
    }
}