package dev.folio.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test fun systemFollowsTheSystem() { assertTrue(AppTheme.System.isDark(true)); assertFalse(AppTheme.System.isDark(false)) }
    @Test fun lightAndDarkIgnoreTheSystem() {
        assertFalse(AppTheme.Light.isDark(true)); assertTrue(AppTheme.Dark.isDark(false))
    }
    @Test fun anUnknownOrMissingValueMeansSystem() {
        assertEquals(AppTheme.System, AppTheme.parse(null)); assertEquals(AppTheme.System, AppTheme.parse("Sepia")); assertEquals(AppTheme.Dark, AppTheme.parse("Dark"))
    }
}
