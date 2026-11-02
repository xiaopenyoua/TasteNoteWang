package com.weiji.wang.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientScalerTest {

    @Test
    fun `scales linearly`() {
        assertEquals(200.0, IngredientScaler.scale(100.0, 2, 4)!!, 0.001)
        assertEquals(50.0, IngredientScaler.scale(100.0, 2, 1)!!, 0.001)
        assertEquals(100.0, IngredientScaler.scale(100.0, 2, 2)!!, 0.001)
    }

    @Test
    fun `kitchen rounding stays readable`() {
        assertEquals("200", IngredientScaler.format(200.0))
        assertEquals("1.5", IngredientScaler.format(1.5))
        assertEquals("2.3", IngredientScaler.format(2.33))
        assertEquals("105", IngredientScaler.format(103.0))
    }

    @Test
    fun `formats with unit`() {
        assertEquals("400克", IngredientScaler.format(400.0, "克"))
        assertEquals("1.5勺", IngredientScaler.format(1.5, "勺"))
    }

    @Test
    fun `scales range and keeps range shape`() {
        assertEquals("4-6勺", IngredientScaler.scaleRange("2-3勺", 2, 4))
    }

    @Test
    fun `vague words are not scaled`() {
        assertTrue(IngredientScaler.isVague("适量盐"))
        assertTrue(IngredientScaler.isVague("少许油"))
        assertFalse(IngredientScaler.isVague("200克"))
    }

    @Test
    fun `null amount stays null`() {
        assertEquals(null, IngredientScaler.scale(null, 2, 4))
    }
}

class UlidTest {
    @Test
    fun `generates valid sortable ids`() {
        val a = Ulid.next(1_000)
        val b = Ulid.next(2_000)
        assertEquals(26, a.length)
        assertTrue(Ulid.isValid(a))
        assertTrue(Ulid.isValid(b))
        assertTrue(a < b)
        assertEquals(1_000L, Ulid.timestampOf(a))
        assertEquals(2_000L, Ulid.timestampOf(b))
    }

    @Test
    fun `rejects malformed ids`() {
        assertFalse(Ulid.isValid("abc"))
        assertFalse(Ulid.isValid("I".repeat(26)))
        assertNotNull(Ulid.timestampOf(Ulid.next()))
    }
}
