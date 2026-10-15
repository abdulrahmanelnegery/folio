package dev.manhal.reader.render

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.manhal.reader.ManhalViewModel
import dev.manhal.reader.Strings
import dev.manhal.reader.data.Book
import dev.manhal.reader.data.DayStat
import dev.manhal.reader.data.HomeTab
import dev.manhal.reader.data.HomeTabConfig
import dev.manhal.reader.data.Kind
import dev.manhal.reader.data.ReadSession
import dev.manhal.reader.data.Status
import dev.manhal.reader.data.Tab
import dev.manhal.reader.ui.LibraryScreen
import dev.manhal.reader.ui.LocalClockHour
import dev.manhal.reader.ui.ManhalTheme
import dev.manhal.reader.ui.SettingsPage
import dev.manhal.reader.ui.SettingsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reference screenshots of the main screens at the normal and at 130 percent font scale, to catch overflow and crowding.
 * Update the images with `./gradlew :app:recordRoborazziDebug`; CI runs `:app:verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var model: ManhalViewModel
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f))

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Strings.res = app.resources
        model = ManhalViewModel(app)
        val day = 24 * 3600 * 1000L
        val t0 = 1_760_000_000_000L
        listOf(
            Book("1", "u1", "The Count of Monte Cristo", kind = Kind.Pdf, pages = 1200, page = 410, status = Status.Reading, lastOpened = t0, added = t0, readSeconds = 7200, pagesRead = 80),
            Book("2", "u2", "كليلة ودمنة", kind = Kind.Epub, pages = 30, page = 3, status = Status.Reading, lastOpened = t0 - day, added = t0, favorite = true, rating = 4),
            Book("3", "u3", "Notes on Systems", kind = Kind.Text, pages = 200, status = Status.ToRead, added = t0),
            Book("4", "u4", "A Short History of Nearly Everything", kind = Kind.Pdf, pages = 600, page = 600, status = Status.Finished, lastOpened = t0 - 3 * day, added = t0, rating = 5),
        ).forEach(model.repo::add)
        model.repo.update {
            it.copy(
                sessions = listOf(ReadSession("1", t0, 1800, 20), ReadSession("2", t0 - day, 900, 3)),
                days = mapOf("2025-10-09" to DayStat(1800, 20), "2025-10-08" to DayStat(900, 3)),
                homeTabs = listOf(HomeTabConfig(HomeTab.Home), HomeTabConfig(HomeTab.Stats), HomeTabConfig(HomeTab.Library)),
                tourDone = true,
                hiddenNews = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty(),
            )
        }
    }

    @After fun tearDown() { Strings.res = null }

    private fun show(fontScale: Float, content: @androidx.compose.runtime.Composable () -> Unit) = compose.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale), LocalClockHour provides 9) { ManhalTheme { content() } }
    }

    private fun library() = @androidx.compose.runtime.Composable { LibraryScreen(model, onOpenPicker = {}, onSettings = {}, onAbout = {}, onBrowse = { _, _ -> }) }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", options)
    }

    private fun home(scale: Float, tab: String?, name: String) {
        show(scale, library())
        if (tab != null) { compose.onAllNodesWithText(tab).onFirst().performClick(); compose.waitForIdle() }
        shot(name)
    }

    @Test fun homeNormal() = home(1f, null, "home_normal")
    @Test fun homeLargeFont() = home(1.3f, null, "home_font130")
    @Test fun statsNormal() = home(1f, "Stats", "stats_normal")
    @Test fun statsLargeFont() = home(1.3f, "Stats", "stats_font130")
    @Test fun libraryTabNormal() = home(1f, "Library", "librarytab_normal")
    @Test fun libraryTabLargeFont() = home(1.3f, "Library", "librarytab_font130")

    @Test fun allBooksNormal() { model.libraryTab = Tab.All; show(1f, library()); shot("allbooks_normal") }
    @Test fun allBooksLargeFont() { model.libraryTab = Tab.All; show(1.3f, library()); shot("allbooks_font130") }

    @Test fun settingsNormal() { show(1f) { SettingsScreen(model, onBack = {}) }; shot("settings_normal") }
    @Test fun settingsLargeFont() { show(1.3f) { SettingsScreen(model, onBack = {}) }; shot("settings_font130") }
    @Test fun appearanceNormal() { show(1f) { SettingsScreen(model, onBack = {}, initialPage = SettingsPage.Appearance) }; shot("appearance_normal") }
    @Test fun appearanceLargeFont() { show(1.3f) { SettingsScreen(model, onBack = {}, initialPage = SettingsPage.Appearance) }; shot("appearance_font130") }
    @Test fun layoutPage() { show(1f) { SettingsScreen(model, onBack = {}, initialPage = SettingsPage.Layout) }; shot("settings_layout") }
    @Test fun readerPage() { show(1f) { SettingsScreen(model, onBack = {}, initialPage = SettingsPage.Reader) }; shot("settings_reader") }
    @Test fun privacyPage() { show(1f) { SettingsScreen(model, onBack = {}, initialPage = SettingsPage.Privacy) }; shot("settings_privacy") }

    private fun bookMenu(scale: Float, name: String) {
        model.libraryTab = Tab.All
        show(scale, library())
        compose.onAllNodesWithText("Notes on Systems").onFirst().performTouchInput { longClick() }
        compose.waitForIdle()
        shot(name)
    }
    @Test fun bookMenuNormal() = bookMenu(1f, "menu_book")
    @Test fun bookMenuLargeFont() = bookMenu(1.3f, "menu_book_font130")

    private fun searchSheet(scale: Float, name: String) {
        val hits = listOf(
            dev.manhal.reader.data.SearchHit(11, 40, "The black cat sat on the warm windowsill all afternoon while the rain fell."),
            dev.manhal.reader.data.SearchHit(12, 5, "Every cat knows that a closed door is only a suggestion."),
            dev.manhal.reader.data.SearchHit(40, 90, "Schrodinger never said whether the cat liked boxes."),
        )
        show(scale) {
            dev.manhal.reader.ui.BookSearchSheet(
                search = { kotlinx.coroutines.flow.flowOf(dev.manhal.reader.data.SearchProgress(hits, 1f, true)) },
                where = { dev.manhal.reader.s(dev.manhal.reader.R.string.search_page_hit, it.unit + 1) },
                onPick = { _, _ -> }, onDismiss = {},
            )
        }
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("cat")
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        shot(name)
    }
    @Test fun searchNormal() = searchSheet(1f, "search_normal")
    @Test fun searchLargeFont() = searchSheet(1.3f, "search_font130")

    private fun drawer(scale: Float, name: String) {
        show(scale, library())
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.mainClock.advanceTimeBy(600)
        shot(name)
    }
    @Test fun drawerNormal() = drawer(1f, "drawer_normal")
    @Test fun drawerLargeFont() = drawer(1.3f, "drawer_font130")

    @Config(qualifiers = "+ar")
    @Test fun drawerArabic() {
        show(1f, library())
        compose.onNodeWithContentDescription("القائمة").performClick()
        compose.mainClock.advanceTimeBy(600)
        shot("drawer_arabic")
    }
}
