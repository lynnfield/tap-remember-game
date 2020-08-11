package com.genovich.remembertaps

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import arrow.core.NonEmptyList
import arrow.core.Tuple2
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.extensions.io.functor.mapConst
import arrow.fx.typeclasses.Duration
import arrow.fx.typeclasses.seconds
import arrow.syntax.collections.tail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.min

class Game(
    val ui: (State) -> IO<Action>,
    val sleep: (Duration) -> IO<Unit>
) : Feature<Game.State, Game.Action> {

    companion object {
        // todo extract dependency?
        val tolerance = .05f

        // todo extract dependency?
        val timeout = 2.seconds
    }

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
            val currentTaps: List<Tap>,
            val timeout: Duration
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

        object Timeout : Action() {
            override fun toString() = this::class.simpleName!!
        }

        object Next : Action() {
            override fun toString() = this::class.simpleName!!
        }
    }

    override fun process(input: Tuple2<State, Action>): Tuple2<State, NonEmptyList<IO<Action>>> =
        when (val state = input.a) {
            is State.Adding -> when (val action = input.b) {
                is Action.PlayerTap -> State.Repeating(
                    playersQueue = state.playersQueue.tail() + state.playersQueue.first(),
                    originalTaps = state.originalTaps + (state.playersQueue.first() to action.tap),
                    currentTaps = emptyList(),
                    timeout = timeout
                ).let { it toT NonEmptyList(ui(it), sleep(it.timeout).mapConst(Action.Timeout)) }
                Action.Next, Action.Timeout -> state.let(::stateAndShow)
            }
            is State.Repeating -> when (val action = input.b) {
                is Action.PlayerTap -> {
                    // add to currentTaps
                    val taps = state.currentTaps + action.tap
                    // check correctness
                    if (state.originalTaps.map { it.second }.zip(taps).lastOrNull()
                            ?.let(tolerance::near) != false
                    ) {
                        // if currentTaps == originalTaps -> Adding
                        if (state.originalTaps.size == taps.size) {
                            State.Adding(
                                playersQueue = state.playersQueue,
                                originalTaps = state.originalTaps,
                                currentTaps = taps
                            ).let(::stateAndShow)
                        } else {
                            state.copy(currentTaps = taps).let {
                                it toT NonEmptyList(
                                    ui(it),
                                    sleep(it.timeout).mapConst(Action.Timeout)
                                )
                            }
                        }
                    } else {
                        // if failed -> GameOver
                        State.GameOver(
                            loser = state.playersQueue.first(),
                            others = state.playersQueue.tail(),
                            originalTaps = state.originalTaps,
                            currentTaps = taps
                        ).let(::stateAndShow)
                    }
                }
                Action.Next -> state.let(::stateAndShow)
                Action.Timeout ->
                    State.GameOver(
                        loser = state.playersQueue.first(),
                        others = state.playersQueue.tail(),
                        originalTaps = state.originalTaps,
                        currentTaps = state.currentTaps
                    ).let(::stateAndShow)
            }
            is State.GameOver -> state.let(::stateAndShow)
        }

    private fun stateAndShow(state: State) = state toT NonEmptyList(ui(state))

    class View(context: Context) : LinearLayout(context), Widget<State, Action> {

        private val main = Dispatchers.Main

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

        private var anim: ValueAnimator? = null

        override suspend fun show(state: State) = when (state) {
            is State.Adding -> show(state)
            is State.Repeating -> show(state)
            is State.GameOver -> show(state)
        }

        suspend fun show(state: State.GameOver): Action.Next = withContext(main) {
            nameField.text = state.loser.name
            // todo failed to update value: add tap; repeat; add tap; repeat first; timeout -> don't see lose
            stateField.text = context.getString(R.string.game_lose)
            gameField.show(state.originalTaps.map { it.second }).let { Action.Next }
            // todo listen both
//            setOnClickListener {
//                setOnClickListener(null)
//                callback(Action.Next)
//            }
        }

        suspend fun show(state: State.Repeating): Action.PlayerTap = withContext(main) {
            nameField.text = state.playersQueue.first().name
            val statePrefix = context.getString(R.string.game_repeat_taps)
            stateField.text = statePrefix
            val timeout = state.timeout.timeUnit.toMillis(state.timeout.amount)
            // todo animation cancellation
            anim = ValueAnimator.ofInt(timeout.toInt(), 0).apply {
                duration = timeout
                addUpdateListener {
                    stateField.text =
                        String.format("$statePrefix %.3f", it.animatedValue as Int / 1000f)
                }
                start()
            }
            Action.PlayerTap(gameField.show(state.currentTaps))
        }

        suspend fun show(state: State.Adding): Action.PlayerTap = withContext(main) {
            anim?.cancel()
            nameField.text = state.playersQueue.first().name
            stateField.text = context.getString(R.string.game_add_tap)
            Action.PlayerTap(gameField.show(state.currentTaps))
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

        override suspend fun show(state: List<Tap>): Tap =
            suspendCancellableCoroutine { continuation ->
                taps = state
                setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        setOnTouchListener(null)
                        performClick()
                        continuation.resume(Tap(event.x / v.width, event.y / v.height))
                        true
                    } else {
                        false
                    }
                }
                continuation.invokeOnCancellation {
                    setOnTouchListener(null)
                }
                postInvalidate()
            }
    }
}
