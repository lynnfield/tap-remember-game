package com.genovich.remembertaps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import arrow.core.Tuple2
import arrow.syntax.collections.tail
import kotlin.math.min

object Game : Feature<Game.State, Game.Action> {

    val tolerance = .05f

    sealed class State {

        data class Adding(
            // todo maybe non empty list?
            val playersQueue: List<Player>,
            val originalTaps: List<Pair<Player, Tap>>,
            val currentTaps: List<Tap>
        ) : State()

        data class Repeating(
            val playersQueue: List<Player>,
            val originalTaps: List<Pair<Player, Tap>>,
            val currentTaps: List<Tap>
        ) : State()

        data class GameOver(
            val loser: Player,
            val others: List<Player>,
            val originalTaps: List<Pair<Player, Tap>>,
            val currentTaps: List<Tap>
        ) : State()
    }

    sealed class Action {
        data class PlayerTap(val tap: Tap) : Action()
        object Next : Action()
    }

    override fun process(input: Tuple2<State, Action>): State = when (val state = input.a) {
        is State.Adding -> when (val action = input.b) {
            is Action.PlayerTap -> State.Repeating(
                playersQueue = state.playersQueue.tail() + state.playersQueue.first(),
                originalTaps = state.originalTaps + (state.playersQueue.first() to action.tap),
                currentTaps = emptyList()
            )
            Action.Next -> state
        }
        is State.Repeating -> when (val action = input.b) {
            is Action.PlayerTap -> {
                // add to currentTaps
                val taps = state.currentTaps + action.tap
                // check correctness
                if (state.originalTaps.map { it.second }.zip(taps).all(tolerance::near)) {
                    // if currentTaps == originalTaps -> Adding
                    if (state.originalTaps.size == taps.size) {
                        State.Adding(
                            playersQueue = state.playersQueue,
                            originalTaps = state.originalTaps,
                            currentTaps = taps
                        )
                    } else {
                        state.copy(currentTaps = taps)
                    }
                } else {
                    // if failed -> GameOver
                    State.GameOver(
                        loser = state.playersQueue.first(),
                        others = state.playersQueue.tail(),
                        originalTaps = state.originalTaps,
                        currentTaps = taps
                    )
                }
            }
            Action.Next -> state
        }
        is State.GameOver -> state
    }

    class View(context: Context) : LinearLayout(context), Widget<State, Action> {

        private val nameField = TextView(context)
        private val stateField = TextView(context)
        private val gameField = GameField(context)

        init {
            orientation = VERTICAL
            addView(nameField, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(stateField, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            addView(gameField, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        /*
            |---------------------|
            |current player's name|
            |current state's name |
            |---------------------|
            |  *                  |
            |            *        |
            |       *             |
            |                *    |
            |           *         |
            |   *                 |
            |---------------------|
        */

        override fun show(state: State, callback: (Action) -> Unit) = when (state) {
            is State.Adding -> {
                nameField.text = state.playersQueue.first().name
                stateField.text = "add tap"
                gameField.show(state.currentTaps) { callback(Action.PlayerTap(it)) }
            }
            is State.Repeating -> {
                nameField.text = state.playersQueue.first().name
                stateField.text = "repeat taps"
                gameField.show(state.currentTaps) { callback(Action.PlayerTap(it)) }
            }
            is State.GameOver -> {
                nameField.text = state.loser.name
                stateField.text = "lose"
                gameField.show(state.originalTaps.map { it.second }) {
                    callback(Action.Next)
                }
                setOnClickListener {
                    setOnClickListener(null)
                    callback(Action.Next)
                }
            }
        }
    }

    class GameField(context: Context) : android.view.View(context), Widget<List<Tap>, Tap> {

        private var taps: List<Tap> = listOf()

        private var radius: Float = 0f
        private val circlePaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.DKGRAY
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            radius = min(measuredHeight, measuredWidth) * tolerance
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            taps.forEach {
                canvas.drawCircle(it.x * width, it.y * height, radius, circlePaint)
            }
        }

        override fun show(state: List<Tap>, callback: (Tap) -> Unit) {
            taps = state
            setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    setOnTouchListener(null)
                    performClick()
                    callback(Tap(event.x / v.width, event.y / v.height))
                    true
                } else {
                    false
                }
            }
        }
    }
}
