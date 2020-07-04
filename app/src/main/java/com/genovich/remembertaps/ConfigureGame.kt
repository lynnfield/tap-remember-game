package com.genovich.remembertaps

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Tuple2
import java.lang.Math.random

object ConfigureGame : Feature<ConfigureGame.State, ConfigureGame.Action> {
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

    override fun process(input: Tuple2<State, Action>): State = when (val state = input.a) {
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
            text = "Add"
        }
        private val next = Button(context).apply {
            text = "Next"
        }
        private val adapter = Adapter()

        init {
            orientation = VERTICAL
            addView(list, LayoutParams(MATCH_PARENT, 0, 1f))
            addView(add, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(next, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            list.adapter = adapter
        }

        override fun show(state: State, callback: (Action) -> Unit) {
            when (state) {
                is State.PlayerList -> {
                    adapter.submitList(listOf(state.first, state.second) + state.others)
                    adapter.onAction = {
                        adapter.onAction = null
                        callback(
                            when (it) {
                                is Adapter.Action.Remove -> Action.Remove(it.player)
                            }
                        )
                    }
                    add.setOnClickListener {
                        it.setOnClickListener(null)
                        // todo add some fancy names
                        callback(Action.Add(Player("new player " + (random() * 1000 + 1).toInt())))
                    }
                    next.setOnClickListener {
                        it.setOnClickListener(null)
                        callback(Action.Next)
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
                holder.playerView.show(getItem(position)) {
                    onAction?.invoke(
                        when (it) {
                            is PlayerItemView.Action.Remove -> Action.Remove(it.player)
                        }
                    )
                }
            }

        }

        class ViewHolder(context: Context) : RecyclerView.ViewHolder(PlayerItemView(context)) {
            val playerView: PlayerItemView get() = itemView as PlayerItemView
        }

        class PlayerItemCallback : DiffUtil.ItemCallback<Player>() {
            override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
                return oldItem == newItem
            }
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
                addView(name, LayoutParams(0, MATCH_PARENT, 1f))
                // todo adjust to end
                addView(removeButton, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            }

            override fun show(state: Player, callback: (Action) -> Unit) {
                name.text = state.name
                removeButton.setOnClickListener {
                    callback(Action.Remove(state))
                }
            }
        }

    }
}