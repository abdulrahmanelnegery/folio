package dev.manhal.reader

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitsTest {
    @After fun reset() { Strings.arabicDigits = false }

    @Test fun `counters keep Latin digits by default`() = assertEquals("3/12", digits("3/12"))

    @Test fun `counters follow the Arabic digit setting`() {
        Strings.arabicDigits = true
        assertEquals("٣/١٢", digits("3/12"))
    }
}
