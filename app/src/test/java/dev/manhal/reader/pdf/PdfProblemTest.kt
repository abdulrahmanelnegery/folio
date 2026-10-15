package dev.manhal.reader.pdf

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PdfProblemTest {
    @Test fun `a security exception means a password`() = assertEquals(PdfFile.Companion.Problem.Password, PdfFile.classify(SecurityException("password required")))

    @Test fun `anything else is a damaged file`() {
        assertEquals(PdfFile.Companion.Problem.Damaged, PdfFile.classify(IOException("not a PDF")))
        assertEquals(PdfFile.Companion.Problem.Damaged, PdfFile.classify(IllegalStateException()))
    }
}
