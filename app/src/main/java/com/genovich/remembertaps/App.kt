package com.genovich.remembertaps

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import arrow.core.Tuple2

object App : Feature<App.State, App.Action> {
    sealed class State {
        data class Menu(val state: com.genovich.remembertaps.Menu.State) : State()
        object ConfigureGame : State()
        object Game : State()

        companion object {
            val initial: State = Menu(com.genovich.remembertaps.Menu.State.Menu)
        }
    }

    sealed class Action {
        data class Menu(val action: com.genovich.remembertaps.Menu.Action) : Action()
        object ConfigureGame : Action()
        object Game : Action()
    }

    override fun process(input: Tuple2<State, Action>): State = when (val state = input.a) {
        is State.Menu -> when (val action = input.b) {
            is Action.Menu -> when (action.action) {
                Menu.Action.Start -> State.ConfigureGame
            }
            Action.ConfigureGame -> state
            Action.Game -> state
        }
        State.ConfigureGame -> when (input.b) {
            is Action.Menu -> state
            Action.ConfigureGame -> State.Game
            Action.Game -> state
        }
        State.Game -> when (input.b) {
            is Action.Menu -> state
            Action.ConfigureGame -> state
            Action.Game -> State.Menu(Menu.State.Menu)
        }
    }

    class View(context: Context) : FrameLayout(context), Widget<State, Action> {

        private val stub = TextView(context).apply {
            gravity = Gravity.CENTER
        }

        private val menu = Menu.View(context)

        override fun show(state: State, callback: (Action) -> Unit) {
            removeAllViews()
            when (state) {
                is State.Menu -> {
                    addView(menu, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
                    menu.show(state.state) { callback(Action.Menu(it)) }
                }
                State.ConfigureGame -> {
                    addView(stub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    stub.text = "Configure game"
                    stub.setOnClickListener {
                        it.setOnClickListener(null)
                        callback(Action.ConfigureGame)
                    }
                }
                State.Game -> {
                    addView(stub, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    stub.text = "Game"
                    stub.setOnClickListener {
                        it.setOnClickListener(null)
                        callback(Action.Game)
                    }
                }
            }
        }
    }
}