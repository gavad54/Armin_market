package com.arminmarket.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MarketCartTest {
    @Test
    fun addingSameSkuIncrementsQuantityWithoutDuplicatingLine() {
        val item = MarketCartItem("DEMO-A24", 12_800_000L)
        val cart = MarketCart.add(MarketCart.add(emptyList(), item), item)

        assertEquals(1, cart.size)
        assertEquals(2, cart.single().quantity)
        assertEquals(2, MarketCart.itemCount(cart))
        assertEquals(25_600_000L, MarketCart.totalToman(cart))
    }

    @Test
    fun rejectsMalformedSkuAndNegativePrice() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketCart.add(emptyList(), MarketCartItem(" ", 100L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MarketCart.add(emptyList(), MarketCartItem("DEMO-1", -1L))
        }
    }

    @Test
    fun rejectsPriceChangeForAnExistingCartLine() {
        val cart = MarketCart.add(emptyList(), MarketCartItem("DEMO-A24", 100L))
        assertThrows(IllegalArgumentException::class.java) {
            MarketCart.add(cart, MarketCartItem("DEMO-A24", 120L))
        }
    }

    @Test
    fun rejectsArithmeticOverflowInCartTotal() {
        val cart = listOf(MarketCartLine("DEMO-1", Long.MAX_VALUE, 2))
        assertThrows(ArithmeticException::class.java) {
            MarketCart.totalToman(cart)
        }
    }
}
