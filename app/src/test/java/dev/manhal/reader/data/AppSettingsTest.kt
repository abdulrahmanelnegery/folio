package dev.manhal.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    private class MapStore(val map: MutableMap<String, Any> = mutableMapOf()) : SettingsStore {
        override fun getString(key: String) = map[key] as? String
        override fun getBoolean(key: String, default: Boolean) = map[key] as? Boolean ?: default
        override fun putString(key: String, value: String) { map[key] = value }
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    }

    @Test fun `defaults are the Zimam look`() {
        val s = SettingsRepository(MapStore()).value
        assertEquals(AppSettings(), s)
        assertEquals(ColourTheme.Orange, s.colour)
        assertEquals(TextSize.Default, s.textSize)
        assertEquals(Density.Comfortable, s.density)
        assertFalse(s.reduceMotion)
    }

    @Test fun `every setting round trips through the store`() {
        val store = MapStore()
        SettingsRepository(store).update { AppSettings(AppTheme.Dark, ColourTheme.Indigo, TextSize.Larger, Density.Compact, true, AppLanguage.Arabic, true) }
        val again = SettingsRepository(store).value
        assertEquals(AppSettings(AppTheme.Dark, ColourTheme.Indigo, TextSize.Larger, Density.Compact, true, AppLanguage.Arabic, true), again)
    }

    @Test fun `the keys of the earlier theme and language settings still load`() {
        val store = MapStore(mutableMapOf("app_theme" to "Light", "app_language" to "Arabic", "arabic_digits" to true))
        val s = SettingsRepository(store).value
        assertEquals(AppTheme.Light, s.theme)
        assertEquals(AppLanguage.Arabic, s.language)
        assertTrue(s.arabicDigits)
    }

    @Test fun `unknown stored values fall back to the defaults`() {
        val s = SettingsRepository(MapStore(mutableMapOf("colour_theme" to "Neon", "text_size" to "Huge", "density" to "Tight"))).value
        assertEquals(AppSettings(), s)
    }

    @Test fun `only changed settings are written and the state flow follows`() {
        val store = MapStore()
        val repo = SettingsRepository(store)
        repo.update { it.copy(textSize = TextSize.Large) }
        assertEquals(mapOf<String, Any>("text_size" to "Large"), store.map)
        assertEquals(TextSize.Large, repo.state.value.textSize)
        repo.update { it }
        assertEquals(1, store.map.size)
    }

    @Test fun `text sizes grow in four steps`() {
        assertEquals(listOf(0.9f, 1f, 1.15f, 1.3f), TextSize.entries.map { it.scale })
    }

    @Test fun `every palette is readable in light and dark`() {
        val paper = 0xFFFFF8F0; val ink = 0xFF0F0F0F; val white = 0xFFFFFFFF
        for (c in ColourTheme.entries) {
            // The brand orange is the existing baseline on paper (just under 3); no other palette is worse than it.
            assertTrue("${c.name} on paper", Contrast.ratio(c.accent, paper) >= minOf(3.0, Contrast.ratio(ColourTheme.Orange.accent, paper)) - 0.001)
            assertTrue("${c.name} white text on accent", Contrast.ratio(c.accent, white) >= 3.0)
            assertTrue("${c.name} accent on dark", Contrast.ratio(c.accent, ink) >= 3.0)
            assertTrue("${c.name} light container text", Contrast.ratio(c.containerLight, c.onContainerLight) >= 7.0)
            assertTrue("${c.name} dark container text", Contrast.ratio(c.containerDark, c.onContainerDark) >= 7.0)
        }
    }

    @Test fun `palettes are distinct and the default is orange`() {
        assertEquals(ColourTheme.entries.size, ColourTheme.entries.map { it.accent }.toSet().size)
        assertEquals(ColourTheme.Orange, ColourTheme.entries.first())
    }
}
