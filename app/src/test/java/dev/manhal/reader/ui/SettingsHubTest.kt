package dev.manhal.reader.ui

import dev.manhal.reader.EnglishStrings
import dev.manhal.reader.data.AppLanguage
import dev.manhal.reader.data.AppSettings
import dev.manhal.reader.data.AppTheme
import dev.manhal.reader.data.ColourTheme
import dev.manhal.reader.data.HomeTab
import dev.manhal.reader.data.HomeTabConfig
import dev.manhal.reader.data.LibraryState
import dev.manhal.reader.data.TextSize
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsHubTest {
    @Before fun english() = EnglishStrings.install()

    private fun summary(page: SettingsPage, app: AppSettings = AppSettings(), lib: LibraryState = LibraryState(), shared: Boolean = false) = page.summary(app, lib, shared, "1.2.3")

    @Test fun `the rows follow the shared order of the family`() {
        assertEquals(listOf("Appearance", "Layout", "Language", "Reader", "Data", "Privacy", "About"), SettingsPage.entries.map { it.name })
    }

    @Test fun `appearance summarises mode, colour and text size`() {
        assertEquals("System, Orange, Default", summary(SettingsPage.Appearance))
        assertEquals("Dark, Teal, Larger", summary(SettingsPage.Appearance, AppSettings(theme = AppTheme.Dark, colour = ColourTheme.Teal, textSize = TextSize.Larger)))
    }

    @Test fun `layout lists the sections that are shown`() {
        val lib = LibraryState(homeTabs = listOf(HomeTabConfig(HomeTab.Home), HomeTabConfig(HomeTab.Stats, visible = false), HomeTabConfig(HomeTab.Library)))
        assertEquals("Home, Library", summary(SettingsPage.Layout, lib = lib))
    }

    @Test fun `language names the choice and the digit style`() {
        assertEquals("System", summary(SettingsPage.Language))
        assertEquals("English", summary(SettingsPage.Language, AppSettings(language = AppLanguage.English)))
        assertEquals("العربية, Arabic digits", summary(SettingsPage.Language, AppSettings(language = AppLanguage.Arabic, arabicDigits = true)))
    }

    @Test fun `data, reader, privacy and about summaries`() {
        assertEquals("Inside the app", summary(SettingsPage.Data))
        assertEquals("In the Zimam/Manhal folder", summary(SettingsPage.Data, shared = true))
        assertEquals("Normal theme, EPUB text 100%", summary(SettingsPage.Reader))
        assertEquals("No account, no tracking", summary(SettingsPage.Privacy))
        assertEquals("Version 1.2.3", summary(SettingsPage.About))
    }
}
