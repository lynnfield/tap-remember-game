package com.genovich.remembertaps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppProcessTest {

    @Test
    fun process() {
        // given
        val input = App.State.Menu(Menu.State.Menu) to App.Action.Menu(Menu.Action.Start)
        val expected = App.State.ConfigureGame

        // when
        val actual = App.process(input)

        // then
        assertEquals(expected, actual)
    }
}