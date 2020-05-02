package com.genovich.remembertaps

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.coroutines.suspendCoroutine

object App {
    sealed class State {
        object Menu : State()
        object ConfigureGame : State()
        object Game : State()
    }

    sealed class Action {
        object Menu : Action()
        object ConfigureGame : Action()
        object Game : Action()
    }

    fun process(input: Pair<State, Action>): State = when (val state = input.first) {
        State.Menu -> when (input.second) {
            Action.Menu -> State.ConfigureGame
            Action.ConfigureGame -> state
            Action.Game -> state
        }
        State.ConfigureGame -> when (input.second) {
            Action.Menu -> state
            Action.ConfigureGame -> State.Game
            Action.Game -> state
        }
        State.Game -> when (input.second) {
            Action.Menu -> state
            Action.ConfigureGame -> state
            Action.Game -> State.Menu
        }
    }

    class View(context: Context) : FrameLayout(context) {

        private val stub = TextView(context).apply {
            gravity = Gravity.CENTER
        }

        init {
            addView(stub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        fun show(state: State): Source<Action> {
            return suspend {
                when (state) {
                    State.Menu -> {
                        stub.text = "Menu"
                        suspendCoroutine<Action> { continuation ->
                            stub.setOnClickListener {
                                it.setOnClickListener(null)
                                continuation.resumeWith(Result.success(Action.Menu))
                            }
                        }
                    }
                    State.ConfigureGame -> {
                        stub.text = "Configure game"
                        suspendCoroutine { continuation ->
                            stub.setOnClickListener {
                                it.setOnClickListener(null)
                                continuation.resumeWith(Result.success(Action.ConfigureGame))
                            }
                        }
                    }
                    State.Game -> {
                        stub.text = "Game"
                        suspendCoroutine { continuation ->
                            stub.setOnClickListener {
                                it.setOnClickListener(null)
                                continuation.resumeWith(Result.success(Action.Game))
                            }
                        }
                    }
                }
            }
        }
    }
}