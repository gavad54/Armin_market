package com.arminmarket.app

/** Local cart rules for the current demo catalog. No order or payment is submitted. */
data class MarketCartItem(
    val sku: String,
    val unitPriceToman: Long
)

data class MarketCartLine(
    val sku: String,
    val unitPriceToman: Long,
    val quantity: Int
)

object MarketCart {
    fun add(lines: List<MarketCartLine>, item: MarketCartItem): List<MarketCartLine> {
        require(item.sku.matches(Regex("[A-Z0-9][A-Z0-9._-]{1,39}"))) { "INVALID_SKU" }
        require(item.unitPriceToman >= 0) { "INVALID_PRICE" }

        val existing = lines.firstOrNull { it.sku == item.sku }
        if (existing == null) {
            return lines + MarketCartLine(item.sku, item.unitPriceToman, 1)
        }
        require(existing.unitPriceToman == item.unitPriceToman) { "CART_PRICE_CHANGED" }
        require(existing.quantity < Int.MAX_VALUE) { "CART_QUANTITY_LIMIT" }
        return lines.map {
            if (it.sku == item.sku) it.copy(quantity = it.quantity + 1) else it
        }
    }

    fun itemCount(lines: List<MarketCartLine>): Int =
        lines.fold(0) { total, line ->
            require(line.quantity > 0) { "INVALID_QUANTITY" }
            Math.addExact(total, line.quantity)
        }

    fun totalToman(lines: List<MarketCartLine>): Long =
        lines.fold(0L) { total, line ->
            require(line.quantity > 0) { "INVALID_QUANTITY" }
            require(line.unitPriceToman >= 0) { "INVALID_PRICE" }
            Math.addExact(total, Math.multiplyExact(line.unitPriceToman, line.quantity.toLong()))
        }
}
