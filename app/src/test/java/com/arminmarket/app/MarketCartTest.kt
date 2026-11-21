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
    fun supportsQuantityUpdatesRemovalAndClear() {
        val first = MarketCart.add(emptyList(), MarketCartItem("DEMO-A24", 100L))
        val second = MarketCart.add(first, MarketCartItem("DEMO-HEADSET", 50L))
        val updated = MarketCart.setQuantity(second, "DEMO-A24", 3)

        assertEquals(4, MarketCart.itemCount(updated))
        assertEquals(350L, MarketCart.totalToman(updated))
        assertEquals(1, MarketCart.remove(updated, "DEMO-A24").size)
        assertEquals(emptyList<MarketCartLine>(), MarketCart.clear())
        val zeroQuantity = MarketCart.setQuantity(updated, "DEMO-A24", 0)
        assertEquals(1, zeroQuantity.size)
        assertEquals("DEMO-HEADSET", zeroQuantity.single().sku)
    }

    @Test
    fun rejectsUnknownSkuForQuantityUpdateAndNegativeQuantity() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketCart.setQuantity(emptyList(), "DEMO-MISSING", 2)
        }
        val cart = MarketCart.add(emptyList(), MarketCartItem("DEMO-A24", 100L))
        assertThrows(IllegalArgumentException::class.java) {
            MarketCart.setQuantity(cart, "DEMO-A24", -1)
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
