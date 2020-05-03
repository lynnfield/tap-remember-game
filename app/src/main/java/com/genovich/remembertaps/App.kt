package com.genovich.remembertaps

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.coroutines.suspendCoroutine

object App {
    sealed class State {
        data class Menu(val state: com.genovich.remembertaps.Menu.State) : State()
        object ConfigureGame : State()
        object Game : State()
    }

    sealed class Action {
        data class Menu(val action: com.genovich.remembertaps.Menu.Action) : Action()
        object ConfigureGame : Action()
        object Game : Action()
    }

    fun process(input: Pair<State, Action>): State = when (val state = input.first) {
        is State.Menu -> when (val action = input.second) {
            is Action.Menu -> when (action.action) {
                Menu.Action.Start -> State.ConfigureGame
            }
            Action.ConfigureGame -> state
            Action.Game -> state
        }
        State.ConfigureGame -> when (input.second) {
            is Action.Menu -> state
            Action.ConfigureGame -> State.Game
            Action.Game -> state
        }
        State.Game -> when (input.second) {
            is Action.Menu -> state
            Action.ConfigureGame -> state
            Action.Game -> State.Menu(Menu.State.Menu)
        }
    }

    class View(context: Context) : FrameLayout(context) {

        private val stub = TextView(context).apply {
            gravity = Gravity.CENTER
        }

        private val menu = Menu.View(context)

        fun show(state: State): Source<Action> {
            return suspend {
                removeAllViews()
                when (state) {
                    is State.Menu -> {
                        addView(menu, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
                        Action.Menu(menu.show())
                    }
                    State.ConfigureGame -> {
                        addView(stub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                        stub.text = "Configure game"
                        suspendCoroutine<Action> { continuation ->
                            stub.setOnClickListener {
                                it.setOnClickListener(null)
                                continuation.resumeWith(Result.success(Action.ConfigureGame))
                            }
                        }
                    }
                    State.Game -> {
                        addView(stub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
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