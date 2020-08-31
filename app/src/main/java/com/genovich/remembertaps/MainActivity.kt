package com.genovich.remembertaps

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import arrow.core.NonEmptyList
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.IODispatchers
import arrow.fx.extensions.io.functor.tupleLeft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn

@ExperimentalStdlibApi
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    private val app by lazy {
        App(
            configureGame = ConfigureGame(
                ui = { IO { view.show(App.State.ConfigureGame(it)).action } }
            ),
            game = Game(
                ui = { IO { view.show(App.State.Game(it)).action } },
                sleep = { IO.sleep(it, IODispatchers.IOPool) }
            ),
            ui = { IO { view.show(it) } }
        )
    }
    private val view by lazy { App.View(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        execute(
            logic = { input ->
                Log.d("Logic/Step/State", " " + input.a.toString())
                Log.d("Logic/Step/Action", input.b.toString())
                app.process(input).also { output ->
                    Log.d("Logic/Step/Result", output.a.toString())
                }
            },
            fallback = {
                Log.d("Logic/Fallback", "fail", it)
                App.State.initial.let { state ->
                    IO { view.show(state) }.tupleLeft(state)
                }
            },
            initial = App.State.initial.let { it toT NonEmptyList(IO { view.show(it) }) },
            parallelDispatcher = IODispatchers.IOPool
        )
            .launchIn(lifecycleScope)
    }
}