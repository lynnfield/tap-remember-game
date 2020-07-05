package com.genovich.remembertaps

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import arrow.core.Tuple2
import arrow.core.toT

object App : Feature<App.State, App.Action> {
    sealed class State {
        data class Menu(val state: com.genovich.remembertaps.Menu.State) : State()
        data class ConfigureGame(val state: com.genovich.remembertaps.ConfigureGame.State) : State()
        data class Game(val state: com.genovich.remembertaps.Game.State) : State()

        companion object {
            val initial: State = Menu(com.genovich.remembertaps.Menu.State.Menu)
        }
    }

    sealed class Action {
        data class Menu(val action: com.genovich.remembertaps.Menu.Action) : Action()
        data class ConfigureGame(val aciton: com.genovich.remembertaps.ConfigureGame.Action) :
            Action()

        data class Game(val action: com.genovich.remembertaps.Game.Action) : Action()
    }

    override fun process(input: Tuple2<State, Action>): State = when (val state = input.a) {
        is State.Menu -> when (val action = input.b) {
            is Action.Menu -> when (action.action) {
                Menu.Action.Start -> State.ConfigureGame(ConfigureGame.State.initial)
            }
            is Action.ConfigureGame -> state
            is Action.Game -> state
        }
        is State.ConfigureGame -> when (val action = input.b) {
            is Action.Menu -> state
            is Action.ConfigureGame -> when (action.aciton) {
                ConfigureGame.Action.Next -> when (state.state) {
                    is ConfigureGame.State.PlayerList -> State.Game(
                        Game.State.Adding(
                            playersQueue = listOf(
                                state.state.first,
                                state.state.second
                            ) + state.state.others,
                            originalTaps = emptyList(),
                            currentTaps = emptyList()
                        )
                    )
                }
                else -> State.ConfigureGame(ConfigureGame.process(state.state toT action.aciton))
            }
            is Action.Game -> state
        }
        is State.Game -> when (val action = input.b) {
            is Action.Menu -> state
            is Action.ConfigureGame -> state
            is Action.Game -> when (action.action) {
                is Game.Action.PlayerTap -> State.Game(Game.process(state.state toT action.action))
                Game.Action.Next -> State.Menu(Menu.State.Menu)
            }
        }
    }

    class View(context: Context) : FrameLayout(context), Widget<State, Action> {

        private val menu = Menu.View(context)
        private val configureGame = ConfigureGame.View(context)
        private val game = Game.View(context)

        override fun show(state: State, callback: (Action) -> Unit) {
            removeAllViews()
            when (state) {
                is State.Menu -> {
                    addView(menu, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
                    menu.show(state.state) { callback(Action.Menu(it)) }
                }
                is State.ConfigureGame -> {
                    addView(configureGame, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    configureGame.show(state.state) {
                        callback(Action.ConfigureGame(it))
                    }
                }
                is State.Game -> {
                    addView(game, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    game.show(state.state) {
                        callback(Action.Game(it))
                    }
                }
            }
        }
    }
}