package com.arminmarket.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MarketCartSnapshotCodecTest {
    @Test
    fun roundTripsMultipleCartLinesWithoutLosingQuantityOrPrice() {
        val cart = listOf(
            MarketCartLine("DEMO-A24", 12_800_000L, 2),
            MarketCartLine("DEMO-HEADSET", 2_450_000L, 1)
        )

        assertEquals(cart, MarketCartSnapshotCodec.decode(MarketCartSnapshotCodec.encode(cart)))
    }

    @Test
    fun treatsMissingOrUnknownVersionSnapshotsAsEmpty() {
        assertEquals(emptyList<MarketCartLine>(), MarketCartSnapshotCodec.decode(null))
        assertEquals(emptyList<MarketCartLine>(), MarketCartSnapshotCodec.decode("v9\nDEMO-A24|100|1"))
    }

    @Test
    fun rejectsCorruptOrDuplicateSnapshotLines() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketCartSnapshotCodec.decode("v1\nBAD SKU|100|1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            MarketCartSnapshotCodec.decode("v1\nDEMO-A24|100|1\nDEMO-A24|100|2")
        }
    }

    @Test
    fun rejectsOversizedSnapshotsBeforeParsingRows() {
        val oversized = "v1\n" + "A".repeat(64 * 1024)

        assertThrows(IllegalArgumentException::class.java) {
            MarketCartSnapshotCodec.decode(oversized)
        }
    }

    @Test
    fun rejectsSnapshotsWithMoreThanFiveHundredLines() {
        val rows = (1..501).joinToString("\n") { index ->
            "SKU${index.toString().padStart(3, '0')}|100|1"
        }

        assertThrows(IllegalArgumentException::class.java) {
            MarketCartSnapshotCodec.decode("v1\n$rows")
        }
    }

    @Test
    fun rejectsEncodingMoreThanFiveHundredCartLines() {
        val cart = (1..501).map { index ->
            MarketCartLine("SKU${index.toString().padStart(3, '0')}", 100L, 1)
        }

        assertThrows(IllegalArgumentException::class.java) {
            MarketCartSnapshotCodec.encode(cart)
        }
    }
}
