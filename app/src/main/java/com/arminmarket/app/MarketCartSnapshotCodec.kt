package com.arminmarket.app

/** Versioned local-only snapshot format. It does not submit orders or payments. */
object MarketCartSnapshotCodec {
    private const val VERSION = "v1"
    private const val MAX_LINES = 500
    private const val MAX_SNAPSHOT_CHARS = 64 * 1024
    private val skuPattern = Regex("[A-Z0-9][A-Z0-9._-]{1,39}")

    fun encode(lines: List<MarketCartLine>): String {
        require(lines.size <= MAX_LINES) { "CART_LINE_LIMIT" }
        val seen = mutableSetOf<String>()
        lines.forEach { line ->
            require(skuPattern.matches(line.sku)) { "INVALID_SKU" }
            require(line.unitPriceToman >= 0) { "INVALID_PRICE" }
            require(line.quantity > 0) { "INVALID_QUANTITY" }
            require(seen.add(line.sku)) { "DUPLICATE_SKU" }
        }
        val snapshot = buildString {
            append(VERSION)
            lines.forEach { line ->
                append('\n')
                append(line.sku)
                append('|')
                append(line.unitPriceToman)
                append('|')
                append(line.quantity)
            }
        }
        require(snapshot.length <= MAX_SNAPSHOT_CHARS) { "SNAPSHOT_TOO_LARGE" }
        return snapshot
    }

    fun decode(snapshot: String?): List<MarketCartLine> {
        if (snapshot.isNullOrBlank()) return emptyList()
        require(snapshot.length <= MAX_SNAPSHOT_CHARS) { "SNAPSHOT_TOO_LARGE" }

        val rows = snapshot.split('\n')
        if (rows.firstOrNull() != VERSION) return emptyList()
        require(rows.size - 1 <= MAX_LINES) { "CART_LINE_LIMIT" }

        val seen = mutableSetOf<String>()
        return rows.drop(1).map { row ->
            val fields = row.split('|')
            require(fields.size == 3) { "CORRUPT_CART_SNAPSHOT" }
            val sku = fields[0]
            val price = fields[1].toLongOrNull()
            val quantity = fields[2].toIntOrNull()
            require(skuPattern.matches(sku)) { "INVALID_SKU" }
            require(price != null && price >= 0) { "INVALID_PRICE" }
            require(quantity != null && quantity > 0) { "INVALID_QUANTITY" }
            require(seen.add(sku)) { "DUPLICATE_SKU" }
            MarketCartLine(sku, price, quantity)
        }
    }
}
