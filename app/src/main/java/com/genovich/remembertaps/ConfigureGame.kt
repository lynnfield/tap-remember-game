package com.genovich.remembertaps

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Tuple2
import arrow.fx.IO
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
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
        object Next : Action() {
            override fun toString() = this::class.qualifiedName!!
        }
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
                    state.second -> state.copy(second = state.second.copy(name = action.name))
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
        private val add = MaterialButton(context).apply {
            text = context.getString(R.string.configure_add)
        }
        private val next = MaterialButton(context).apply {
            text = context.getString(R.string.configure_next)
        }
        private val adapter = Adapter()

        init {
            setPadding(context.resources.getDimensionPixelOffset(R.dimen.dp8))
            orientation = VERTICAL
            addView(list, LayoutParams(MATCH_PARENT, 0, 1f))
            addView(add, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(next, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            list.adapter = adapter
        }

        override suspend fun show(state: State): Action = suspendCancellableCoroutine { cont ->
            when (state) {
                is State.PlayerList -> {
                    adapter.submitList(listOf(state.first, state.second) + state.others)

                    fun disableAll() {
                        adapter.onAction = null
                        add.setOnClickListener(null)
                        next.setOnClickListener(null)
                    }

                    cont.invokeOnCancellation {
                        disableAll()
                    }
                    adapter.onAction = {
                        disableAll()
                        cont.resume(
                            when (it) {
                                is Adapter.Action.Remove -> Action.Remove(it.player)
                                is Adapter.Action.Rename -> Action.SetName(it.player, it.name)
                            }
                        )
                    }
                    add.setOnClickListener {
                        disableAll()
                        // todo add some fancy names
                        cont.resume(Action.Add(Player("new player " + (random() * 1000 + 1).toInt())))
                    }
                    next.setOnClickListener {
                        disableAll()
                        cont.resume(Action.Next)
                    }
                }
            }
        }

        class Adapter : ListAdapter<Player, ViewHolder>(PlayerItemCallback()) {

            sealed class Action {
                data class Remove(val player: Player) : Action()
                data class Rename(val player: Player, val name: String) : Action()
            }

            var onAction: ((Action) -> Unit)? = null

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(parent.context)

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                // todo do not work as expected, because of incorrect usage
                holder.launch {
                    // todo looks creepy
                    val action = when (val action = holder.playerView.show(getItem(position))) {
                        is PlayerItemView.Action.Remove -> Action.Remove(action.player)
                        is PlayerItemView.Action.Rename -> Action.Rename(action.player, action.name)
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
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean =
                oldItem == newItem
        }

        class PlayerItemView(context: Context) : Widget<Player, PlayerItemView.Action>,
            LinearLayout(context) {

            sealed class Action {
                data class Remove(val player: Player) : Action()
                data class Rename(val player: Player, val name: String) : Action()
            }

            private val nameEditor = TextInputEditText(context).apply {
                TextViewCompat.setTextAppearance(
                    this,
                    TypedValue().also {
                        context.theme.resolveAttribute(R.attr.textAppearanceListItem, it, true)
                    }.data
                )
                gravity = Gravity.CENTER_VERTICAL
            }
            private val removeButton = AppCompatImageButton(
                context,
                null,
                R.style.Widget_MaterialComponents_Button_OutlinedButton
            ).apply {
                setImageResource(R.drawable.ic_baseline_delete_24)
                setPadding(resources.getDimensionPixelOffset(R.dimen.dp8))
            }

            init {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                addView(nameEditor, LayoutParams(0, WRAP_CONTENT, 1f))
                addView(removeButton, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            }

            override suspend fun show(state: Player): Action = withContext(Dispatchers.Main) {
                nameEditor.setText(state.name)
                oneOf(
                    later { Action.Rename(state, nameEditor.getChangedText()) },
                    later { removeButton.awaitClick().let { Action.Remove(state) } }
                )
            }
        }
    }
}