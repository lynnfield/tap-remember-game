package com.genovich.remembertaps

import kotlin.math.hypot

// todo replace float with percent
data class Tap(val x: Float, val y: Float)

fun Float.near(taps: Pair<Tap, Tap>): Boolean =
    hypot(taps.second.x - taps.first.x, taps.second.y - taps.first.y) <= this