package com.genovich.remembertaps

import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private val view by lazy { App.View(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(view, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        lifecycleScope.launchWhenResumed {
            simple(
                initial = App.State.Menu(Menu.State.Menu),
                process = App::process,
                show = view::show
            )
        }
    }
}