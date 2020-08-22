package com.genovich.remembertaps

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import arrow.core.NonEmptyList
import arrow.core.Tuple2
import arrow.core.toT
import arrow.fx.IO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalStdlibApi
class App(
    val configureGame: Feature<ConfigureGame.State, ConfigureGame.Action>,
    val game: Feature<Game.State, Game.Action>,
    val ui: (State) -> IO<Action>
) : Feature<App.State, App.Action> {

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
        data class ConfigureGame(val action: com.genovich.remembertaps.ConfigureGame.Action) :
            Action()

        data class Game(val action: com.genovich.remembertaps.Game.Action) : Action()
    }

    override fun process(input: Tuple2<State, Action>): Tuple2<State, NonEmptyList<IO<Action>>> =
        when (val state = input.a) {
            is State.Menu -> when (val action = input.b) {
                is Action.Menu -> when (action.action) {
                    Menu.Action.Start ->
                        State.ConfigureGame(ConfigureGame.State.initial).let(::stateAndShow)
                }
                is Action.ConfigureGame -> state.let(::stateAndShow)
                is Action.Game -> state.let(::stateAndShow)
            }
            is State.ConfigureGame -> when (val action = input.b) {
                is Action.Menu -> state.let(::stateAndShow)
                is Action.ConfigureGame -> when (action.action) {
                    ConfigureGame.Action.Next -> when (state.state) {
                        is ConfigureGame.State.PlayerList -> State.Game(
                            Game.State.Adding(
                                playersQueue = buildList {
                                    add(state.state.first)
                                    add(state.state.second)
                                    addAll(state.state.others)
                                },
                                originalTaps = emptyList(),
                                currentTaps = emptyList()
                            )
                        ).let(::stateAndShow)
                    }
                    is ConfigureGame.Action.Add, is ConfigureGame.Action.Remove, is ConfigureGame.Action.SetName ->
                        configureGame.process(state.state toT action.action).bimap(
                            fl = { State.ConfigureGame(it) },
                            fr = { actions -> actions.map { it.map(Action::ConfigureGame) } }
                        )
                }
                is Action.Game -> state.let(::stateAndShow)
            }
            is State.Game -> when (val action = input.b) {
                is Action.Menu -> state.let(::stateAndShow)
                is Action.ConfigureGame -> state.let(::stateAndShow)
                is Action.Game -> when (action.action) {
                    Game.Action.Next -> State.Menu(Menu.State.Menu).let(::stateAndShow)
                    is Game.Action.PlayerTap, Game.Action.Timeout ->
                        game.process(state.state toT action.action).bimap(
                            fl = { State.Game(it) },
                            fr = { actions -> actions.map { it.map(Action::Game) } }
                        )
                }
            }
        }

    private fun stateAndShow(state: State) = state toT NonEmptyList(ui(state))

    class View(context: Context) : FrameLayout(context), Widget<State, Action> {

        private val main = Dispatchers.Main
        private val menu = Menu.View(context)
        private val configureGame = ConfigureGame.View(context)
        private val game = Game.View(context)

        override suspend fun show(state: State): Action = when (state) {
            is State.Menu -> show(state)
            is State.ConfigureGame -> show(state)
            is State.Game -> show(state)
        }

        suspend fun show(state: State.Menu): Action.Menu = withContext(main) {
            removeAllViews()
            addView(menu, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            Action.Menu(menu.show(state.state))
        }

        suspend fun show(state: State.ConfigureGame): Action.ConfigureGame = withContext(main) {
            removeAllViews()
            addView(configureGame, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            Action.ConfigureGame(configureGame.show(state.state))
        }

        suspend fun show(state: State.Game): Action.Game = withContext(main) {
            removeAllViews()
            addView(game, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            Action.Game(game.show(state.state))
        }
    }
}