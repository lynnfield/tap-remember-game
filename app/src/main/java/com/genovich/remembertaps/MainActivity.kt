package com.genovich.remembertaps

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.text.NumberFormat
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    private val textView by lazy {
        TextView(this).apply {
            gravity = Gravity.CENTER
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(textView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        lifecycleScope.launchWhenResumed {
            simple(
                initial = 1,
                process = { (state, input) ->
                    state + input
                },
                show = { state ->
                    suspend {
                        textView.text = NumberFormat.getInstance().format(state)
                        suspendCoroutine<Int> { continuation ->
                            textView.setOnClickListener {
                                it.setOnClickListener(null)
                                continuation.resumeWith(Result.success(1))
                            }
                        }
                    }
                }
            )
        }
    }
}