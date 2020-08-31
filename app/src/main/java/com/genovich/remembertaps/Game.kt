package com.genovich.remembertaps

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.setPadding
import arrow.core.NonEmptyList
import arrow.core.Tuple2
import arrow.core.toT
import arrow.fx.IO
import arrow.fx.extensions.io.functor.mapConst
import arrow.fx.typeclasses.Duration
import arrow.fx.typeclasses.seconds
import arrow.syntax.collections.tail
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.resume
import kotlin.math.min

class Game(
    val ui: (State) -> IO<Action>,
    val sleep: (Duration) -> IO<Unit>
) : Feature<Game.State, Game.Action> {

    companion object {
        // todo extract dependency?
        const val tolerance = .05f

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
            setPadding(context.resources.getDimensionPixelOffset(R.dimen.dp8))
            clipToPadding = false
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

        @ExperimentalCoroutinesApi
        override suspend fun show(state: State) = when (state) {
            is State.Adding -> show(state)
            is State.Repeating -> show(state)
            is State.GameOver -> show(state)
        }

        @Suppress("MemberVisibilityCanBePrivate")
        suspend fun show(state: State.GameOver): Action.Next = withContext(main) {
            nameField.text = state.loser.name
            stateField.text = context.getString(R.string.game_lose)
            oneOf(
                later { awaitClick().let { Action.Next } },
                later { gameField.show(state.originalTaps.map { it.second }).let { Action.Next } }
            )
        }

        @ExperimentalCoroutinesApi
        suspend fun show(state: State.Repeating): Action.PlayerTap = withContext(main) {
            nameField.text = state.playersQueue.first().name
            val statePrefix = context.getString(R.string.game_repeat_taps)
            stateField.text = statePrefix
            oneOf(
                forever {
                    animation(state.timeout).collect {
                        if (isActive) {
                            stateField.text = String.format("$statePrefix %.3f", it / 1000f)
                        }
                    }
                },
                later { Action.PlayerTap(gameField.show(state.currentTaps)) }
            )
        }

        @Suppress("MemberVisibilityCanBePrivate")
        suspend fun show(state: State.Adding): Action.PlayerTap = withContext(main) {
            nameField.text = state.playersQueue.first().name
            stateField.text = context.getString(R.string.game_add_tap)
            Action.PlayerTap(gameField.show(state.currentTaps))
        }
    }

    class GameField(context: Context) : CardView(context), Widget<List<Tap>, Tap> {

        private var taps: List<Tap> = listOf()

        private var circleRadius: Float = 0f
        private val circlePaint = Paint().apply {
            style = Paint.Style.FILL
            color = TypedValue().also {
                context.theme.resolveAttribute(
                    R.attr.colorSecondary,
                    it,
                    true
                )
            }.data
        }

        init {
            radius = context.resources.getDimension(R.dimen.dp8)
            elevation = context.resources.getDimension(R.dimen.dp8)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val defaultWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
            val defaultHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
            val size = min(defaultWidth, defaultHeight)
            setMeasuredDimension(size, size)
            circleRadius = min(measuredHeight, measuredWidth) * tolerance
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            taps.forEach {
                canvas.drawCircle(it.x * width, it.y * height, circleRadius, circlePaint)
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
