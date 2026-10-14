package dev.folio.reader.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StorageLabelTest {
    @Test fun theOldTopLevelFolderAndTheOldZimamFolderGetDifferentMarkers() {
        val top = Storage.labelOf(File("/storage/emulated/0/Rukn"))
        val zimam = Storage.labelOf(File("/storage/emulated/0/Zimam/Rukn"))
        assertEquals("Rukn", top)
        assertEquals("Zimam-Rukn", zimam)
        assertNotEquals(top, zimam)
    }
}
