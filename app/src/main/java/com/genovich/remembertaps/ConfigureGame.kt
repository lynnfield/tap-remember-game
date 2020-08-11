package com.genovich.remembertaps

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Tuple2
import arrow.fx.IO
import kotlinx.coroutines.*
import java.lang.Math.random
import kotlin.coroutines.resume

class ConfigureGame(ui: (State) -> IO<Action>) :
    SimpleFeature<ConfigureGame.State, ConfigureGame.Action>(ui) {
    sealed class State {
        data class PlayerList(
            val first: Player,
            val second: Player,
            val others: List<Player>
        ) : State()

        companion object {
            val initial: State = PlayerList(
                first = Player("player1"),
                second = Player("player2"),
                others = emptyList()
            )
        }
    }

    sealed class Action {
        data class Add(val player: Player) : Action()
        data class Remove(val player: Player) : Action()
        data class SetName(val player: Player, val name: String) : Action()
        object Next : Action()
    }

    override fun simpleProcess(input: Tuple2<State, Action>): State = when (val state = input.a) {
        is State.PlayerList -> when (val action = input.b) {
            is Action.Add -> state.copy(others = state.others + action.player)
            is Action.Remove -> when {
                state.others.isEmpty() -> state
                state.first == action.player -> State.PlayerList(
                    first = state.second,
                    second = state.others.first(),
                    others = state.others.drop(1)
                )
                state.second == action.player -> State.PlayerList(
                    first = state.first,
                    second = state.others.first(),
                    others = state.others.drop(1)
                )
                else -> state.copy(others = state.others.filter { it != action.player })
            }
            is Action.SetName -> {
                when (action.player) {
                    state.first -> state.copy(first = state.first.copy(name = action.name))
                    state.second -> state.copy(first = state.second.copy(name = action.name))
                    else -> state.copy(others = state.others.map {
                        when (action.player) {
                            it -> it.copy(name = action.name)
                            else -> it
                        }
                    })
                }
            }
            Action.Next -> state
        }
    }

    class View(context: Context) : LinearLayout(context), Widget<State, Action> {

        private val list = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
        }
        private val add = Button(context).apply {
            text = context.getString(R.string.configure_add)
        }
        private val next = Button(context).apply {
            text = context.getString(R.string.configure_next)
        }
        private val adapter = Adapter()

        init {
            orientation = VERTICAL
            addView(list, LayoutParams(MATCH_PARENT, 0, 1f))
            addView(add, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(next, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            list.adapter = adapter
        }

        override suspend fun show(state: State): Action =
            suspendCancellableCoroutine { continuation ->
                when (state) {
                    is State.PlayerList -> {
                        adapter.submitList(listOf(state.first, state.second) + state.others)

                        fun disableAll() {
                            adapter.onAction = null
                            add.setOnClickListener(null)
                            next.setOnClickListener(null)
                        }

                        continuation.invokeOnCancellation {
                            disableAll()
                        }
                        adapter.onAction = {
                            disableAll()
                            continuation.resume(
                                when (it) {
                                    is Adapter.Action.Remove -> Action.Remove(it.player)
                                }
                            )
                        }
                        add.setOnClickListener {
                            disableAll()
                            // todo add some fancy names
                            continuation.resume(Action.Add(Player("new player " + (random() * 1000 + 1).toInt())))
                        }
                        next.setOnClickListener {
                            disableAll()
                            continuation.resume(Action.Next)
                        }
                    }
                }
            }

        class Adapter : ListAdapter<Player, ViewHolder>(PlayerItemCallback()) {

            // todo edit name
            sealed class Action {
                data class Remove(val player: Player) : Action()
            }

            var onAction: ((Action) -> Unit)? = null

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(parent.context)

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.launch {
                    val action = when (val action = holder.playerView.show(getItem(position))) {
                        is PlayerItemView.Action.Remove -> Action.Remove(action.player)
                    }
                    onAction?.invoke(action)
                }
            }

            override fun onViewRecycled(holder: ViewHolder) {
                super.onViewRecycled(holder)
                holder.coroutineContext.cancelChildren()
            }
        }

        class ViewHolder(
            context: Context
        ) : RecyclerView.ViewHolder(PlayerItemView(context)),
            CoroutineScope by CoroutineScope(Job()) {

            val playerView: PlayerItemView get() = itemView as PlayerItemView
        }

        class PlayerItemCallback : DiffUtil.ItemCallback<Player>() {
            override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean =
                oldItem === newItem

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean =
                oldItem == newItem
        }

        class PlayerItemView(context: Context) : Widget<Player, PlayerItemView.Action>,
            LinearLayout(context) {

            sealed class Action {
                data class Remove(val player: Player) : Action()
            }

            private val name = AppCompatTextView(context).apply {
                TextViewCompat.setTextAppearance(this, android.R.style.TextAppearance_Large)
            }
            private val removeButton = AppCompatImageButton(context).apply {
                setImageResource(android.R.drawable.ic_delete)
            }

            init {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                setPadding(resources.getDimensionPixelOffset(R.dimen.dp5))
                addView(name, LayoutParams(0, MATCH_PARENT, 1f))
                addView(removeButton, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            }

            override suspend fun show(state: Player): Action {
                name.text = state.name
                return removeButton.awaitClick().let { Action.Remove(state) }
            }
        }
    }
}