package dev.manhal.reader.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StorageLabelTest {
    @Test fun theOldTopLevelFolderAndTheOldZimamFolderGetDifferentMarkers() {
        val top = Storage.labelOf(File("/storage/emulated/0/${LegacyNames.TOP_FOLDER}"))
        val zimam = Storage.labelOf(File("/storage/emulated/0/${LegacyNames.ZIMAM_FOLDER}"))
        assertEquals(LegacyNames.TOP_FOLDER, top)
        assertEquals("Zimam-${LegacyNames.TOP_FOLDER}", zimam)
        assertNotEquals(top, zimam)
    }
}
