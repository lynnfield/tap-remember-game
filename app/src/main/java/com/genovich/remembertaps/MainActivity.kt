package com.genovich.remembertaps

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import arrow.fx.IO

class MainActivity : AppCompatActivity() {

    private val view by lazy { App.View(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        lifecycleScope.launchWhenResumed {
            val ui: (App.State) -> IO<App.Action> = { state ->
                Log.d("Logic/State", state.toString())
                IO { view.show(state).also { Log.d("Logic/Action", it.toString()) } }
            }
            val initial = simpleInitial(ui, App.State.initial)
            execute(
                logic = simpleLogic(ui, App::process),
                fallback = { initial },
                initial = initial
            )
                .suspended()
        }
    }
}